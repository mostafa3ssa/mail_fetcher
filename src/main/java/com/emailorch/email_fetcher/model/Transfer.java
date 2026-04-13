package com.emailorch.email_fetcher.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transfers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"uid", "msg_id", "fname"})
})
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Long uid;

    @Column(name = "msg_id")
    private String msgId;

    @Column(name = "att_id", columnDefinition = "TEXT")
    private String attId;

    @Column(columnDefinition = "TEXT")
    private String fname;
    private Long bytes;

    @Column(name = "mime_type")
    private String mimeType;

    @Enumerated(EnumType.STRING)
    private Status status;           // null = synced, never uploaded

    @Column(name = "s3_key", length = 1000)
    private String s3Key;

    @Column(columnDefinition = "TEXT")
    private String err;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "done_at")
    private Instant doneAt;

    @Column(name = "sender_email", columnDefinition = "TEXT")
    private String senderEmail;

    @Column(name = "email_sent_at")
    private Instant emailSentAt;

    // 1. JPA no-args (required)
    public Transfer() {}

    // 2. Sync constructor (Gmail discovery — status is NULL)
    public Transfer(Long uid, String msgId, String attId, String fname,
                    Long bytes, String mimeType, String senderEmail, Instant emailSentAt) {
        this.uid = uid;
        this.msgId = msgId;
        this.attId = attId;
        this.fname = fname;
        this.bytes = bytes;
        this.mimeType = mimeType;
        this.senderEmail = senderEmail;
        this.emailSentAt = emailSentAt;
        this.status = null;            // ← DISCOVERED, NOT UPLOADED
        this.createdAt = Instant.now();
    }

    // Getters and Setters (unchanged)
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Long getUid() { return uid; }
    public void setUid(Long uid) { this.uid = uid; }

    public String getMsgId() { return msgId; }
    public void setMsgId(String msgId) { this.msgId = msgId; }

    public String getAttId() { return attId; }
    public void setAttId(String attId) { this.attId = attId; }

    public String getFname() { return fname; }
    public void setFname(String fname) { this.fname = fname; }

    public Long getBytes() { return bytes; }
    public void setBytes(Long bytes) { this.bytes = bytes; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }

    public String getErr() { return err; }
    public void setErr(String err) { this.err = err; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getDoneAt() { return doneAt; }
    public void setDoneAt(Instant doneAt) { this.doneAt = doneAt; }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public Instant getEmailSentAt() { return emailSentAt; }
    public void setEmailSentAt(Instant emailSentAt) { this.emailSentAt = emailSentAt; }
}