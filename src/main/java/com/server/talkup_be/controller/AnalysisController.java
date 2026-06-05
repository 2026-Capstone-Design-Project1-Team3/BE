package com.server.talkup_be.controller;

import com.server.talkup_be.dto.AnalysisDto;
import com.server.talkup_be.entity.EyeCalibration;
import com.server.talkup_be.repo.EmitterRepo;
import com.server.talkup_be.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/analysis")
public class AnalysisController {
    private final AnalysisService analysisService;
    private final EmitterRepo emitterRepo;

    @Value("${app.api.internal-secret}")
    private String internalSecret;

    @Value("${ai.server.url}")
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
    @GetMapping(value = "/alarm", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectAlarm(
            @AuthenticationPrincipal String userIdStr,
            @RequestParam String fileKey,
            @RequestParam UUID folderId,
            @RequestParam int type,
            @RequestParam String title) {

        // 1. 토큰 추출
        UUID userId = UUID.fromString(userIdStr);

        // 2. Analysis 대기 상태 생성
        AnalysisDto.PendingAnalysisResult result = analysisService.createPendingAnalysis(userId, folderId, title, fileKey, type);

        // 3. SseEmitter 생성 (타임아웃 60분) 및 저장
        Long timeout = 60L * 1000 * 60;
        SseEmitter emitter = new SseEmitter(timeout);
        // TODO: repo를 service단으로 내리기
        emitterRepo.save(fileKey, emitter);

        // 4. 503 에러 방지용 더미 데이터 발송
        try {
            emitter.send(SseEmitter.event().name("connect").data("SSE Connected successfully"));
        } catch (IOException e) {
            emitterRepo.delete(fileKey);
            log.error("SSE 연결 오류: ", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "알림 연결 실패");
        }

        // 15초마다 Heartbeat를 보내는 스케줄러 (CloudFront 연결 끊김 방지용)
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // 무시해도 되는 ping 데이터
                emitter.send(SseEmitter.event().name("ping").data("heartbeat"));
            } catch (Exception e) {
                scheduler.shutdown(); // 연결이 닫혔거나 에러가 나면 스케줄러 종료
            }
        }, 15, 15, TimeUnit.SECONDS);

        // Emitter가 완료되거나 타임아웃될 때 스케줄러도 함께 종료되도록 콜백 등록
        emitter.onCompletion(scheduler::shutdown);
        emitter.onTimeout(scheduler::shutdown);
        emitter.onError(e -> scheduler.shutdown());

        // 5. AI 서버로 분석 시작 요청(비동기 스레드)
        CompletableFuture.runAsync(() -> {
            triggerAiAnalysis(result.getAnalysisId(), fileKey, type, result.getExtraInfo(), result.getEyeCalibration());
        });

        return emitter;
    }

    // --- 내부 통신용 유틸 메서드 ---
    private void triggerAiAnalysis(UUID analysisId, String fileKey, int type, String extraInfo, EyeCalibration eyeCalibration) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Secret", internalSecret);

        Map<String, Object> body = Map.of(
                "analysisId", analysisId,
                "fileKey", fileKey,
                "type", type,
                "extraInfo", extraInfo,
                "eyeCalibration", eyeCalibration
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
            // 1. 대기 상태 Analysis 및 fileKey 영상 지우기
            analysisService.rollbackPendingAnalysis(analysisId, fileKey);
        }
    }

    // ai 분석 완료 후 analysis 생성
    @PostMapping("")
    public ResponseEntity<String> receiveAiResult(
            @RequestHeader("X-Internal-Secret") String secretHeader,
            @RequestBody AnalysisDto.ResultInput resultInput) {
        try{
        // 1. 비밀키 검증
        if (!internalSecret.equals(secretHeader)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "허가되지 않은 접근입니다.");
        }

        log.info("AI 서버 분석 완료 수신 - analysisId: {}", resultInput.getAnalysisId());

        // 2. Service 호출
        analysisService.processAndSaveResultAsync(resultInput);

        return ResponseEntity.ok("Success");
    } catch (IllegalArgumentException | IllegalStateException e) {
        // AI가 값을 잘못 보냈거나 권한이 없을 때 (400 or 403)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    } catch (Exception e) {
        // 서버나 DB가 터졌을 때 (500)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류가 발생했습니다.");
    }
    }
}
