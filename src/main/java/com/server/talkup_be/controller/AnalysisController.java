package com.server.talkup_be.controller;

import com.server.talkup_be.dto.AnalysisDto;
import com.server.talkup_be.dto.AnalysisDto;
import com.server.talkup_be.service.AnalysisService;
import com.server.talkup_be.service.AnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/analysis")
public class AnalysisController {
    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    // 연습기록 미리보기 개수 조회
    @GetMapping("/total")
    public ResponseEntity<?> getAnalysisTotal(
            @AuthenticationPrincipal String userIdStr,
            @RequestParam(required = false) String folderId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String keyWord) {
        try {
            // 1. 토큰 추출 (프론트가 준 토큰)
            UUID userId = UUID.fromString(userIdStr);

            // 2. Service 호출
            AnalysisDto.AnalysisPageCount result= analysisService.getAnalysisTotal(userId, folderId, type, limit, keyWord);
            return ResponseEntity.ok().body(result);

        }  catch (IllegalArgumentException | IllegalStateException e) {
            // 프론트가 값을 잘못 보냈거나 권한이 없을 때 (400 or 403)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // 서버나 DB가 터졌을 때 (500)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류가 발생했습니다.");
        }
    }

    // 연습기록 간이(미리보기) 조회
    @GetMapping("/cardNews")
    public ResponseEntity<?> getAnalysisCardNews(
            @AuthenticationPrincipal String userIdStr,
            @RequestParam(required = false) String folderId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer page,
            @RequestParam(defaultValue = "0") Integer how,
            @RequestParam(required = false) String keyWord) {
        try {
            // 1. 토큰 추출 (프론트가 준 토큰)
            UUID userId = UUID.fromString(userIdStr);

            // 2. Service 호출
            AnalysisDto.AnalysisCardnewsInfo result= analysisService.getAnalysisCardnewsData(userId, folderId, type, limit, page, how, keyWord);
            return ResponseEntity.ok().body(result);

        } catch (IllegalArgumentException | IllegalStateException e) {
            // 프론트가 값을 잘못 보냈거나 권한이 없을 때 (400 or 403)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // 서버나 DB가 터졌을 때 (500)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류가 발생했습니다.");
        }
    }

    // 연습기록 상세 조회
    @GetMapping("/{analysisId}")
    public ResponseEntity<?> getAnalysis(
            @AuthenticationPrincipal String userIdStr,
            @PathVariable UUID analysisId) {
        try {
            // 1. 토큰 추출 (프론트가 준 토큰)
            UUID userId = UUID.fromString(userIdStr);

            // 2. Service 호출
            AnalysisDto.AnalysisInfo result = analysisService.getAnalysisData(userId, analysisId);
            return ResponseEntity.ok().body(result);

        }catch (IllegalStateException e) {
            // 권한 없는 연습기록 조회 요청(403)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }catch (IllegalArgumentException e) {
            // 프론트가 값을 잘못 보냈을 때(400)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // 서버나 DB가 터졌을 때 (500)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류가 발생했습니다.");
        }
    }

    // 연습기록 최신 n개 피드백 수치들 조회
    @GetMapping("/statistics/{limit}")
    public ResponseEntity<?> getAnalysisN(
            @AuthenticationPrincipal String userIdStr,
            @PathVariable Integer limit) {
        try {
            // 1. 토큰 추출 (프론트가 준 토큰)
            UUID userId = UUID.fromString(userIdStr);

            // 2. Service 호출
            AnalysisDto.AnalysisStatistics result= analysisService.getAnalysisNData(userId, limit);
            return ResponseEntity.ok().body(result);

        } catch (IllegalArgumentException | IllegalStateException e) {
            // 프론트가 값을 잘못 보냈거나 권한이 없을 때 (400 or 403)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // 서버나 DB가 터졌을 때 (500)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류가 발생했습니다.");
        }
    }

    // 연습기록 삭제
    @PostMapping("/delete")
    public ResponseEntity<?> deleteAnalysis(
            @AuthenticationPrincipal String userIdStr,
            @RequestBody AnalysisDto.AnalysisDeleteList analysisIds) {
        try {
            // 1. 토큰 추출 (프론트가 준 토큰)
            UUID userId = UUID.fromString(userIdStr);

            // 2. Service 호출
            analysisService.deleteAnalysis(userId, analysisIds.getAnalysisId());

            return ResponseEntity.ok().body("연습기록 삭제가 성공적으로 설정되었습니다.");
        } catch (IllegalStateException e){
            // 권한 없는 폴더 삭제 요청(403)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            // 프론트가 값을 잘못 보냈거나 권한이 없을 때 (400 or 403)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
