package com.server.talkup_be.controller;

import com.server.talkup_be.config.JwtProvider;
import com.server.talkup_be.dto.UserDto;
import com.server.talkup_be.entity.User;
import com.server.talkup_be.service.RedisBlacklistService;
import com.server.talkup_be.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.UUID;

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
    public ResponseEntity<?> signUp(
            @Valid @RequestBody UserDto.UserInput userInput, // @Valid - DTO 검사를 시작
            BindingResult bindingResult // 검사 결과(에러)
    ) {
        // 1. DTO 규칙 위반 시
        if (bindingResult.hasErrors()) {
            // 첫 번째 에러 메시지
            String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            // 400 Bad Request와 함께 프론트엔드에게 실패 이유
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
        }

        try {
            //log는 추후 삭제 요망
            log.info("회원가입 요청 데이터 확인: " + userInput.toString());
            // 2. user save 호출
            userService.save(userInput);
            return ResponseEntity.ok("회원가입 성공");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
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
        String token = jwtProvider.generateToken(user.getId().toString(), loginId, user.getName());

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

    // 회원 정보 수정
    @PatchMapping("")
    public ResponseEntity<String> updateUser(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UserDto.UserUpdate updateDto) {
        try {
            // 1. 토큰에서 내 ID get
            String token = authHeader.replace("Bearer ", "");
            String userIdStr = jwtProvider.validateAndGetUserId(token);
            UUID userId = UUID.fromString(userIdStr);

            // 2. Service 호출
            userService.updateUser(userId, updateDto);

            // 3. 200 성공 반환
            return ResponseEntity.ok("수정 성공 및 완료");

        } catch (IllegalStateException e) {
            // past와 new가 같이 오지 않은 경우
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            // 기존 비번이 틀린 경우
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (Exception e) {
            // 토큰이 이상하거나 기타 에러
            return ResponseEntity.status(401).body("로그인 필요 또는 유효하지 않은 토큰");
        }
    }
}