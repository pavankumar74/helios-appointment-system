package com.hellodoctor.helios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class HeliosApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeliosApplication.class, args);
    }
}
