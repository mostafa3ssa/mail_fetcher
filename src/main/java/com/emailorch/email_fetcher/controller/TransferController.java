package com.emailorch.email_fetcher.controller;

import com.emailorch.email_fetcher.model.Status;
import com.emailorch.email_fetcher.model.Transfer;
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

    public TransferController(TransferService svc, TransferRepository repo, UserRepository urepo) {
        this.svc = svc;
        this.repo = repo;
        this.urepo = urepo;
    }

    // ── POST /api/transfers ─────────────────────────────────────
    @PostMapping
    ResponseEntity<?> create(
            @RequestBody TransferReq req,
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient c,
            @AuthenticationPrincipal OAuth2User u) {

        // 1. Resolve user
        Long uid = resolveUid(u);

        // [CHANGE 1]: Validate 'fname' instead of 'attId'
        if (req.msgId == null || req.fname == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "validation_error",
                            "message", "msgId and fname are required"));
        }

        // [CHANGE 2]: Call the new Repository method using 'fname'
        var opt = repo.findByUidAndMsgIdAndFname(uid, req.msgId, req.fname);

        if (opt.isEmpty()) {
            // [CHANGE 3]: Update the error message so your frontend devs aren't confused
            return ResponseEntity.status(404)
                    .body(Map.of("error", "not_found",
                            "message", "No synced attachment found for this msgId and fname"));
        }

        Transfer t = opt.get();

        // 4. Validate status
        if (t.getStatus() != null) {
            if (t.getStatus() == Status.FAILED) {
                // Retry allowed — reset error fields
                t.setErr(null);
                t.setS3Key(null);
                t.setDoneAt(null);
            } else if (t.getStatus() == Status.DONE) {
                return ResponseEntity.status(409)
                        .body(Map.of("error", "transfer_already_completed"));
            } else {
                // PENDING or STREAMING
                return ResponseEntity.status(409)
                        .body(Map.of("error", "transfer_already_in_progress"));
            }
        }

        // 5. Set PENDING and save
        t.setStatus(Status.PENDING);
        repo.save(t);

        // 6. Dispatch async — returns instantly
        String tok = c.getAccessToken().getTokenValue();
        svc.exec(t, tok);

        // 7. Return 202
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