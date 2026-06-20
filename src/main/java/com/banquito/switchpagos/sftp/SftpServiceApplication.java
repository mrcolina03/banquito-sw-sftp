package com.banquito.switchpagos.sftp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@ConfigurationPropertiesScan
public class SftpServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SftpServiceApplication.class, args);
    }
}
