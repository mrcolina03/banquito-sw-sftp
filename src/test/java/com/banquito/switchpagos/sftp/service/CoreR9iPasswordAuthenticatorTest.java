package com.banquito.switchpagos.sftp.service;

import com.banquito.switchpagos.sftp.config.SftpDemoProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreR9iPasswordAuthenticatorTest {

    @Test
    void acceptsConfiguredUserWithAnyNonEmptyPassword() {
        assertTrue(authenticator().authenticate("empresa.sierraazul", "Password123!", null));
        assertTrue(authenticator().authenticate("empresa.sierraazul", "cualquier-clave", null));
    }

    @Test
    void rejectsUsernameNotConfiguredForSftp() {
        assertFalse(authenticator().authenticate("cliente.maria", "Password123!", null));
    }

    @Test
    void rejectsBlankPassword() {
        assertFalse(authenticator().authenticate("empresa.sierraazul", "", null));
        assertFalse(authenticator().authenticate("empresa.sierraazul", " ", null));
    }

    private CoreR9iPasswordAuthenticator authenticator() {
        SftpDemoProperties sftpProperties = new SftpDemoProperties();
        sftpProperties.getDemoUser().setUsername("empresa.sierraazul");
        sftpProperties.getDemoUser().setCompanyRuc("1792103456001");
        sftpProperties.getDemoUser().setCustomerUuid("3f26a20e-c149-5666-84b9-7c8ce0ed2712");
        return new CoreR9iPasswordAuthenticator(sftpProperties);
    }
}
