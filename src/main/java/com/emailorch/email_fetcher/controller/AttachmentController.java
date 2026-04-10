package com.emailorch.email_fetcher.controller;

import com.emailorch.email_fetcher.model.Transfer;
import com.emailorch.email_fetcher.service.AttachmentService;
import com.google.api.services.gmail.model.Message;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

@RestController
@RequestMapping("/api/attachments")
@CrossOrigin
public class AttachmentController {
//    TransferRepository transferRepository;
    AttachmentService attachmentService;
    public AttachmentController(AttachmentService attachmentService){
        this.attachmentService=attachmentService;
//         this.transferRepository=transferRepository;
     }

     @PostMapping("/")
    public List<Transfer> attachments() throws IOException {


         return List.of();
     }


     @GetMapping("/atttest")
    public List<Transfer> listAllAttachmentstest() throws IOException, ParseException {
         return attachmentService.listTranfertest();
     }
    @GetMapping("/test")
    public List<Message> listAllAttachmentsTest() throws IOException {
        //return transferRepository.findall();
        return attachmentService.unitTest();
    }


}
