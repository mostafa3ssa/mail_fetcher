package com.emailorch.email_fetcher.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityCfg {

    // 1. Create the Supabase Token Service
    @Bean
    public OAuth2AuthorizedClientService acs(JdbcOperations jdbc, ClientRegistrationRepository crr) {
        return new JdbcOAuth2AuthorizedClientService(jdbc, crr);
    }

    // 2. Wire it into the Filter Chain
    @Bean
    public SecurityFilterChain fc(HttpSecurity h, OAuth2AuthorizedClientService acs) throws Exception {
        return h
                .csrf(c -> c.disable())
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/", "/error", "/oauth2/**", "/login/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(o -> o
                        .defaultSuccessUrl("/api/me", true)
                        .authorizedClientService(acs) // <--- THIS SAVES THE TOKEN TO SUPABASE
                )
                .logout(l -> l.logoutSuccessUrl("/"))
                .build();
    }
}
