package com.emailorch.email_fetcher.provider;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

public class R2Provider implements CloudProvider {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bkt;

    public R2Provider(S3Client s3, S3Presigner presigner, String bkt) {
        this.s3 = s3;
        this.presigner = presigner;
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

    @Override
    public String presign(String key) {
        var getReq = GetObjectRequest.builder()
                .bucket(bkt)
                .key(key)
                .build();

        var presignReq = GetObjectPresignRequest.builder()
                .getObjectRequest(getReq)
                .signatureDuration(Duration.ofMinutes(15))
                .build();

        return presigner.presignGetObject(presignReq).url().toString();
    }
}