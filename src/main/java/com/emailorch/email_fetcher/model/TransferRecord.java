package com.emailorch.email_fetcher.model;

import ch.qos.logback.core.status.Status;

import java.time.Instant;
import java.util.UUID;

public record TransferRecord(UUID id,
                             Long uid,
                             String msgId,
                             String attId,
                             String fname,
                             Long bytes,
                             Status status,
                             String s3Key,
                             String err,
                             Instant createdAt,
                             Instant doneAt) {
}
