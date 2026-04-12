package com.emailorch.email_fetcher.service;

import com.emailorch.email_fetcher.model.Status;
import com.emailorch.email_fetcher.model.Transfer;
import com.emailorch.email_fetcher.provider.CloudProvider;
import com.emailorch.email_fetcher.provider.EmailProvider;
import com.emailorch.email_fetcher.repository.TransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Instant;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final EmailProvider ep;
    private final CloudProvider cp;
    private final TransferRepository repo;

    public TransferService(EmailProvider ep, CloudProvider cp, TransferRepository repo) {
        this.ep = ep;
        this.cp = cp;
        this.repo = repo;
    }

    @Async("tp")
    public void exec(Transfer t, String tok) {
        log.info("Starting stream: {} → {}", t.getMsgId(), t.getFname());

        t.setStatus(Status.STREAMING);
        repo.save(t);

        String k = String.format("uploads/%d/%s/%s",
                t.getUid(), t.getMsgId(), t.getFname());

        try (InputStream in = ep.stream(tok, t.getMsgId(), t.getAttId())) {

            String s3k = cp.upload(k, in, t.getBytes(), t.getMimeType());
            t.setS3Key(s3k);
            t.setStatus(Status.DONE);
            log.info("Stream complete: {} → {}", t.getFname(), s3k);

        } catch (Exception ex) {

            t.setStatus(Status.FAILED);
            t.setErr(ex.getMessage());
            log.error("Stream failed: {} → {}", t.getFname(), ex.getMessage());
        }

        t.setDoneAt(Instant.now());
        repo.save(t);
    }
}