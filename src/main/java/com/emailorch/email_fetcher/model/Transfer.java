package com.emailorch.email_fetcher.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers")
@Getter
@Setter
@NoArgsConstructor // Required for JPA
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Long uid;

    @Column(nullable = false)
    private String msgId;

    @Column(nullable = false)
    private String attId;

    private String fname;

    private Long bytes;

    @Enumerated(EnumType.STRING) // Maps Enum to VARCHAR in DB
    private Status status = Status.PENDING;

    private String s3Key;

    @Column(columnDefinition = "TEXT")
    private String err;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant doneAt; // Date of cloud upload

    // New metadata fields from Gmail
    private String senderEmail;
    private Instant emailSentAt;

    // Convenience constructor for creating new transfers
    public Transfer(Long uid, String msgId, String attId, String fname, Long bytes, String senderEmail, Instant emailSentAt) {
        this.uid = uid;
        this.msgId = msgId;
        this.attId = attId;
        this.fname = fname;
        this.bytes = bytes;
        this.senderEmail = senderEmail;
        this.emailSentAt = emailSentAt;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
    }
}