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
    GmailService gmailService;
    TransferRepository transferRepository;
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
    //TODO this is for test only
    @Async
    public CompletableFuture<List<Transfer>> fetchAndSave(String accessToken, Long uid) {
        try {
            List<Transfer> attachmentsEmails = new ArrayList<>();

            // 1. Create the client with the REAL Google Access Token! No more hardcoded strings.
            var gmailClient = gmailService.createClient(accessToken);

            // 2. Build the query
            // IMPORTANT PRO-TIP: Since you now have multiple users, you need to update
            // your repository to find the latest transfer FOR THIS SPECIFIC UID.
            // Change this to something like: transferRepository.findLatestTransferDateByUid(uid);
            Instant last = transferRepository.findLatestTransfersByUid(uid);

            String query = "has:attachment";
            if (last != null) {
                // Subtracting 1 day (86400 seconds) just to be perfectly safe with the overlap
                long safeEpoch = last.getEpochSecond() - 86400;
                query = "has:attachment after:" + safeEpoch;
            }

            String pageToken = null;

            // 3. The Gmail API Pagination Loop
            do {
                // Build the request
                var request = gmailClient.users().messages().list("me").setQ(query);

                // Inject token for the next page if it exists
                if (pageToken != null) {
                    request.setPageToken(pageToken);
                }

                // Execute the request
                var response = request.execute();

                if (response.getMessages() != null) {
                    for (Message msg : response.getMessages()) {

                        // Fetch full details for each message
                        Message fullmessage = gmailClient.users().messages().get("me", msg.getId()).execute();

                        String from = null;
                        Instant instantDate = null;

                        // Extract Headers (Sender and Date)
                        for (MessagePartHeader header : fullmessage.getPayload().getHeaders()) {
                            if (header.getName().equalsIgnoreCase("from")) from = header.getValue();
                            if (header.getName().equalsIgnoreCase("date")) {
                                try {
                                    String dateValue = header.getValue().replaceAll("\\s\\(.*\\)$", "");
                                    SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
                                    instantDate = format.parse(dateValue).toInstant();
                                } catch (Exception e) {
                                    // Fallback to Google's internal timestamp if the header fails to parse
                                    instantDate = Instant.ofEpochMilli(fullmessage.getInternalDate());
                                }
                            }
                        }

                        // 4. Dig out the attachments and PASS THE UID DOWN
                        findAttachmentsRecursive(
                                fullmessage.getPayload().getParts(),
                                msg.getId(),
                                from,
                                instantDate,
                                attachmentsEmails,
                                uid // <--- Injected here!
                        );
                    }
                }

                // Update the pageToken. Null means we reached the end of the inbox.
                pageToken = response.getNextPageToken();

            } while (pageToken != null);

            return CompletableFuture.completedFuture(attachmentsEmails);

        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
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
