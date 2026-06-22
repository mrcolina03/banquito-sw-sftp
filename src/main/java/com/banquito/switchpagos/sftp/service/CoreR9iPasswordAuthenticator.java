package com.banquito.switchpagos.sftp.service;

import com.banquito.switchpagos.sftp.config.SftpDemoProperties;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CoreR9iPasswordAuthenticator implements PasswordAuthenticator {

    private static final Logger LOG = LoggerFactory.getLogger(CoreR9iPasswordAuthenticator.class);

    private final SftpDemoProperties sftpProperties;

    public CoreR9iPasswordAuthenticator(SftpDemoProperties sftpProperties) {
        this.sftpProperties = sftpProperties;
    }

    @Override
    public boolean authenticate(String username, String password, ServerSession session) {
        boolean accepted = StringUtils.hasText(username)
                && username.equals(sftpProperties.getDemoUser().getUsername())
                && StringUtils.hasText(password);
        if (accepted) {
            LOG.warn("SFTP demo authentication bypass enabled. Accepted configured user {} without Core validation.",
                    username);
        }
        return accepted;
    }
}
