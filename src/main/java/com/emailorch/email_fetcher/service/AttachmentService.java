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
    public CompletableFuture<List<Transfer>> listTranfertest() {
        try {
            List<Transfer> attachmentsEmails = new ArrayList<>();
            // Hardcoded for testing - remember this token expires!
            String credentiels = "";
            String query = "has:attachment";
            Instant last = transferRepository.findLatestTransfers();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneId.systemDefault());
            String date = dateTimeFormatter.format(last);
            if(last==null){
                query = "has:attachment";
            }else {
                query ="has:attachment after:"+date;
            }
            var gmailClient = gmailService.createClient(credentiels);
            String pageToken = null;

            // The do-while loop guarantees we fetch at least once, and keep going as long as there is a next page
            do {
                // 1. Build the request
                var request = gmailClient.users().messages().list("me").setQ(query);

                // 2. If we have a token from a previous loop, inject it to get the next page
                if (pageToken != null) {
                    request.setPageToken(pageToken);
                }

                // Execute the request for this specific page
                var response = request.execute();

                if (response.getMessages() != null) {
                    for (Message msg : response.getMessages()) {
                        // Fetch full details for each message in the current page
                        Message fullmessage = gmailClient.users().messages().get("me", msg.getId()).execute();

                        String from = null;
                        Instant instantDate = null;

                        // Extract Headers
                        for (MessagePartHeader header : fullmessage.getPayload().getHeaders()) {
                            if (header.getName().equalsIgnoreCase("from")) from = header.getValue();
                            if (header.getName().equalsIgnoreCase("date")) {
                                try {
                                    String dateValue = header.getValue().replaceAll("\\s\\(.*\\)$", "");
                                    SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
                                    instantDate = format.parse(dateValue).toInstant();
                                } catch (Exception e) {
                                    instantDate = Instant.ofEpochMilli(fullmessage.getInternalDate());
                                }
                            }
                        }

                        // Recursively dig out every attachment in the payload
                        findAttachmentsRecursive(fullmessage.getPayload().getParts(), msg.getId(), from, instantDate, attachmentsEmails);
                    }
                }

                // 3. Update the pageToken. If Gmail has more pages, this gets a new string. If we are done, it becomes null.
                pageToken = response.getNextPageToken();

            } while (pageToken != null); // Loop repeats until pageToken is null

            return CompletableFuture.completedFuture(attachmentsEmails);

        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    // Keep your existing recursive helper method right below it!
    private void findAttachmentsRecursive(List<MessagePart> parts, String msgId, String from, Instant date, List<Transfer> list) {
        if (parts == null) return;

        for (MessagePart part : parts) {
            if (part.getParts() != null) {
                findAttachmentsRecursive(part.getParts(), msgId, from, date, list);
            }

            String filename = part.getFilename();
            if (filename != null && !filename.isEmpty() && part.getBody().getAttachmentId() != null) {
                Integer sizeInt = part.getBody().getSize();
                Long sizeLong = (sizeInt != null) ? sizeInt.longValue() : 0L;

                Transfer t = new Transfer(
                        30L,
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
