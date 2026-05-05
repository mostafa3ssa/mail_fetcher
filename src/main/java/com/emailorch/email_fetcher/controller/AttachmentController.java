package com.emailorch.email_fetcher.controller;

import com.emailorch.email_fetcher.model.Transfer;
import com.emailorch.email_fetcher.model.User;
import com.emailorch.email_fetcher.repository.TransferRepository;
import com.emailorch.email_fetcher.repository.UserRepository;
import com.emailorch.email_fetcher.service.AttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attachments")
@CrossOrigin
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final TransferRepository transferRepository;
    private final UserRepository userRepository;
    private final OAuth2AuthorizedClientService clientService;
    @Autowired
    public AttachmentController(AttachmentService attachmentService,
                                TransferRepository transferRepository,
                                UserRepository userRepository,
                                OAuth2AuthorizedClientService clientService) {
        this.attachmentService = attachmentService;
        this.transferRepository = transferRepository;
        this.userRepository = userRepository;
        this.clientService = clientService;
    }
    /// REQuest Parma
    /// path variable
    /// consumes json
    ///
    @GetMapping
    public ResponseEntity<?> listAttachments(
            @AuthenticationPrincipal OAuth2User u,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean sync,
            Authentication auth
    ) throws Exception {
        // 1. Security Check
        if (u == null) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthenticated"));
        }

        // 2. Identify the User
        String email = u.getAttribute("email");
        User dbUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Long uid = dbUser.getId();
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) auth;
        String provider = token.getAuthorizedClientRegistrationId();
        attachmentService.syncall(uid,u.getName(),sync,provider);
        Map<String, Object> response = attachmentService.response(uid,page,size);
        return ResponseEntity.ok(response);
    }
}