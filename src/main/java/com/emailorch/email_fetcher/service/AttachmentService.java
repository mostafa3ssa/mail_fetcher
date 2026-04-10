package com.emailorch.email_fetcher.service;

import com.emailorch.email_fetcher.model.Transfer;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
@Service
public class AttachmentService {
    GmailService gmailService;
    AttachmentService(GmailService gmail){
        this.gmailService =gmail;
    }
    public List<Transfer> fetchAttachment() throws IOException {
        String credentiels=System.getenv("GMAIL_TOKEN");
        var gmailclient =   gmailService.createClient(credentiels);
        Long maxResults= 40L;
        var response  =  gmailclient.users().messages().list("me").setQ("has:attachment").setMaxResults(maxResults).execute();
        List<Transfer> attachmentsEmails = new ArrayList<>();
        int count = 0;
        for ( var msg : response.getMessages() ){
            var message = gmailclient.users().messages()
                    .get("me", msg.getId())
                    .execute();

            System.out.println(hasAttachment(message));

            if(!hasAttachment(message)) continue;
            Transfer t =  new Transfer();




        }


        System.out.println(attachmentsEmails);
        //transferRepository.save(attachmentsEmails);

        return attachmentsEmails;



    }
    private boolean hasAttachment(Message msg){
        if(msg.getPayload()!=null){
            var body = msg.getPayload().getBody();
            System.out.println(body);
            if(body==null){
                return false;
            }
            if(body.getAttachmentId()==null) {
                System.out.println(body.getAttachmentId());
                return false;
            }
            return true;


        }

        return false;
    }
    public List<Message> unitTest() throws IOException {

        String credentiels=System.getenv("GMAIL_TOKEN");
        var gmailclient =   gmailService.createClient(credentiels);
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
    public List<Transfer> listTranfertest() throws IOException, ParseException {
        List<Transfer> attachmentsEmails = new ArrayList<>();
        String credentiels=System.getenv("GMAIL_TOKEN");
        var gmailClient  = gmailService.createClient(credentiels);
        Long results = 20L;
        var response = gmailClient.users().messages().list("me").setQ("has:attachment").setMaxResults(results).execute();
        for(Message msg :response.getMessages()) {
            Message fullmessage = gmailClient.users().messages().get("me", msg.getId()).execute();

            String from = null;
            Instant instantDate = null;
            for (MessagePartHeader header : fullmessage.getPayload().getHeaders()) {
                if (header.getName().equalsIgnoreCase("from")) {
                    from = header.getValue();
                    break;
                }
                if (header.getName().equalsIgnoreCase("date")) {
                    SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
                    Date date = format.parse(header.getValue());
                    instantDate = date.toInstant();

                }


            }
            String attachmentID = null;
            String filename = null;
            Long size = null;
            String mimetype = null;
            List<MessagePart> parts = fullmessage.getPayload().getParts();
            if(parts!=null) {
            for (MessagePart part : parts) {

                    filename = part.getFilename();
                    if (filename != "" || !filename.isEmpty()) {
                        attachmentID = part.getBody().getAttachmentId();
//                        size = (long) part.getBody().size();
                        size = Long.valueOf(part.getBody().getSize());
                        mimetype = part.getMimeType();

                    }
                }
            }

//   Long uid,
//    String msgId,
//    String attId,
//    String fname,
//    Long bytes,
//    String mimeType,
//    String senderEmail,
//    Instant emailSentAt
            Transfer t = new Transfer(30L, msg.getId(), attachmentID, filename, size, mimetype, from, instantDate);
            attachmentsEmails.add(t);

        }




        return  attachmentsEmails;
    }

//    private List<Transfer> firstLogin(){
//        return  ;
//    }
//    private List<Transfer> normlLoging(){
//        return ;
//    }
}
