package com.emailorch.email_fetcher.config;

import com.emailorch.email_fetcher.provider.*;
import com.emailorch.email_fetcher.service.GmailService;
import com.emailorch.email_fetcher.service.OutlookService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class StreamCfg {

    @Bean("gmail")
    GmailProvider ep(GmailService svc) {
        return new GmailProvider(svc);

        // Swap to: return new ImapProvider(...);
        // Swap to: return new GraphProvider(...);
    }

    @Bean("outlook")
    OutlookProvider op(OutlookService svc){
        return new OutlookProvider(svc);
    }
    @Bean
    CloudProvider cp(S3Client s3, S3Presigner presigner, @Value("${app.r2.bucket}") String bkt) {
        return new R2Provider(s3, presigner, bkt);
    }
}