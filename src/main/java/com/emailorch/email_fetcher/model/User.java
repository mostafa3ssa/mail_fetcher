package com.emailorch.email_fetcher.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor // Required by JPA for instantiation
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String name;

    private String pic;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Field to track the last time they logged in
    private Instant lastLoginAt;

    // Convenience constructor for new users
    public User(String email, String name, String pic) {
        this.email = email;
        this.name = name;
        this.pic = pic;
        this.createdAt = Instant.now();
        this.lastLoginAt = Instant.now();
    }
}