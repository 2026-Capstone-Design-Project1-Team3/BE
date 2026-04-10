package com.server.talkup_be.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
        @NotBlank(message = "아이디를 입력해주세요.")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*\\d)[a-z\\d]{8,16}$",
                message = "아이디는 영소문자와 숫자를 포함하여 8자~16자로 입력해주세요.")
        private String loginId;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,16}$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함하여 8자~16자로 입력해주세요.")
        private String passWord;

        @NotBlank(message = "이름을 입력해주세요.")
        @Pattern(regexp = "^[가-힣]{2,10}$",
                message = "이름은 한글 2자~10자로 입력해주세요.")
        private String name;

        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "올바른 이메일 형식이 아닙니다.") // @와 . 포함 확인
        @Pattern(regexp = "^\\S+$", message = "이메일에 공백이 포함될 수 없습니다.") // 공백 x 확인
        private String email;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class UserUpdate {
        private String pastPassWord; // null 허용
        private String newPassWord;  // null 허용
        private String name;
        private String email;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public class UserInfo {
        private String loginId;
        private String name;
        private String email;
    }
}
