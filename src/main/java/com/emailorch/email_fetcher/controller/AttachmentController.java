package com.emailorch.email_fetcher.controller;

import com.emailorch.email_fetcher.model.Transfer;
import com.emailorch.email_fetcher.repository.TransferRepository;
import com.emailorch.email_fetcher.service.AttachmentService;
import com.google.api.services.gmail.model.Message;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/attachments")
@CrossOrigin
public class AttachmentController {
    TransferRepository transferRepository;
    AttachmentService attachmentService;
    public AttachmentController(AttachmentService attachmentService,TransferRepository transferRepository){
        this.attachmentService=attachmentService;
         this.transferRepository=transferRepository;
     }

     @GetMapping("/")
    public List<Transfer> attachments() throws IOException {


         return transferRepository.findAll();
     }


     @GetMapping("/atttest")
    public List<Transfer> listAllAttachmentstest() throws IOException, ParseException {
         return (List<Transfer>) attachmentService.listTranfertest().join();
//         CompletableFuture<List<Transfer>> future = attachmentService.listTranfertest();
//
//         // 2. Attach a callback to handle the list when Google finishes responding
//         future.thenAccept(transfers -> {
//             // This code runs in the background thread once the list is full
//             transferRepository.saveAll(transfers);
//             System.out.println("Async processing complete. Saved " + transfers.size() + " items.");
//         });
//
//         // 3. Return immediately to the user so they don't see a "loading" spinner forever
//         return (List<Transfer>) ResponseEntity.ok("Sync started in the background...");
     }
    @GetMapping("/test")
    public List<Message> listAllAttachmentsTest() throws IOException {
        //return transferRepository.findall();
        return attachmentService.unitTest();
    }


}
