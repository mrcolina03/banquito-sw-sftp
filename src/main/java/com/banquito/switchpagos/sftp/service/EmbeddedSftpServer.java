package com.banquito.switchpagos.sftp.service;

import com.banquito.switchpagos.sftp.config.SftpDemoProperties;
import jakarta.annotation.PreDestroy;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class EmbeddedSftpServer {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedSftpServer.class);

    private final SftpDemoProperties properties;
    private final CoreR9iPasswordAuthenticator passwordAuthenticator;
    private SshServer sshServer;

    public EmbeddedSftpServer(
            SftpDemoProperties properties,
            CoreR9iPasswordAuthenticator passwordAuthenticator) {
        this.properties = properties;
        this.passwordAuthenticator = passwordAuthenticator;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() throws IOException {
        if (!properties.isEnabled()) {
            LOG.info("Embedded SFTP disabled by configuration");
            return;
        }

        createDirectories();

        sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(properties.getPort());
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(
                properties.getBaseDirectory().resolve("hostkey.ser")));
        sshServer.setPasswordAuthenticator(passwordAuthenticator);
        sshServer.setFileSystemFactory(new VirtualFileSystemFactory(properties.getBaseDirectory()));
        sshServer.setSubsystemFactories(List.of(new SftpSubsystemFactory.Builder().build()));
        sshServer.start();

        LOG.info("Embedded SFTP started on port {} with base directory {}", properties.getPort(),
                properties.getBaseDirectory());
    }

    @PreDestroy
    public void stop() throws IOException {
        if (sshServer != null && sshServer.isOpen()) {
            sshServer.stop();
        }
    }

    private void createDirectories() throws IOException {
        Files.createDirectories(properties.getBaseDirectory());
        Files.createDirectories(properties.getUploadDirectory());
        Files.createDirectories(properties.getProcessedDirectory());
        Files.createDirectories(properties.getRejectedDirectory());
        Files.createDirectories(properties.getProcessedDirectory().resolve(properties.getDemoUser().getUsername()));
        Files.createDirectories(properties.getRejectedDirectory().resolve(properties.getDemoUser().getUsername()));
        Path readme = properties.getBaseDirectory().resolve("README.txt");
        if (!Files.exists(readme)) {
            Files.writeString(readme, """
                    Banco BanQuito Switch SFTP demo

                    Upload directory: /upload
                    Processed files: /processed
                    Rejected files: /rejected
                    """);
        }
    }
}
