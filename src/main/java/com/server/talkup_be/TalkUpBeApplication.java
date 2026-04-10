package com.server.talkup_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

// 임시 비밀번호 로그 끄기
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class TalkUpBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(TalkUpBeApplication.class, args);
    }

}
