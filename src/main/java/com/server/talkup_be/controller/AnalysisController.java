package com.server.talkup_be.controller;

import com.server.talkup_be.dto.AnalysisDto;
import com.server.talkup_be.repo.EmitterRepository;
import com.server.talkup_be.service.AnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/analysis")
public class AnalysisController {
    private final AnalysisService analysisService;
    private final EmitterRepository emitterRepository;

    public AnalysisController(AnalysisService analysisService, EmitterRepository emitterRepository) {
        this.analysisService = analysisService;
        this.emitterRepository = emitterRepository;
    }

    @Value("${ai.internal.secret}")
    private String internalSecret;

    @Value("${ai.server.url}") // AI 서버 주소 (application.yml에 추가 필요)
    private String aiServerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // 연습기록 간이(미리보기) 조회
    @GetMapping("/cardNews")
    public ResponseEntity<?> getAnalysisCardNews(
            @AuthenticationPrincipal String userIdStr,
            @RequestParam(required = false) UUID folderId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer page,
            @RequestParam(defaultValue = "0") Integer how,
            @RequestParam(required = false) String keyWord) {
        try {
            // 1. 토큰 추출
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
            // 1. 토큰 추출
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
            // 1. 토큰 추출
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
            // 1. 토큰 추출
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

    // 프론트 SSE 연결 및 AI 서버 분석 요청
    @GetMapping(value = "/analysis/alarm", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectAlarm(
            @AuthenticationPrincipal String userIdStr,
            @RequestParam String fileKey,
            @RequestParam String fileName, // TODO: 프론트 답변 보고 삭제하거나 남기기
            @RequestParam UUID folderId,
            @RequestParam int type,
            @RequestParam String title) {

        // 1. 토큰 추출
        UUID userId = UUID.fromString(userIdStr);

        // 2. Analysis 대기 상태 생성
        UUID analysisId = analysisService.createPendingAnalysis(userId, folderId, title, fileKey, type);

        // 3. SseEmitter 생성 (타임아웃 60분) 및 저장
        Long timeout = 60L * 1000 * 60;
        SseEmitter emitter = new SseEmitter(timeout);
        emitterRepository.save(fileKey, emitter);

        // 4. 503 에러 방지용 더미 데이터 발송
        try {
            emitter.send(SseEmitter.event().name("connect").data("SSE Connected successfully"));
        } catch (IOException e) {
            emitterRepository.delete(fileKey);
            log.error("SSE 연결 오류: ", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "알림 연결 실패");
        }

        // 5. AI 서버로 분석 시작 요청
        triggerAiAnalysis(analysisId, fileKey, type);

        return emitter;
    }

    // --- 내부 통신용 유틸 메서드 ---
    private void triggerAiAnalysis(UUID analysisId, String fileKey, int type) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Secret", internalSecret);

        Map<String, Object> body = Map.of(
                "analysisId", analysisId,
                "fileKey", fileKey,
                "type", type
        );

        try {
            restTemplate.postForEntity(
                    aiServerUrl + "/analysis/start",
                    new HttpEntity<>(body, headers),
                    String.class
            );
            log.info("AI 서버 분석 요청 성공 - analysisId: {}, fileKey: {}", analysisId, fileKey);
        } catch (Exception e) {
            log.error("AI 서버 분석 요청 실패", e);
            // TODO: 실패 시 대기 상태의 Analysis 삭제 및 S3 파일 삭제 등의 보상 트랜잭션 필요
        }
    }
}
