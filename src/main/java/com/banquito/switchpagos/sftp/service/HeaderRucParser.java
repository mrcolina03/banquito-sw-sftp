package com.banquito.switchpagos.sftp.service;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class HeaderRucParser {

    public String readCompanyRuc(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String[] columns = trimmed.split(",", -1);
                if (columns.length < 2 || !"H".equals(columns[0].trim())) {
                    throw new IllegalArgumentException("HEADER_NOT_FOUND");
                }
                String companyRuc = columns[1].trim();
                if (companyRuc.isBlank()) {
                    throw new IllegalArgumentException("HEADER_RUC_REQUIRED");
                }
                return companyRuc;
            }
        }
        throw new IllegalArgumentException("HEADER_NOT_FOUND");
    }
}
