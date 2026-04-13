package com.emailorch.email_fetcher.config;

import com.emailorch.email_fetcher.model.User;
import com.emailorch.email_fetcher.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository repo;

    public OAuth2LoginSuccessHandler(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req,
                                        HttpServletResponse res,
                                        Authentication auth) throws IOException, ServletException {

        OAuth2User u = (OAuth2User) auth.getPrincipal();

        String email = u.getAttribute("email");
        String name = u.getAttribute("name");
        String pic = u.getAttribute("picture");

        // Create user if not exists — runs BEFORE redirect
        if (repo.findByEmail(email).isEmpty()) {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setPic(pic);
            repo.save(newUser);
            System.out.println(">>> User created: " + email);
        } else {
            System.out.println(">>> User exists: " + email);
        }

        // NOW redirect to frontend — user is guaranteed in DB
        res.sendRedirect("http://localhost:5173/");
    }
}