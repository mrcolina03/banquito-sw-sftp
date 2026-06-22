package com.banquito.switchpagos.sftp.service;

import com.banquito.switchpagos.sftp.config.CoreAuthenticationProperties;
import com.banquito.switchpagos.sftp.config.SftpDemoProperties;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class CoreR9iPasswordAuthenticator implements PasswordAuthenticator {

    private static final Logger LOG = LoggerFactory.getLogger(CoreR9iPasswordAuthenticator.class);

    private final SftpDemoProperties sftpProperties;
    private final CoreAuthenticationProperties authProperties;
    private final RestClient restClient;

    public CoreR9iPasswordAuthenticator(
            SftpDemoProperties sftpProperties,
            CoreAuthenticationProperties authProperties) {
        this.sftpProperties = sftpProperties;
        this.authProperties = authProperties;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(authProperties.getConnectTimeoutMs()))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(authProperties.getReadTimeoutMs()));
        this.restClient = RestClient.builder()
                .baseUrl(authProperties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public boolean authenticate(String username, String password, ServerSession session) {
        if (!StringUtils.hasText(username)
                || !username.equals(sftpProperties.getDemoUser().getUsername())
                || !StringUtils.hasText(password)) {
            return false;
        }

        CoreLoginResponse response = null;
        try {
            response = restClient.post()
                    .uri(authProperties.getLoginPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Map.of("username", username, "password", password))
                    .retrieve()
                    .body(CoreLoginResponse.class);

            boolean accepted = isAcceptedEnterpriseUser(response);
            if (!accepted) {
                LOG.warn("Core R9I authenticated SFTP user {}, but role or customer association is not allowed",
                        username);
            }
            return accepted;
        } catch (RestClientResponseException exception) {
            LOG.warn("Core R9I rejected SFTP authentication for user {} with HTTP {}",
                    username, exception.getStatusCode().value());
            return false;
        } catch (RestClientException exception) {
            LOG.error("Core R9I authentication is unavailable for SFTP user {}", username);
            return false;
        } finally {
            closeCoreSession(response);
        }
    }

    private boolean isAcceptedEnterpriseUser(CoreLoginResponse response) {
        if (response == null || !StringUtils.hasText(response.accessToken())) {
            return false;
        }
        if (!"CLIENTE".equalsIgnoreCase(response.actorType())) {
            return false;
        }
        List<String> roles = response.roles() == null ? List.of() : response.roles();
        boolean allowedRole = authProperties.getAllowedRoles().stream().anyMatch(roles::contains);
        if (!allowedRole) {
            return false;
        }
        String expectedCustomerUuid = sftpProperties.getDemoUser().getCustomerUuid();
        return !StringUtils.hasText(expectedCustomerUuid)
                || expectedCustomerUuid.equalsIgnoreCase(response.customerUuid());
    }

    private void closeCoreSession(CoreLoginResponse response) {
        if (response == null || !StringUtils.hasText(response.sessionUuid())) {
            return;
        }
        try {
            restClient.post()
                    .uri(authProperties.getLogoutPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(response.accessToken()))
                    .body(new CoreLogoutRequest(
                            response.accessToken(),
                            response.refreshToken(),
                            response.sessionUuid()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            LOG.warn("Core R9I session cleanup failed after SFTP credential validation");
        }
    }

    record CoreLoginResponse(
            String accessToken,
            String refreshToken,
            String sessionUuid,
            String actorType,
            List<String> roles,
            String customerUuid) {
    }

    record CoreLogoutRequest(String accessToken, String refreshToken, String sessionUuid) {
    }
}
