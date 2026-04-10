package com.emailorch.email_fetcher.model;

import java.time.Instant;

public record UserRecord(
        Long id,
        String email,
        String name,
        String pic,
        Instant createdAt

) {
}
