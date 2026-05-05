package com.emailorch.email_fetcher.controller;

import com.emailorch.email_fetcher.model.Transfer;
import com.emailorch.email_fetcher.service.AttachmentService;
import com.google.api.services.gmail.model.Message;
import com.microsoft.graph.models.AttachmentCollectionResponse;
import com.microsoft.graph.models.MessageCollectionResponse;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/test")
@CrossOrigin
public class TestController {

    private final AttachmentService attachmentService;

    // Use constructor injection properly
    public TestController(AttachmentService attachmentService){
        this.attachmentService = attachmentService;
    }

    @GetMapping("/outlook")
    public List<Transfer> outlooktest() throws Exception {
        // I'm assuming your service returns List<Message>
        // rather than List<MessageCollectionResponse>
        return attachmentService.outlookmessagestest();
    }
}

