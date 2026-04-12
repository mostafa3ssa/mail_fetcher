package com.emailorch.email_fetcher.provider;

import com.emailorch.email_fetcher.service.GmailService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class GmailProvider implements EmailProvider {

    private final GmailService svc;

    public GmailProvider(GmailService svc) {
        this.svc = svc;
    }

    @Override
    public InputStream stream(String tok, String msgId, String attId) {
        try {
            var g = svc.createClient(tok);

            // Fetch attachment — returns base64url-encoded "data" field
            var body = g.users().messages().attachments()
                    .get("me", msgId, attId)
                    .execute();

            // body.getData() = base64url string
            // We decode it as a STREAM, not a full byte[]
            byte[] b64 = body.getData().getBytes(StandardCharsets.US_ASCII);
            return Base64.getUrlDecoder().wrap(new ByteArrayInputStream(b64));

            // ⚠️ The base64 STRING is in memory (Gmail API limitation)
            // But the DECODED binary bytes are emitted chunk-by-chunk
            // by the Base64 decoder wrapper. The decoded file is never
            // a single byte[] in RAM.

        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}