package com.emailorch.email_fetcher.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/me")
public class UserController {

    @GetMapping("")
    public ResponseEntity<?> me(@AuthenticationPrincipal OAuth2User u) {
        // If there's no session, return a 401 Unauthorized
        if (u == null) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthenticated"));
        }

        // Return the Google profile data as JSON
        return ResponseEntity.ok(Map.of(
                "email", u.getAttribute("email"),
                "name", u.getAttribute("name"),
                "pic", u.getAttribute("picture")
        ));
    }
    

}
