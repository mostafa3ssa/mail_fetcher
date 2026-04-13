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

    private final DataSource dataSource;
    private final OAuth2LoginSuccessHandler successHandler;  // ← inject

    public SecurityCfg(DataSource dataSource, OAuth2LoginSuccessHandler successHandler) {
        this.dataSource = dataSource;
        this.successHandler = successHandler;
    }

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository) {
        return new JdbcOAuth2AuthorizedClientService(
                new org.springframework.jdbc.core.JdbcTemplate(dataSource),
                clientRegistrationRepository
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   OAuth2AuthorizedClientService acs) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/error", "/oauth2/**", "/login/**", "/api/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(successHandler)      // ← replaces defaultSuccessUrl
                        .authorizedClientService(acs)
                )
                .logout(logout -> logout.logoutSuccessUrl("/"));

        return http.build();
    }
}