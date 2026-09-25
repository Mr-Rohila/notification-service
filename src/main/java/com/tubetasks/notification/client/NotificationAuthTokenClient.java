package com.tubetasks.notification.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tubetasks.notification.common.NotificationServiceProperties;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class NotificationAuthTokenClient {

    private final RestClient restClient;
    private final ServiceAddressResolver addressResolver;
    private final NotificationServiceProperties properties;
    private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();
    private final ReentrantLock refreshLock = new ReentrantLock();

    public NotificationAuthTokenClient(
            RestClient.Builder builder,
            ServiceAddressResolver addressResolver,
            NotificationServiceProperties properties) {
        this.restClient = builder.build();
        this.addressResolver = addressResolver;
        this.properties = properties;
    }

    public String accessToken() {
        CachedToken current = cachedToken.get();
        if (current != null && current.expiresAt().isAfter(Instant.now().plusSeconds(30))) {
            return current.value();
        }
        refreshLock.lock();
        try {
            current = cachedToken.get();
            if (current != null && current.expiresAt().isAfter(Instant.now().plusSeconds(30))) {
                return current.value();
            }
            NotificationServiceProperties.UserService userService = properties.getUserService();
            if (!StringUtils.hasText(userService.getClientSecret())) {
                throw new IllegalStateException("notification-service client secret is not configured");
            }
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("scope", userService.getScope());
            TokenResponse response = restClient
                    .post()
                    .uri(addressResolver.resolve(userService.getTokenUrl()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .headers(headers -> headers.setBasicAuth(userService.getClientId(), userService.getClientSecret()))
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            if (response == null || !StringUtils.hasText(response.accessToken())) {
                throw new IllegalStateException("Auth token response was empty");
            }
            Instant expiresAt = Instant.now().plusSeconds(response.expiresIn() > 0 ? response.expiresIn() : 300);
            cachedToken.set(new CachedToken(response.accessToken(), expiresAt));
            return response.accessToken();
        } finally {
            refreshLock.unlock();
        }
    }

    private record CachedToken(String value, Instant expiresAt) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenResponse(
            @JsonProperty("access_token") String accessToken, @JsonProperty("expires_in") long expiresIn) {}
}
