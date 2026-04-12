package com.emailorch.email_fetcher.controller;

import com.emailorch.email_fetcher.model.Transfer;
import com.emailorch.email_fetcher.model.User;
import com.emailorch.email_fetcher.repository.TransferRepository;
import com.emailorch.email_fetcher.repository.UserRepository;
import com.emailorch.email_fetcher.service.AttachmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
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

    public AttachmentController(AttachmentService attachmentService,
                                TransferRepository transferRepository,
                                UserRepository userRepository,
                                OAuth2AuthorizedClientService clientService) {
        this.attachmentService = attachmentService;
        this.transferRepository = transferRepository;
        this.userRepository = userRepository;
        this.clientService = clientService;
    }

    @GetMapping
    public ResponseEntity<?> listAttachments(
            @AuthenticationPrincipal OAuth2User u,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean sync
    ) {
        // 1. Security Check
        if (u == null) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthenticated"));
        }

        // 2. Identify the User
        String email = u.getAttribute("email");
        User dbUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Long uid = dbUser.getId();

        // 3. Enforce the Frontend's Size Limit (Max 50)
        int safeSize = Math.min(size, 50);
        Pageable pageable = PageRequest.of(page, safeSize);

        // 4. Implement the "Sync Logic" from your documentation
        long dbCount = transferRepository.countByUid(uid);
        boolean needsSync = dbCount == 0 || sync;

        if(needsSync) {
            // Double-check: only sync if still 0 (another request might have just finished)
            synchronized (this) {
                dbCount = transferRepository.countByUid(uid);
                if (dbCount > 0 && !sync) {
                    needsSync = false;
                }
            }
        }

        if (needsSync) {
            OAuth2AuthorizedClient client = clientService.loadAuthorizedClient("google", u.getName());
            String accessToken = client.getAccessToken().getTokenValue();

            List<Transfer> fetchedTransfers = attachmentService.fetchAndSave(accessToken, uid);
            System.out.println(">>> FETCHED: " + fetchedTransfers.size());

            // Dedup in memory: msgId + fname is the ONLY reliable unique key
            Map<String, Transfer> uniqueBatch = new java.util.HashMap<>();
            for (Transfer t : fetchedTransfers) {
                // E.g., "18b9c1a2b3:report.pdf"
                String key = t.getMsgId() + ":" + t.getFname();
                uniqueBatch.put(key, t);
            }

            // Check DB: skip rows that already exist
            List<Transfer> toSave = new java.util.ArrayList<>();
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
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                    // Genuine race condition — another request saved the same rows
                    System.out.println(">>> Race condition: " + e.getMessage());
                    // Save one by one to rescue the non-duplicate rows
                    for (Transfer t : toSave) {
                        try {
                            transferRepository.save(t);
                        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                            // This specific row was the duplicate — skip it
                        }
                    }
                }
            }
        }

        // 5. Query the Database for the requested page
        Page<Transfer> transferPage = transferRepository.findByUidOrderByEmailSentAtDesc(uid, pageable);

        // 6. Map to EXACTLY match the Frontend JSON Documentation
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", transferPage.getContent());
        response.put("page", transferPage.getNumber());
        response.put("size", transferPage.getSize());
        response.put("totalElements", transferPage.getTotalElements());
        response.put("totalPages", transferPage.getTotalPages());
        response.put("first", transferPage.isFirst());
        response.put("last", transferPage.isLast());
        response.put("empty", transferPage.isEmpty());

        return ResponseEntity.ok(response);
    }
}