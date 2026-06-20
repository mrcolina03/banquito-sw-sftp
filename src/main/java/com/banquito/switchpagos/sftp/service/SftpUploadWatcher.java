package com.banquito.switchpagos.sftp.service;

import com.banquito.switchpagos.sftp.config.SftpDemoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class SftpUploadWatcher {

    private static final Logger LOG = LoggerFactory.getLogger(SftpUploadWatcher.class);

    private final SftpDemoProperties properties;
    private final SftpFileProcessor processor;
    private final Map<Path, FileSnapshot> snapshots = new HashMap<>();

    public SftpUploadWatcher(SftpDemoProperties properties, SftpFileProcessor processor) {
        this.properties = properties;
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "${sftp.scan-delay-ms:2000}")
    public void scan() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            Files.createDirectories(properties.getUploadDirectory());
            try (Stream<Path> stream = Files.list(properties.getUploadDirectory())) {
                stream.filter(Files::isRegularFile)
                        .filter(this::isProcessableName)
                        .forEach(this::processIfStable);
            }
            removeMissingSnapshots();
        } catch (IOException exception) {
            LOG.error("Could not scan SFTP upload directory", exception);
        }
    }

    private boolean isProcessableName(Path file) {
        String name = file.getFileName().toString();
        return !name.endsWith(".tmp") && !name.endsWith(".part") && !name.endsWith(".metadata.json")
                && !name.endsWith(".reason.txt");
    }

    private void processIfStable(Path file) {
        try {
            long size = Files.size(file);
            Instant modifiedAt = Files.getLastModifiedTime(file).toInstant();
            FileSnapshot previous = snapshots.get(file);
            Instant now = Instant.now();
            if (previous == null || previous.size != size || !previous.modifiedAt.equals(modifiedAt)) {
                snapshots.put(file, new FileSnapshot(size, modifiedAt, now));
                return;
            }
            if (now.toEpochMilli() - previous.seenStableAt.toEpochMilli() < properties.getFileStableMs()) {
                return;
            }
            snapshots.remove(file);
            processor.process(file);
        } catch (IOException exception) {
            LOG.warn("Could not inspect SFTP uploaded file {}", file, exception);
        }
    }

    private void removeMissingSnapshots() {
        Iterator<Path> iterator = snapshots.keySet().iterator();
        while (iterator.hasNext()) {
            Path file = iterator.next();
            if (!Files.exists(file)) {
                iterator.remove();
            }
        }
    }

    private record FileSnapshot(long size, Instant modifiedAt, Instant seenStableAt) {
    }
}
