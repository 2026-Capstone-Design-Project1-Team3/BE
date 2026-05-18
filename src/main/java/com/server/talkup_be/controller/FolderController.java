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
@RequestMapping("/folder")
public class FolderController {
    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    // 폴더 생성
    @PostMapping("")
    public ResponseEntity<?> setFolder(
            @AuthenticationPrincipal String userIdStr,
            @RequestBody FolderDto.FolderInput folderInput) {
        try {
            // 1. 토큰 추출 (프론트가 준 토큰)
            UUID userId = UUID.fromString(userIdStr);

            // 2. Service 호출
            folderService.saveFolderData(userId, folderInput);

            return ResponseEntity.ok().body("폴더 생성이 성공적으로 설정되었습니다.");

        }  catch (IllegalArgumentException | IllegalStateException e) {
            // 프론트가 값을 잘못 보냈거나 권한이 없을 때 (400 or 403)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // 서버나 DB가 터졌을 때 (500)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류가 발생했습니다.");
        }
    }

    // 폴더 미리보기 개수 조회
    @GetMapping("/total")
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

        }  catch (IllegalArgumentException | IllegalStateException e) {
            // 프론트가 값을 잘못 보냈거나 권한이 없을 때 (400 or 403)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // 서버나 DB가 터졌을 때 (500)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류가 발생했습니다.");
        }
    }

    // 폴더 미리보기 조회
    @GetMapping("")
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

        } catch (IllegalArgumentException | IllegalStateException e) {
            // 프론트가 값을 잘못 보냈거나 권한이 없을 때 (400 or 403)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // 서버나 DB가 터졌을 때 (500)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류가 발생했습니다.");
        }
    }

    // 폴더 삭제
    @PostMapping("/delete")
    public ResponseEntity<?> setFolder(
            @AuthenticationPrincipal String userIdStr,
            @RequestBody FolderDto.FolderDeleteList folderIds) {
        try {
            // 1. 토큰 추출 (프론트가 준 토큰)
            UUID userId = UUID.fromString(userIdStr);

            // 2. Service 호출
            folderService.deleteFolder(userId, folderIds.getFolderId());

            return ResponseEntity.ok().body("폴더 삭제가 성공적으로 설정되었습니다.");
        } catch (IllegalStateException e){
            // 권한 없는 폴더 삭제 요청(403)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            // 프론트가 값을 잘못 보냈거나 권한이 없을 때 (400 or 403)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
