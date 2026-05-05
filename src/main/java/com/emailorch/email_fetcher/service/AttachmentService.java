package com.emailorch.email_fetcher.service;
//TODO uncoment config oauth,email fetcher remove the excludes, userrepo , usercontroller
//import com.azure.core.http.rest.Page;
import com.emailorch.email_fetcher.model.Transfer;
import com.emailorch.email_fetcher.repository.TransferRepository;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.microsoft.graph.models.Attachment;
import com.microsoft.graph.models.AttachmentCollectionResponse;
import com.microsoft.graph.models.FileAttachment;
import com.microsoft.graph.models.MessageCollectionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;

@Service
public class AttachmentService {
    private final GmailService gmailService;
    private final TransferRepository transferRepository ;
    private final OutlookService outlookService;
    private final OAuth2AuthorizedClientService clientService;
    private final EmailFetchingAsyncService emailFetchingAsyncService;

    AttachmentService(GmailService gmail,TransferRepository transferRepository,OutlookService outlookService,OAuth2AuthorizedClientService clientService,EmailFetchingAsyncService emailFetchingAsyncService) throws Exception {
        this.gmailService =gmail;
        this.outlookService=outlookService;
        this.clientService=clientService;
        this.transferRepository=transferRepository;
        this.emailFetchingAsyncService=emailFetchingAsyncService;
    }


    public List<Message> unitTest() throws IOException {

        String credentiels="";
        var gmailclient =   gmailService.createClient(credentiels);
//        Transfer latest = (Transfer) transferRepository.findLatestTransfers();


        var response  =  gmailclient.users().messages().list("me").setQ("has:attachment").setMaxResults(1L).execute();

        System.out.println(response);

//       System.out.print(hasAttachment(msg));
//       Transfer t = new Transfer();
//           if(hasAttachment(msg)) transfers.add(msg) ;
//       return transfers;
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
/// provider
///


    // Recursive Helper Method

//Done

    /// [
    ///{
    ///     "id": "fdcba37b-aed8-496f-88bc-4d3a0d3f29e6",
    ///     "att_id": "ANGjdJ9WeFSIHCkOyt9Rq6GLtMK6FMidTNzMS-CL2bVhdClQJPPnL_A1j_7aGZVM9-68Ninj_8K8M0lHMh4eVgwBwVvMpaCzzUXvnoBFwBwxYLH1B7Jig16sMd7_uUBKMmICQQSFuMtce9q7WMI9AvwzzJaNjG7dR2-EzNagg1_q6xyj9L6bac6WQPvDCbYqd-v_ZZK63kVGxFEDoIfdWnQvG5qku2mCSTE_5MAof4pWsk-VavDld-R1KcnSg-rN2Pw2GmZPctaLWpD-ZP0QMxl4xnq-_vw3AruBgc63q1XCA0rZfhGX0436tY1dH6Bh3-zarSh5izOER9_D9FQ1sxi_mpbu56Ysr1Uj1Qq5jxcPB7aLGOHoM1Z0ZJPHKu70xZhDkWac5o2BtOVWYK_S",
    ///     "bytes": 258583,
    ///     "created_at": "2026-04-13 08:36:24.83135+00",
    ///     "done_at": null,
    ///     "email_sent_at": "2024-09-08 22:33:38+00",
    ///     "err": null,
    ///     "fname": "INS_شهادة المقرر باللغة العربية.pdf",
    ///     "mime_type": "application/pdf",
    ///     "msg_id": "191d3c57725811ef",
    ///     "s3_key": null,
    ///     "sender_email": "\"لا ترسل رد على هذا البريد الإلكتروني\" <noreply@maharatech.gov.eg>",
    ///     "status": null,
    ///     "uid": 12
    ///   }
    /// ]

public Map<String, Object>  response (Long uid,int page,int size){
        Map<String,Object> response = new LinkedHashMap<>();
        int safesize= Math.min(size,50);
        Pageable pageable= PageRequest.of(page,safesize);
        Page<Transfer> transferPage = (Page<Transfer>) transferRepository.findByUidOrderByEmailSentAtDesc(uid, pageable);
        response.put("content", transferPage.getContent());
        response.put("page", transferPage.getNumber());
        response.put("size", transferPage.getSize());
        response.put("totalElements", transferPage.getTotalElements());
        response.put("totalPages", transferPage.getTotalPages());
        response.put("first", transferPage.isFirst());
        response.put("last", transferPage.isLast());
        response.put("empty", transferPage.isEmpty());



    return response;
}



    public void syncall(Long uid,String uname , boolean sync ,String provider) throws Exception {

        OAuth2AuthorizedClient googleClinet  = clientService.loadAuthorizedClient("google",uname);
        if(googleClinet!=null){
            System.out.println("FETCHING FOR GMAIL");
            emailFetchingAsyncService.syncMessages(uid,uname,sync,"google");
        }

        OAuth2AuthorizedClient microsoftClinet  = clientService.loadAuthorizedClient("microsoft",uname);
        if(microsoftClinet!=null){
            System.out.println("FETCHING FOR outlook");
            emailFetchingAsyncService.syncMessages(uid,uname,sync,"microsoft");
        }
    }



    public  List<Transfer> outlookmessagestest() throws Exception {
        return null;
    }
}
