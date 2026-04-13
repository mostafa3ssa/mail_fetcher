package com.emailorch.email_fetcher.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class R2Config {

    @Value("${app.r2.account-id}")
    private String acct;

    @Value("${app.r2.access-key}")
    private String ak;

    @Value("${app.r2.secret-key}")
    private String sk;

    @Bean
    S3Client s3() {
        var creds = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(ak, sk)
        );
        return S3Client.builder()
                .endpointOverride(URI.create("https://" + acct + ".r2.cloudflarestorage.com"))
                .region(Region.of("auto"))
                .credentialsProvider(creds)
                .build();
    }

    @Bean
    S3Presigner presigner() {
        var creds = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(ak, sk)
        );
        return S3Presigner.builder()
                .endpointOverride(URI.create("https://" + acct + ".r2.cloudflarestorage.com"))
                .region(Region.of("auto"))
                .credentialsProvider(creds)
                .build();
    }
}