package com.banquito.switchpagos.sftp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "sftp")
public class SftpDemoProperties {

    private boolean enabled = true;
    private int port = 2222;
    private Path baseDirectory = Path.of("/app/sftp");
    private Path uploadDirectory = Path.of("/app/sftp/upload");
    private Path processedDirectory = Path.of("/app/sftp/processed");
    private Path rejectedDirectory = Path.of("/app/sftp/rejected");
    private long scanDelayMs = 2000L;
    private long fileStableMs = 1500L;
    private DemoUser demoUser = new DemoUser();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Path getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(Path baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public Path getUploadDirectory() {
        return uploadDirectory;
    }

    public void setUploadDirectory(Path uploadDirectory) {
        this.uploadDirectory = uploadDirectory;
    }

    public Path getProcessedDirectory() {
        return processedDirectory;
    }

    public void setProcessedDirectory(Path processedDirectory) {
        this.processedDirectory = processedDirectory;
    }

    public Path getRejectedDirectory() {
        return rejectedDirectory;
    }

    public void setRejectedDirectory(Path rejectedDirectory) {
        this.rejectedDirectory = rejectedDirectory;
    }

    public long getScanDelayMs() {
        return scanDelayMs;
    }

    public void setScanDelayMs(long scanDelayMs) {
        this.scanDelayMs = scanDelayMs;
    }

    public long getFileStableMs() {
        return fileStableMs;
    }

    public void setFileStableMs(long fileStableMs) {
        this.fileStableMs = fileStableMs;
    }

    public DemoUser getDemoUser() {
        return demoUser;
    }

    public void setDemoUser(DemoUser demoUser) {
        this.demoUser = demoUser;
    }

    public static class DemoUser {
        private String username = "empresa.sierraazul";
        private String companyRuc = "1792103456001";
        private String customerUuid = "3f26a20e-c149-5666-84b9-7c8ce0ed2712";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getCompanyRuc() {
            return companyRuc;
        }

        public void setCompanyRuc(String companyRuc) {
            this.companyRuc = companyRuc;
        }

        public String getCustomerUuid() {
            return customerUuid;
        }

        public void setCustomerUuid(String customerUuid) {
            this.customerUuid = customerUuid;
        }
    }
}
