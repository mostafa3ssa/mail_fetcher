package com.emailorch.email_fetcher.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("tp")
    Executor tp() {
        var e = new ThreadPoolTaskExecutor();
        e.setCorePoolSize(4);       // 4 concurrent streams at rest
        e.setMaxPoolSize(8);        // burst to 8 if queue fills
        e.setQueueCapacity(50);     // 50 waiting before rejection
        e.setThreadNamePrefix("stream-");
        e.initialize();
        return e;
    }
}