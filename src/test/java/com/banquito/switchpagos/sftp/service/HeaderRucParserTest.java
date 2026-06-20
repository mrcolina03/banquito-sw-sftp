package com.banquito.switchpagos.sftp.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HeaderRucParserTest {

    private final HeaderRucParser parser = new HeaderRucParser();

    @TempDir
    Path tempDir;

    @Test
    void readsCompanyRucFromHeader() throws Exception {
        Path file = tempDir.resolve("batch.csv");
        Files.writeString(file, """
                H,1792103456001,NOM,2026-06-05T10:20:00-05:00,0010000010599,1,10.00
                D,1,1792103456001,Comercial Sierra Azul S.A.,0010000010600,10,10.00,Pago,pagos@sierraazul.com.ec
                T,HASH,1,10.00
                """);

        assertEquals("1792103456001", parser.readCompanyRuc(file));
    }

    @Test
    void rejectsFileWithoutHeader() throws Exception {
        Path file = tempDir.resolve("batch.csv");
        Files.writeString(file, "D,1,1792103456001,Cliente,0010000010600,10,10.00,Pago,pagos@sierraazul.com.ec");

        assertThrows(IllegalArgumentException.class, () -> parser.readCompanyRuc(file));
    }
}
