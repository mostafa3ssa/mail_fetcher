package com.emailorch.email_fetcher.controller;

import com.emailorch.email_fetcher.model.Status;
import com.emailorch.email_fetcher.model.Transfer;
import com.emailorch.email_fetcher.provider.CloudProvider;
import com.emailorch.email_fetcher.repository.TransferRepository;
import com.emailorch.email_fetcher.repository.UserRepository;
import com.emailorch.email_fetcher.service.TransferService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/transfers")
@CrossOrigin
public class TransferController {

    private final TransferService svc;
    private final TransferRepository repo;
    private final UserRepository urepo;
    private final CloudProvider cp;       // ← NEW

    public TransferController(TransferService svc, TransferRepository repo,
                              UserRepository urepo, CloudProvider cp) {
        this.svc = svc;
        this.repo = repo;
        this.urepo = urepo;
        this.cp = cp;                     // ← NEW
    }

    // ── POST /api/transfers ─────────────────────────────────────
    @PostMapping
    ResponseEntity<?> create(
            @RequestBody TransferReq req,
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient c,
            @AuthenticationPrincipal OAuth2User u) {

        Long uid = resolveUid(u);

        if (req.msgId == null || req.fname == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "validation_error",
                            "message", "msgId and fname are required"));
        }

        var opt = repo.findByUidAndMsgIdAndFname(uid, req.msgId, req.fname);

        if (opt.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(Map.of("error", "not_found",
                            "message", "No synced attachment found for this msgId and fname"));
        }

        Transfer t = opt.get();

        if (t.getStatus() != null) {
            if (t.getStatus() == Status.FAILED) {
                t.setErr(null);
                t.setS3Key(null);
                t.setDoneAt(null);
            } else if (t.getStatus() == Status.DONE) {
                return ResponseEntity.status(409)
                        .body(Map.of("error", "transfer_already_completed"));
            } else {
                return ResponseEntity.status(409)
                        .body(Map.of("error", "transfer_already_in_progress"));
            }
        }

        t.setStatus(Status.PENDING);
        repo.save(t);

        String tok = c.getAccessToken().getTokenValue();
        svc.exec(t, tok);

        return ResponseEntity.accepted()
                .body(Map.of("id", t.getId(), "status", "PENDING"));
    }

    // ── GET /api/transfers/{id} ─────────────────────────────────
    @GetMapping("/{id}")
    ResponseEntity<?> poll(@PathVariable UUID id) {
        return repo.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404)
                        .body(Map.of("error", "not_found",
                                "message", "Transfer not found")));
    }

    // ── GET /api/transfers/{id}/download ────────────────────────  ← NEW
    @GetMapping("/{id}/download")
    ResponseEntity<?> download(@PathVariable UUID id,
                               @AuthenticationPrincipal OAuth2User u) {

        // 1. Auth check
        if (u == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "unauthenticated"));
        }

        // 2. Find the transfer
        var opt = repo.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(Map.of("error", "not_found",
                            "message", "Transfer not found"));
        }

        Transfer t = opt.get();

        // 3. Verify ownership
        Long uid = resolveUid(u);
        if (!t.getUid().equals(uid)) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "forbidden"));
        }

        // 4. Must be DONE with a valid s3Key
        if (t.getStatus() != Status.DONE || t.getS3Key() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "not_uploaded",
                            "message", "File has not been uploaded yet"));
        }

        // 5. Generate presigned URL (valid 15 min)
        String url = cp.presign(t.getS3Key());

        return ResponseEntity.ok(Map.of(
                "url", url,
                "fname", t.getFname(),
                "expiresIn", 900
        ));
    }

    // ── Helpers ─────────────────────────────────────────────────
    private Long resolveUid(OAuth2User u) {
        String email = u.getAttribute("email");
        return urepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email))
                .getId();
    }

    // ── Request DTO ─────────────────────────────────────────────
    public static class TransferReq {
        public String msgId;
        public String attId;
        public String fname;
        public Long bytes;
    }
}