package com.banquito.switchpagos.sftp.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreR9iPasswordAuthenticatorTest {

    @Test
    void acceptsAnyUserWithAnyNonEmptyPassword() {
        assertTrue(authenticator().authenticate("empresa.sierraazul", "Password123!", null));
        assertTrue(authenticator().authenticate("cliente.maria", "Password123!", null));
        assertTrue(authenticator().authenticate("usuario.libre", "cualquier-clave", null));
    }

    @Test
    void rejectsBlankPassword() {
        assertFalse(authenticator().authenticate("empresa.sierraazul", "", null));
        assertFalse(authenticator().authenticate("empresa.sierraazul", " ", null));
    }

    @Test
    void rejectsBlankUsername() {
        assertFalse(authenticator().authenticate("", "Password123!", null));
        assertFalse(authenticator().authenticate(" ", "Password123!", null));
    }

    private CoreR9iPasswordAuthenticator authenticator() {
        return new CoreR9iPasswordAuthenticator();
    }
}
