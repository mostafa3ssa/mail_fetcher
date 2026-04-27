package com.emailorch.email_fetcher.service;
//TODO uncoment config oauth,email fetcher remove the excludes, userrepo , usercontroller
import com.emailorch.email_fetcher.model.Transfer;
import com.emailorch.email_fetcher.repository.TransferRepository;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Service
public class AttachmentService {
    private final GmailService gmailService;
    private final TransferRepository transferRepository;
    AttachmentService(GmailService gmail,TransferRepository transferRepository){
        this.gmailService =gmail;
        this.transferRepository=transferRepository;
    }


    public List<Message> unitTest() throws IOException {

        String credentiels="";
        var gmailclient =   gmailService.createClient(credentiels);
//        Transfer latest = (Transfer) transferRepository.findLatestTransfers();


        var response  =  gmailclient.users().messages().list("me").setQ("has:attachment").setMaxResults(1L).execute();

        System.out.println(response);

//       System.out.print(hasAttachment(msg));
//       Transfer t = new Transfer();
//           if(hasAttachment(msg)) attachmentsEmails.add(msg) ;
//       return attachmentsEmails;
        List<Message> messages = new ArrayList<>();
        for(Message msg :response.getMessages()){
            Message fullmessage  = gmailclient.users().messages().get("me", msg.getId())
                    .setFormat("full") // "full" returns the payload, headers, and body
                    .execute();

          System.out.println(fullmessage.getPayload().getFilename());
            messages.add(fullmessage);

    }

        return messages;
    }

    public List<Transfer> fetchAndSave(String accessToken, Long uid) {
        List<Transfer> attachmentsEmails = new ArrayList<>();

        try {
            System.out.println(">>> [1] Creating Gmail client for uid=" + uid);
            var gmailClient = gmailService.createClient(accessToken);

            Instant last = transferRepository.findLatestTransfersByUid(uid);
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

                        int before = attachmentsEmails.size();
                        findAttachmentsRecursive(
                                fullmessage.getPayload().getParts(),
                                msg.getId(), from, instantDate,
                                attachmentsEmails, uid
                        );
                        int after = attachmentsEmails.size();
                        System.out.println(">>> [5] Message " + msg.getId() +
                                " → found " + (after - before) + " attachments");
                    }
                }

                pageToken = response.getNextPageToken();
                System.out.println(">>> [6] Next page token: " + pageToken);

            } while (pageToken != null);

            System.out.println(">>> [7] TOTAL attachments found: " + attachmentsEmails.size());
            return attachmentsEmails;

        } catch (Exception e) {
            System.err.println(">>> [ERROR] Gmail fetch failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Gmail sync failed", e);
        }
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
                        date
                );
                list.add(t);
            }
        }
    }

    // Recursive Helper Method

//Done


}
