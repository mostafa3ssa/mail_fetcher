package com.emailorch.email_fetcher.model;

import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.time.LocalDateTime;

public record User(
        Long id,
        String email,
        String name,
        String pic,
        Instant createdAt

) {
}
