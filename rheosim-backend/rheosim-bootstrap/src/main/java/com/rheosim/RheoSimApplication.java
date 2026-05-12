package com.rheosim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class RheoSimApplication {

    public static void main(String[] args) {
        SpringApplication.run(RheoSimApplication.class, args);
    }
}
