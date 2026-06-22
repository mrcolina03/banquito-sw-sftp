package com.banquito.switchpagos.sftp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "sftp.core-auth")
public class CoreAuthenticationProperties {

    private String baseUrl = "http://banquito-kong-gateway:8000";
    private String loginPath = "/api/v1/auth/login";
    private String logoutPath = "/api/v1/auth/logout";
    private long connectTimeoutMs = 5000L;
    private long readTimeoutMs = 10000L;
    private List<String> allowedRoles = new ArrayList<>(
            List.of("CLIENTE_EMPRESA_PAGOS_MASIVOS", "CLIENTE_EMPRESA"));

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getLoginPath() {
        return loginPath;
    }

    public void setLoginPath(String loginPath) {
        this.loginPath = loginPath;
    }

    public String getLogoutPath() {
        return logoutPath;
    }

    public void setLogoutPath(String logoutPath) {
        this.logoutPath = logoutPath;
    }

    public long getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(long connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public long getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(long readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public List<String> getAllowedRoles() {
        return allowedRoles;
    }

    public void setAllowedRoles(List<String> allowedRoles) {
        this.allowedRoles = allowedRoles == null ? new ArrayList<>() : new ArrayList<>(allowedRoles);
    }
}
