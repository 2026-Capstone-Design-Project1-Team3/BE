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
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    // 폴더 생성
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

    // 폴더 미리보기 개수 조회
    @GetMapping("/folder/total")
    public ResponseEntity<?> getFolderTotal(
            @AuthenticationPrincipal String userIdStr,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String keyWord) {
        try {
            // 1. 토큰 추출 (프론트가 준 토큰)
            UUID userId = UUID.fromString(userIdStr);

            // 2. Service 호출
            FolderDto.FolderPageCount result= folderService.getFolderTotal(userId,type, limit, keyWord);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            // 기타
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("폴더 조회 중 오류가 발생했습니다.");
        }
    }

    // 폴더 미리보기 조회
    @GetMapping("/folder")
    public ResponseEntity<?> getFolder(
            @AuthenticationPrincipal String userIdStr,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer page,
            @RequestParam(defaultValue = "0") Integer how,
            @RequestParam(required = false) String keyWord) {
        try {
            // 1. 토큰 추출 (프론트가 준 토큰)
            UUID userId = UUID.fromString(userIdStr);

            // 2. Service 호출
            List<FolderDto.FolderInfo> result= folderService.getFolderData(userId,type, limit, page, how, keyWord);
            return ResponseEntity.ok().body(result);

        } catch (Exception e) {
            // 기타
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("폴더 조회 중 오류가 발생했습니다.");
        }
    }
}
