package com.emailorch.email_fetcher.controller;


import com.emailorch.email_fetcher.model.User;
//import com.emailorch.email_fetcher.repository.UserRepository;
import com.emailorch.email_fetcher.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/me")
public class UserController {

    private final UserRepository userRepo;

    public UserController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping("")
    public ResponseEntity<?> me(@AuthenticationPrincipal OAuth2User u) {
        if (u == null) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthenticated"));
        }

        String email = u.getAttribute("email");

        // User is GUARANTEED to exist — created during login
        User dbUser = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        return ResponseEntity.ok(Map.of(
                "id", dbUser.getId(),
                "email", dbUser.getEmail(),
                "name", dbUser.getName(),
                "pic", dbUser.getPic()
        ));
    }
}
