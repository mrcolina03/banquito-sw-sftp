package com.banquito.switchpagos.sftp.service;

import com.banquito.switchpagos.sftp.client.BatchServiceClient;
import com.banquito.switchpagos.sftp.client.BatchUploadResult;
import com.banquito.switchpagos.sftp.config.SftpDemoProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class SftpFileProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SftpFileProcessor.class);

    private final SftpDemoProperties properties;
    private final HeaderRucParser headerRucParser;
    private final BatchServiceClient batchServiceClient;
    private final ObjectMapper objectMapper;

    public SftpFileProcessor(
            SftpDemoProperties properties,
            HeaderRucParser headerRucParser,
            BatchServiceClient batchServiceClient,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.headerRucParser = headerRucParser;
        this.batchServiceClient = batchServiceClient;
        this.objectMapper = objectMapper;
    }

    public void process(Path file) {
        String username = "sftp";
        try {
            String headerRuc = headerRucParser.readCompanyRuc(file);
            BatchUploadResult result = batchServiceClient.upload(
                    file,
                    headerRuc,
                    null,
                    username);
            if (result.accepted()) {
                moveToProcessed(file, username, headerRuc, result);
            } else {
                reject(file, username, result.errorCode(), result.message(), headerRuc, result);
            }
        } catch (Exception exception) {
            reject(file, username, "SFTP_PROCESSING_ERROR", safe(exception.getMessage()), null, null);
        }
    }

    private void moveToProcessed(Path file, String username, String companyRuc, BatchUploadResult result) throws IOException {
        Path userDirectory = properties.getProcessedDirectory().resolve(username);
        Files.createDirectories(userDirectory);
        Path target = uniqueTarget(userDirectory, file.getFileName().toString());
        Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);

        ObjectNode metadata = baseMetadata(target, username, companyRuc, "PROCESSED");
        metadata.put("batchId", result.batchId());
        metadata.put("batchStatus", result.status());
        metadata.put("httpStatus", result.httpStatus());
        metadata.put("message", result.message());
        writeMetadata(target, metadata);
        LOG.info("SFTP file processed. file={} batchId={}", target.getFileName(), result.batchId());
    }

    private void reject(
            Path file,
            String username,
            String reasonCode,
            String reasonMessage,
            String headerRuc,
            BatchUploadResult result) {
        try {
            Path userDirectory = properties.getRejectedDirectory().resolve(username);
            Files.createDirectories(userDirectory);
            Path target = uniqueTarget(userDirectory, file.getFileName().toString());
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);

            ObjectNode metadata = baseMetadata(target, username, headerRuc, "REJECTED");
            metadata.put("reasonCode", reasonCode);
            metadata.put("reasonMessage", reasonMessage);
            if (result != null) {
                metadata.put("httpStatus", result.httpStatus());
                metadata.put("batchServiceMessage", result.message());
            }
            writeMetadata(target, metadata);
            writeReason(target, reasonCode, reasonMessage);
            LOG.warn("SFTP file rejected. file={} reason={}", target.getFileName(), reasonCode);
        } catch (IOException moveException) {
            LOG.error("Could not move rejected SFTP file {}", file, moveException);
        }
    }

    private ObjectNode baseMetadata(Path target, String username, String companyRuc, String status) {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("fileName", target.getFileName().toString());
        metadata.put("username", username);
        metadata.put("companyRuc", companyRuc);
        metadata.put("status", status);
        metadata.put("channel", "SFTP");
        metadata.put("processedAt", OffsetDateTime.now().toString());
        return metadata;
    }

    private Path uniqueTarget(Path directory, String fileName) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(java.time.LocalDateTime.now());
        return directory.resolve(timestamp + "_" + fileName).normalize();
    }

    private void writeMetadata(Path target, ObjectNode metadata) throws IOException {
        Path metadataFile = target.resolveSibling(target.getFileName() + ".metadata.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataFile.toFile(), metadata);
    }

    private void writeReason(Path target, String code, String message) throws IOException {
        Path reasonFile = target.resolveSibling(target.getFileName() + ".reason.txt");
        Files.writeString(reasonFile, code + System.lineSeparator() + message + System.lineSeparator());
    }

    private String safe(String message) {
        return message == null || message.isBlank() ? "Error tecnico controlado" : message;
    }
}
