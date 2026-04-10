package com.server.talkup_be.controller;

import com.server.talkup_be.config.JwtProvider;
import com.server.talkup_be.dto.UserDto;
import com.server.talkup_be.entity.User;
import com.server.talkup_be.service.RedisBlacklistService;
import com.server.talkup_be.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    private final JwtProvider jwtProvider;
    private final RedisBlacklistService redisBlacklistService;
    private final UserService userService;

    public UserController(JwtProvider jwtProvider, RedisBlacklistService redisBlacklistService, UserService userService) {
        this.jwtProvider = jwtProvider;
        this.redisBlacklistService = redisBlacklistService;
        this.userService = userService;
    }

    // 회원가입
    @PostMapping("/signUp")
    public ResponseEntity<?> signUp(@RequestBody UserDto.UserInput userInput) {
        try {
            // 테스트용(추후 지우기)
            log.info("회원가입 요청 데이터 확인: " + userInput.toString());
            // 회원가입 service 호출
            userService.save(userInput);
            return ResponseEntity.ok(200);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody UserDto.UserLogin userLogin) {
        try{
        String loginId = userLogin.getLoginId();
        String passWord = userLogin.getPassWord();

        // 1. DB에 존재하는지 검증
        User user = userService.validateUser(loginId,passWord);

        // 2. 인증 성공 시 토큰 발급 String userId, String loginId, String name
        String token = jwtProvider.generateToken(user.getId(), loginId, user.getName());

        // 3. 헤더에 토큰 담기
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);

        return ResponseEntity.ok()
                .headers(headers)
                .body("로그인 성공");
    } catch (IllegalArgumentException e) {
        // 4. Service에서 던진 에러(아이디 없음, 비번 맞지 않음) 처리
        return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");

        Claims claims = jwtProvider.validateAndGetClaims(token);
        // 시간 파악
        long expirationTime = claims.getExpiration().getTime();
        long now = new Date().getTime();
        // 토큰에 남은 시간
        long remainingTime = expirationTime - now;

        // 블랙리스트에 토큰 올려두고 관리하기
        if (remainingTime > 0) {
            redisBlacklistService.setBlacklist(token, remainingTime);
        }

        return ResponseEntity.ok("로그아웃 되었습니다.");
    }
}