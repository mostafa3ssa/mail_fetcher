package com.emailorch.email_fetcher.model;

import java.time.Instant;

public record Oauth2AuthorizedClient(
        String clientRegistrationId,
        String principalName,
        String accessTokenType,
        byte[] accessTokenValue,
        Instant accessTokenIssuedAt,
        Instant accessTokenExpiresAt,
        String accessTokenScopes,
        byte[] refreshTokenValue,
        Instant refreshTokenIssuedAt,
        Instant createdAt
) {
}
