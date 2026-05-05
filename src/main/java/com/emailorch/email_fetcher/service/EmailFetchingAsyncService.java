package com.emailorch.email_fetcher.service;

import com.emailorch.email_fetcher.model.Transfer;
import com.emailorch.email_fetcher.repository.TransferRepository;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.microsoft.graph.models.Attachment;
import com.microsoft.graph.models.AttachmentCollectionResponse;
import com.microsoft.graph.models.FileAttachment;
import com.microsoft.graph.models.MessageCollectionResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;


@Service
public class EmailFetchingAsyncService {
    private final TransferRepository transferRepository;

    private final OAuth2AuthorizedClientService clientService;
    private final GmailService gmailService;
    private final OutlookService outlookService;
    EmailFetchingAsyncService(TransferRepository transferRepository,OAuth2AuthorizedClientService clientService,GmailService gmailService,OutlookService outlookService){
        this.transferRepository=transferRepository;
        this.clientService = clientService;
        this.gmailService=gmailService;
        this.outlookService=outlookService;
    }
    @Async("tp")
    public void  syncMessages(Long uid,String uname,boolean sync , String provider) throws Exception {
        long dbCount = transferRepository.countByUid(uid);
        boolean needsSync = dbCount == 0 || sync;

        if(needsSync) {
            // Double-check: only sync if still 0 (another request might have just finished)
            //stops sync mess if the user presses 2 times simeltinously Race Condition Protection to prevent duplication
            synchronized (this) {
                dbCount = transferRepository.countByUid(uid);
                if (dbCount > 0 && !sync) {
                    needsSync = false;
                }
            }
        }

        if (needsSync) {
            OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(provider.toLowerCase(), uname);
            String accessToken = client.getAccessToken().getTokenValue();

            List<Transfer> fetchedTransfers = fetchAndSave(accessToken, uid,provider);
            System.out.println(">>> FETCHED: " + fetchedTransfers.size());

            // Dedup in memory: msgId + fname is the ONLY reliable unique key
            Map<String, Transfer> uniqueBatch = new HashMap<>();
            for (Transfer t : fetchedTransfers) {
                // E.g., "18b9c1a2b3:report.pdf"
                String key = t.getMsgId() + ":" + t.getFname();
                uniqueBatch.put(key, t);
            }

            // Check DB: skip rows that already exist ahhh this prevent duplicates
            List<Transfer> toSave = new ArrayList<>();
            for (Transfer t : uniqueBatch.values()) {
                if (!transferRepository.existsByUidAndMsgIdAndFname(uid, t.getMsgId(), t.getFname())) {
                    toSave.add(t);
                }
            }

            System.out.println(">>> NEW attachments to save: " + toSave.size());

            if (!toSave.isEmpty()) {
                try {
                    transferRepository.saveAll(toSave);
                    System.out.println(">>> SAVED: " + toSave.size());
                } catch (DataIntegrityViolationException e) {
                    // Genuine race condition — another request saved the same rows
                    System.out.println(">>> Race condition: " + e.getMessage());
                    // Save one by one to rescue the non-duplicate rows
                    for (Transfer t : toSave) {
                        try {
                            transferRepository.save(t);
                        } catch (DataIntegrityViolationException ex) {
                            // This specific row was the duplicate — skip it
                        }
                    }
                }
            }
        }
    }
    public List<Transfer> fetchAndSave(String accessToken, Long uid,String provider) throws Exception {
        List<Transfer> transfers = new ArrayList<>();

        if(provider.equalsIgnoreCase("microsoft") ||provider.equalsIgnoreCase("azure")|| provider.equalsIgnoreCase("outlook")){
            transfers=  fetchAndSaveOutlook(uid, accessToken, provider);

        }
        if(provider.equalsIgnoreCase("google")){
            transfers =    fetchAndSaveGmail(uid,provider,accessToken);
        }


        return transfers;
    }
    private List<Transfer> fetchAndSaveGmail(Long uid , String provider,String accessToken){
        List<Transfer> transfers = new ArrayList<>();

        try {
            System.out.println(">>> [1] Creating Gmail client for uid=" + uid);
            var gmailClient = gmailService.createClient(accessToken);

            Instant last = transferRepository.findLatestTransfersByUid(uid,provider);
            String query = "has:attachment";
            if (last != null) {
                long safeEpoch = last.getEpochSecond() - 86400;
                query = "has:attachment after:" + safeEpoch;
            }
            System.out.println(">>> [2] Query: " + query);

            String pageToken = null;

            do {
                var request = gmailClient.users().messages().list("me").setQ(query);
                if (pageToken != null) request.setPageToken(pageToken);

                var response = request.execute();

                System.out.println(">>> [3] Messages in response: " +
                        (response.getMessages() != null ? response.getMessages().size() : "NULL"));

                if (response.getMessages() != null) {
                    for (Message msg : response.getMessages()) {

                        System.out.println(">>> [4] Fetching full message: " + msg.getId());
                        Message fullmessage = gmailClient.users().messages()
                                .get("me", msg.getId()).execute();

                        String from = null;
                        Instant instantDate = null;

                        for (MessagePartHeader header : fullmessage.getPayload().getHeaders()) {
                            if (header.getName().equalsIgnoreCase("from")) from = header.getValue();
                            if (header.getName().equalsIgnoreCase("date")) {
                                try {
                                    String dateValue = header.getValue().replaceAll("\\s\\(.*\\)$", "");
                                    SimpleDateFormat format = new SimpleDateFormat(
                                            "EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
                                    instantDate = format.parse(dateValue).toInstant();
                                } catch (Exception e) {
                                    instantDate = Instant.ofEpochMilli(fullmessage.getInternalDate());
                                }
                            }
                        }

                        int before = transfers.size();
                        findAttachmentsRecursive(
                                fullmessage.getPayload().getParts(),
                                msg.getId(), from, instantDate,
                                transfers, uid
                        );
                        int after = transfers.size();
                        System.out.println(">>> [5] Message " + msg.getId() +
                                " → found " + (after - before) + " attachments");
                    }
                }

                pageToken = response.getNextPageToken();
                System.out.println(">>> [6] Next page token: " + pageToken);

            } while (pageToken != null);

            System.out.println(">>> [7] TOTAL attachments found: " + transfers.size());
//                return transfers;

        } catch (Exception e) {
            System.err.println(">>> [ERROR] Gmail fetch failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Gmail sync failed", e);
        }
        return transfers;
    }
    private List<Transfer> fetchAndSaveOutlook(Long uid,String accessToken,String provider) throws Exception {
        List<Transfer> transfers = new ArrayList<>();
        try {

            Instant last = transferRepository.findLatestTransfersByUid(uid,provider);
            String query;
            if (last != null) {
                String safeEpoch = String.valueOf(last.getEpochSecond() - 86400);
                query = "hasAttachments eq true and receivedDateTime ge "+ safeEpoch;
            } else {
                query = "hasAttachments eq true";
            }

            var outlookclient = outlookService.createClient(accessToken);
// 1. Get the "Box" (The Response wrapper)
            MessageCollectionResponse response = outlookclient.me().messages().get(requestConfiguration -> {
                // This is the equivalent of "has:attachment"
                requestConfiguration.queryParameters.filter = query;
                requestConfiguration.queryParameters.top=50;

                // Optimization: Only select the fields you need for your fetcher
                requestConfiguration.queryParameters.select = new String[]{
                        "id", "subject", "sender", "sentDateTime", "from", "receivedDateTime", "hasAttachments"
                };
                requestConfiguration.queryParameters.expand = new String[]{"attachments"};
            });
// 2. Get the "List" from the box
            Set<String> seen = new HashSet<>();

            while (response != null) {
                List<com.microsoft.graph.models.Message> messages = response.getValue();
                for (com.microsoft.graph.models.Message msg : messages) {


                    AttachmentCollectionResponse attachment = (AttachmentCollectionResponse) (AttachmentCollectionResponse) (AttachmentCollectionResponse) outlookclient.me()
                            .messages()
                            .byMessageId(msg.getId())
                            .attachments()
                            .get();
                    for (Attachment a : attachment.getValue()) {

                        if (a instanceof FileAttachment fa) {
                            String mysignature = STR."\{fa.getName()}-\{msg.getSentDateTime().toString()}";
                            if (!seen.contains(mysignature)) {
                                Transfer t = new Transfer(uid,msg.getId(),fa.getId(),fa.getName(),fa.getSize().longValue(),fa.getContentType(),msg.getFrom().getEmailAddress().getAddress(),msg.getSentDateTime().toInstant(),"microsoft");
/// uid,
///                         msgId,
///                         part.getBody().getAttachmentId(),
///                         filename,
///                         sizeLong,
///                         part.getMimeType(),
///                         from,
///                         date,
///                         "google"
                                transfers.add(t);
                                seen.add(mysignature);

                            }
                        }

                    }
                }
                String nextLink = response.getOdataNextLink(); // 1. Grabs the token URL
                if(nextLink!=null){
                    response = outlookclient.me().messages().withUrl(nextLink).get();
                }else{
                    response=null;
                }

            }
            for (Transfer t : transfers) {
                System.out.println(t);
            }
            //  return transfers;
        }catch (Exception e){
            throw  new Exception(e);
        }
        return transfers;
    }
    // Keep your existing recursive helper method right below it!
    private void findAttachmentsRecursive(List<MessagePart> parts, String msgId, String from, Instant date, List<Transfer> list, Long uid) {
        if (parts == null) return;

        for (MessagePart part : parts) {
            if (part.getParts() != null) {
                // Pass uid down the recursive chain
                findAttachmentsRecursive(part.getParts(), msgId, from, date, list, uid);
            }

            String filename = part.getFilename();
            if (filename != null && !filename.isEmpty() && part.getBody().getAttachmentId() != null) {
                Integer sizeInt = part.getBody().getSize();
                Long sizeLong = (sizeInt != null) ? sizeInt.longValue() : 0L;
                if(sizeLong < 20480 && part.getMimeType()!=null && part.getMimeType().startsWith("image/")){
                    continue;
                }
                // BOOM. Real UID injected directly into the row.
                Transfer t = new Transfer(
                        uid,
                        msgId,
                        part.getBody().getAttachmentId(),
                        filename,
                        sizeLong,
                        part.getMimeType(),
                        from,
                        date,
                        "google"
                );
                list.add(t);
            }
        }
    }



}
