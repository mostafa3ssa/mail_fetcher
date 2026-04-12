package com.emailorch.email_fetcher.provider;

import java.io.InputStream;

public interface EmailProvider {

    /**
     * Returns a LIVE InputStream of decoded attachment bytes.
     * Caller MUST close this stream (use try-with-resources).
     * Implementation MUST NOT buffer the entire file into a byte[].
     */
    InputStream stream(String tok, String msgId, String attId);
}