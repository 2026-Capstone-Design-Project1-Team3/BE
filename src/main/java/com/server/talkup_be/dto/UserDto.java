package com.server.talkup_be.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

public class UserDto {
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class UserLogin {
        private String loginId;
        private String passWord;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class UserInput {
        private String loginId;
        private String passWord;
        private String name;
        private String email;
    }
}
