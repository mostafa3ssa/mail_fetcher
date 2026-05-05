package com.emailorch.email_fetcher.service;

import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.authentication.AccessTokenProvider;
import com.microsoft.kiota.authentication.AllowedHostsValidator;
import com.microsoft.kiota.authentication.BaseBearerTokenAuthenticationProvider;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Map;

@Service
public class OutlookService {

    public GraphServiceClient createClient(String accessToken) {

        // You have to implement both methods because it's not a pure Functional Interface
        AccessTokenProvider tokenProvider = new AccessTokenProvider() {
            @Override
            public String getAuthorizationToken(URI uri, Map<String, Object> additionalAuthenticationContext) {
                return accessToken; // Returns your token string
            }

            @Override
            public AllowedHostsValidator getAllowedHostsValidator() {
                // This tells the client it's safe to send the token to Microsoft Graph
                return new AllowedHostsValidator("graph.microsoft.com");
            }
        };

        BaseBearerTokenAuthenticationProvider authProvider =
                new BaseBearerTokenAuthenticationProvider(tokenProvider);

        return new GraphServiceClient(authProvider);
    }
}