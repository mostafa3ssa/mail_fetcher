package com.emailorch.email_fetcher.provider;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;

public class R2Provider implements CloudProvider {

    private final S3Client s3;
    private final String bkt;

    public R2Provider(S3Client s3, String bkt) {
        this.s3 = s3;
        this.bkt = bkt;
    }

    @Override
    public String upload(String key, InputStream in, long len, String mime) {
        var req = PutObjectRequest.builder()
                .bucket(bkt)
                .key(key)
                .contentType(mime)
                .contentLength(len)
                .build();

        s3.putObject(req, RequestBody.fromInputStream(in, len));
        return key;
    }
}