package com.emailorch.email_fetcher.provider;

import java.io.InputStream;

public interface CloudProvider {

    /**
     * Uploads from an InputStream to cloud storage using chunked I/O.
     * Returns the final storage key.
     */
    String upload(String key, InputStream in, long len, String mime);
    String presign(String key);
}