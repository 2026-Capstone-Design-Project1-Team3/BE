package com.server.talkup_be.service;

import com.server.talkup_be.config.JwtProvider;
import com.server.talkup_be.dto.UserDto;
import com.server.talkup_be.entity.User;
import com.server.talkup_be.repo.UserRepo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Getter
@Setter
public class UserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepo userRepo;

    public UserService(PasswordEncoder passwordEncoder, UserRepo userRepo) {
        this.passwordEncoder = passwordEncoder;
        this.userRepo = userRepo;
    }

    // user login 확인
    public User validateUser(String loginId, String passWord) {
        // 1. DB에서 유저 찾기
        User user = (User) userRepo.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("해당 아이디가 없습니다."));

        // 2. 비밀번호가 일치하는지 확인(암호화 된 비번과 비교)
        if (!passwordEncoder.matches(passWord, user.getPassWord())) {
            throw new IllegalArgumentException("비밀번호가 틀렸습니다.");
        }

        return user;
    }

    // user 저장
    public void save(UserDto.UserInput userInput) {
        // 1. 비밀번호 해싱
        String encodedPassword = passwordEncoder.encode(userInput.getPassWord());

        // 2. user 저장
        User newUser = User.builder()
                .loginId(userInput.getLoginId())
                .passWord(encodedPassword)
                .email(userInput.getEmail())
                .name(userInput.getName())
                .build();

        userRepo.save(newUser);
    }
}
