package com.server.talkup_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TalkUpBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(TalkUpBeApplication.class, args);
    }

}
