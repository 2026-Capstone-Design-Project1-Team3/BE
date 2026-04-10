package com.server.talkup_be.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {

    private PrivateKey privateKey;
    private PublicKey publicKey;

    // 토큰 만료 시간 (1시간)
    private final long EXPIRATION_TIME = 1000L * 60 * 60;

    @PostConstruct
    public void init() throws Exception {
        // 서버 실행 시 PEM 파일을 읽어와 객체로 초기화합니다.
        // 실제 운영 환경에서는 파일 경로 관리에 주의
        this.privateKey = KeyReader.getPrivateKey("private_key.pem");
        this.publicKey = KeyReader.getPublicKey("public_key.pem");
    }

    // JWT 토큰 생성
    public String generateToken(String userId, String loginId, String name) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .setSubject(userId)  // [표준] "sub": "String" (user의 id)
                .claim("loginId", loginId)        // "loginId": "String"
                .claim("name", name)              // "name": "String"
                .setIssuedAt(now)                   // [표준] "iat": "Number" (발급 시간)
                .setExpiration(expiryDate)          // [표준] "exp": "Number" (만료 시간)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    // JWT 토큰 검증 및 데이터 추출 (API 요청 들어올 때)
    public String validateAndGetUserId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey) // Public Key로 검증
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject(); // 아까 넣었던 userId 반환
        } catch (Exception e) {
            throw new RuntimeException("유효하지 않은 토큰입니다.", e);
        }
    }

    // JWT 토큰 검증 및 Payload 전체 정보 추출
    public Claims validateAndGetClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(publicKey) // Public Key로 서명이 맞는지 검증
                    .build()
                    .parseClaimsJws(token)    // 토큰 해독
                    .getBody();               // 안의 데이터 전체 반환
        } catch (Exception e) {
            // 토큰이 만료되었거나, 위조되었거나, 형식이 잘못된 경우
            throw new RuntimeException("유효하지 않거나 만료된 JWT 토큰입니다.", e);
        }
    }
}