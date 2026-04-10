package com.server.talkup_be.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class RedisBlacklistService {

    private final StringRedisTemplate redisTemplate;

    public RedisBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 1. 토큰을 Redis에 저장 (남은 시간만큼만 보관하고 자동 삭제됨!)
    public void setBlacklist(String token, long remainingTime) {
        // Key: 토큰, Value: "logout" (값은 아무거나 상관없음)
        redisTemplate.opsForValue().set(token, "logout", remainingTime, TimeUnit.MILLISECONDS);
    }

    // 2. 이 토큰이 블랙리스트에 있는지 확인
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(token));
    }
}