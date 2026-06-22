package com.banquito.switchpagos.sftp.service;

import com.banquito.switchpagos.sftp.config.CoreAuthenticationProperties;
import com.banquito.switchpagos.sftp.config.SftpDemoProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreR9iPasswordAuthenticatorTest {

    private static final String CUSTOMER_UUID = "3f26a20e-c149-5666-84b9-7c8ce0ed2712";

    private HttpServer server;
    private final AtomicReference<String> loginResponse = new AtomicReference<>();
    private final AtomicInteger loginStatus = new AtomicInteger(200);
    private final AtomicInteger logoutCalls = new AtomicInteger();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/auth/login", this::handleLogin);
        server.createContext("/api/v1/auth/logout", this::handleLogout);
        server.start();
        loginResponse.set(successResponse("CLIENTE", List.of("CLIENTE_EMPRESA"), CUSTOMER_UUID));
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void acceptsConfiguredEnterpriseUserAgainstCoreR9i() {
        CoreR9iPasswordAuthenticator authenticator = authenticator();

        assertTrue(authenticator.authenticate("empresa.sierraazul", "Password123!", null));
        assertTrue(logoutCalls.get() > 0);
    }

    @Test
    void rejectsCredentialsRejectedByCoreR9i() {
        loginStatus.set(401);
        loginResponse.set("{\"code\":\"INVALID_CREDENTIALS\"}");

        assertFalse(authenticator().authenticate("empresa.sierraazul", "incorrecta", null));
    }

    @Test
    void rejectsAuthenticatedUserWithoutEnterpriseRole() {
        loginResponse.set(successResponse("CLIENTE", List.of("CLIENTE_PERSONA"), CUSTOMER_UUID));

        assertFalse(authenticator().authenticate("empresa.sierraazul", "Password123!", null));
        assertTrue(logoutCalls.get() > 0);
    }

    @Test
    void rejectsAuthenticatedUserAssociatedWithAnotherCustomer() {
        loginResponse.set(successResponse(
                "CLIENTE",
                List.of("CLIENTE_EMPRESA_PAGOS_MASIVOS"),
                "00000000-0000-0000-0000-000000000001"));

        assertFalse(authenticator().authenticate("empresa.sierraazul", "Password123!", null));
        assertTrue(logoutCalls.get() > 0);
    }

    @Test
    void rejectsUsernameNotConfiguredForSftpWithoutCallingCore() {
        assertFalse(authenticator().authenticate("cliente.maria", "Password123!", null));
    }

    private CoreR9iPasswordAuthenticator authenticator() {
        SftpDemoProperties sftpProperties = new SftpDemoProperties();
        sftpProperties.getDemoUser().setUsername("empresa.sierraazul");
        sftpProperties.getDemoUser().setCompanyRuc("1792103456001");
        sftpProperties.getDemoUser().setCustomerUuid(CUSTOMER_UUID);

        CoreAuthenticationProperties authProperties = new CoreAuthenticationProperties();
        authProperties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        authProperties.setAllowedRoles(List.of("CLIENTE_EMPRESA_PAGOS_MASIVOS", "CLIENTE_EMPRESA"));
        return new CoreR9iPasswordAuthenticator(sftpProperties, authProperties);
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        exchange.getRequestBody().readAllBytes();
        respond(exchange, loginStatus.get(), loginResponse.get());
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        exchange.getRequestBody().readAllBytes();
        logoutCalls.incrementAndGet();
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private String successResponse(String actorType, List<String> roles, String customerUuid) {
        String roleJson = roles.stream()
                .map(role -> "\"" + role + "\"")
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        return """
                {
                  "accessToken": "test-access-token",
                  "refreshToken": "test-refresh-token",
                  "sessionUuid": "test-session",
                  "actorType": "%s",
                  "roles": [%s],
                  "customerUuid": "%s"
                }
                """.formatted(actorType, roleJson, customerUuid);
    }
}
