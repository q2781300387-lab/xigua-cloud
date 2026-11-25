package com.xigua.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class XiGuaClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiGuaClientApplication.class, args);
    }

}
