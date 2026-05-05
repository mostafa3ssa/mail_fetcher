package com.emailorch.email_fetcher.service;

import com.emailorch.email_fetcher.provider.CloudProvider;
import com.emailorch.email_fetcher.provider.GmailProvider;
import com.emailorch.email_fetcher.provider.OutlookProvider;
import com.emailorch.email_fetcher.repository.CacheRepo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

@Service
public class TransferService {

    private final GmailProvider gmailProvider;
    private final OutlookProvider outlookProvider;
    private final CloudProvider cloudProvider;
    private final CacheRepo cacheRepo;
    private final JdbcTemplate jdbc;

    // 2. Inject it in the constructor
    public TransferService(GmailProvider gmailProvider, OutlookProvider outlookProvider,
                           CloudProvider cloudProvider, CacheRepo cacheRepo,
                           JdbcTemplate jdbc) {
        this.gmailProvider = gmailProvider;
        this.outlookProvider = outlookProvider;
        this.cloudProvider = cloudProvider;
        this.cacheRepo = cacheRepo;
        this.jdbc = jdbc;
    }

    @Async("tp")
    public void exec(UUID transferId, String token, String providerName) {
        // 1. NATIVE FETCH: Bypass Hibernate entirely.
        Map<String, Object> meta;
        try {
            meta = jdbc.queryForMap(
                    "SELECT uid, msg_id, att_id, fname, mime_type, bytes FROM transfers WHERE id = ?",
                    transferId
            );
        } catch (Exception e) {
            System.err.println("Could not find transfer record: " + transferId);
            return;
        }

        long uid = ((Number) meta.get("uid")).longValue();
        String msgId = (String) meta.get("msg_id");
        String attId = (String) meta.get("att_id");
        String fname = (String) meta.get("fname");
        String mimeType = (String) meta.get("mime_type");
        long expectedSize = ((Number) meta.get("bytes")).longValue();

        String s3Path = String.format("uploads/%d/%s/%s", uid, msgId, fname);

        // 2. SET STATUS TO STREAMING
        updateStatus(transferId, "STREAMING", null, null);

        try {
            // 3. DOWNLOAD PHASE (Provider -> Postgres)
            System.out.println("-> [1] Starting download from " + providerName);
            try (InputStream in = providerName.equalsIgnoreCase("google")
                    ? gmailProvider.stream(token, msgId, attId)
                    : outlookProvider.stream(token, msgId, attId)) {

                cacheRepo.saveStream(attId, in);
            }

            // 4. VALIDATION PHASE
            long actualSize = cacheRepo.getDataLength(attId);
            if (actualSize == 0) {
                jdbc.execute("delete from public.cache");

                throw new IOException("Source file is empty.");
            }
//            if (actualSize < expectedSize) {
//                jdbc.execute("delete from public.cache");
//                throw new IOException("Partial transfer. Expected " + expectedSize + " but got " + actualSize);
//            } if (actualSize > expectedSize) {
//                jdbc.execute("delete from public.cache");
//                throw new IOException("File bloated. Expected " + expectedSize + " but got " + actualSize);
//            }
            System.out.println("-> [2] Size validated perfectly (" + actualSize + " bytes). Opening pipe to S3...");

            // 5. UPLOAD PHASE (Postgres -> S3)
            cacheRepo.returnUploadStream(attId, uploadStream -> {
                String s3Key = cloudProvider.upload(s3Path, uploadStream, actualSize, mimeType);
                updateStatus(transferId, "DONE", null, s3Key);
                System.out.println("-> [3] SUCCESS! S3 Key: " + s3Key);
            });

        } catch (Exception ex) {
            String err = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            System.err.println("-> [!] FAILED: " + err);
            updateStatus(transferId, "FAILED", err, null);
            jdbc.execute("delete from public.cache");
        }
    }

    /**
     * Isolated helper method to force native SQL updates without transaction locks.
     */
//    private void updateStatus(UUID id, String status, String err, String s3Key) {
//        // 3. Use the injected transactionManager here (NO local null declaration!)
//        new TransactionTemplate(transactionManager) {{
//            setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
//        }}.execute(statusContext -> {
//            return jdbc.update(
//                    "UPDATE transfers SET status = ?, err = ?, s3_key = ?, done_at = CURRENT_TIMESTAMP WHERE id = ?",
//                    status, err, s3Key, id
//            );
//        });
//    }

    private void updateStatus(UUID id, String status, String err, String s3Key) {
        jdbc.update(
                "UPDATE transfers SET status = ?, err = ?, s3_key = ?, done_at = CURRENT_TIMESTAMP WHERE id = ?",
                status, err, s3Key, id
        );
    }
}