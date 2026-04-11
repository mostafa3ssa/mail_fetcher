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

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
public class SecurityCfg {

    // Inject DataSource - Spring uses this to create the JdbcOperations bean internally
    private final DataSource dataSource;

    public SecurityCfg(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // 1. Create the Supabase Token Service using JdbcTemplate
    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
        return new JdbcOAuth2AuthorizedClientService(new org.springframework.jdbc.core.JdbcTemplate(dataSource), clientRegistrationRepository);
    }

    // 2. The SINGLE Filter Chain
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, OAuth2AuthorizedClientService acs) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Permitting these allows Postman to work without the redirect loop
                        .requestMatchers("/", "/error", "/oauth2/**", "/login/**", "/api/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/api/attachments", true)
                        .authorizedClientService(acs) // Saves the Google Token to your Supabase DB
                )
                .logout(logout -> logout.logoutSuccessUrl("/"));

        return http.build();
    }
}