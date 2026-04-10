package com.emailorch.email_fetcher.controller;


import com.emailorch.email_fetcher.model.User;
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
        // If there's no session, return a 401 Unauthorized
        if (u == null) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthenticated"));
        }

        String email = u.getAttribute("email");
        String name = u.getAttribute("name");
        String pic = u.getAttribute("picture");

        Optional<User> existingUser = userRepo.findByEmail(email);
        if (existingUser.isEmpty()) {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setPic(pic);
            userRepo.save(newUser); // Hits the database!
        }


        // Return the Google profile data as JSON
        return ResponseEntity.ok(Map.of(
                "email", email,
                "name", name,
                "pic", pic
        ));
    }
    

}
