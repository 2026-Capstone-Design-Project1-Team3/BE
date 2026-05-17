package com.server.talkup_be.controller;

import com.server.talkup_be.config.JwtProvider;
import com.server.talkup_be.dto.FolderDto;
import com.server.talkup_be.dto.UserDto;
import com.server.talkup_be.service.FolderService;
import com.server.talkup_be.service.RedisBlacklistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
public class FolderController {
    private final JwtProvider jwtProvider;
    private final RedisBlacklistService redisBlacklistService;
    private final FolderService folderService;

    public FolderController(JwtProvider jwtProvider, RedisBlacklistService redisBlacklistService, FolderService folderService) {
        this.jwtProvider = jwtProvider;
        this.redisBlacklistService = redisBlacklistService;
        this.folderService = folderService;
    }

    // 시선 보정값 설정
    @PostMapping("/folder")
    public ResponseEntity<?> setFolder(
            @AuthenticationPrincipal String userIdStr,
            @RequestBody FolderDto.FolderInput folderInput) {
        try {
            // 1. 토큰 추출 (프론트가 준 토큰)
            UUID userId = UUID.fromString(userIdStr);

            // 2. Service 호출
            folderService.saveFolderData(userId, folderInput);

            return ResponseEntity.ok().body("폴더 생성이 성공적으로 설정되었습니다.");

        } catch (Exception e) {
            // 기타
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("폴더 생성 중 오류가 발생했습니다.");
        }
    }
}
