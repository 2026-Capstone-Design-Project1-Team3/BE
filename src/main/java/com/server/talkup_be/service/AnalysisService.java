package com.server.talkup_be.service;

import com.server.talkup_be.dto.AnalysisDto;
import com.server.talkup_be.entity.Analysis;
import com.server.talkup_be.entity.Folder;
import com.server.talkup_be.repo.AnalysisRepo;
import com.server.talkup_be.repo.EmitterRepo;
import com.server.talkup_be.repo.FolderRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.server.talkup_be.entity.AnalysisStatus.FAILED;
import static com.server.talkup_be.entity.AnalysisStatus.PENDING;

@Slf4j
@RequiredArgsConstructor
@Service
public class AnalysisService {
    private final AnalysisRepo analysisRepo;
    private final FolderRepo folderRepo;
    private final S3Service s3Service;
    private final EmitterRepo emitterRepo;
    private final OpenAiService openAiService;

    // 연습기록 간이(미리보기) 조회
    public AnalysisDto.AnalysisCardnewsInfo getAnalysisCardnewsData(UUID userId, UUID folderId, Integer type, Integer limit, Integer page, Integer how, String keyWord) {
        // 키워드 % 추가
        String processedKeyWord = null;
        if (keyWord != null && !keyWord.isEmpty()) {
            processedKeyWord = "%" + keyWord + "%";
        }

        int pageSize = (limit == null || limit <= 0) ? Integer.MAX_VALUE : limit;
        int pageNum = (page == null || page <= 0) ? 1 : page;

        // how == 0이면 최신순, how==1이면 날짜순
        Sort sort = (how != null && how == 1)
                ? Sort.by(Sort.Direction.ASC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");

        // Pageable 생성
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);

        List<AnalysisDto.AnalysisCardnewsInfo.AnalysisCardnews> entityList = analysisRepo.findAnalyses(userId, folderId, type, processedKeyWord, pageable);
        return AnalysisDto.AnalysisCardnewsInfo.builder()
                .total(analysisRepo.countFilteredAnalyses(userId, folderId, type, processedKeyWord))
                .cardnews(entityList)
                .build();
    }

    // 연습기록 상세 보기
    public AnalysisDto.AnalysisInfo getAnalysisData(UUID userId, UUID analysisId) {

        Analysis analysis = analysisRepo.findById(analysisId).orElseThrow(() -> new IllegalArgumentException("해당 연습 기록을 찾을 수 없습니다."));

        // 남의 연습기록 조회 요청시 차단
        if (!analysis.getUserId().equals(userId)) {
            throw new IllegalStateException("조회 권한이 없는 연습기록이 포함되어 있습니다.");
        }
        return AnalysisDto.AnalysisInfo.builder()
                .analysisId(analysisId)
                .folderId(analysis.getFolderId())
                .title(analysis.getTitle())
                .type(analysis.getType())
                .summary(analysis.getSummary())
                .createdAt(analysis.getCreatedAt())
                .gazeScore(analysis.getGazeScore())
                .gazeDistribution(analysis.getGazeDistribution())
                .fluencyLevel(analysis.getFluencyLevel())
                .fluencyFeedback(analysis.getFluencyFeedback())
                .speedScore(analysis.getSpeedScore())
                .speedDistribution(analysis.getSpeedDistribution())
                .gestureFeedbackWord(analysis.getGestureFeedbackWord())
                .gestureFeedbackSentence(analysis.getGestureFeedbackSentence())
                .finalScore(analysis.getFinalScore())
                .finalFeedback(analysis.getFinalFeedback())
                .transcript(analysis.getTranscript())
                .build();
    }

    // 연습기록 최신 N개 피드백 수치 조회
    public AnalysisDto.AnalysisStatistics getAnalysisNData(UUID userId, Integer limit) {
        // limit 파라미터 유무에 따라 Pageable 객체 생성
        Pageable pageable = (limit != null && limit > 0)
                ? PageRequest.of(0, limit)
                : Pageable.unpaged();

        Integer totalCount = analysisRepo.countByUserId(userId);
        List<AnalysisDto.AnalysisStatistics.StatisticData> statsList = analysisRepo.findStatistics(userId,pageable);

        return AnalysisDto.AnalysisStatistics.builder()
                .total(totalCount)
                .statistics(statsList)
                .build();
    }

    // 연습기록 삭제
    @Transactional
    public void deleteAnalysis(UUID userId, List<UUID> analysisIds) {
        // 빈 배열일 경우
        if (analysisIds == null || analysisIds.isEmpty()) {
            throw new IllegalArgumentException("삭제할 연습기록이 선택되지 않았습니다.");
        }

        // userId와 관련된 analysis 전부 호출
        List<Analysis> analyses = analysisRepo.findAllById(analysisIds);

        // user의 연습기록들을 하나씩 검사
        for (Analysis analysis : analyses) {
            // 남의 연습기록 삭제 요청시 차단
            if (!analysis.getUserId().equals(userId)) {
                throw new IllegalStateException("삭제 권한이 없는 연습기록이 포함되어 있습니다.");
            }
        }

        analysisRepo.deleteAll(analyses);
    }

    // 대기 상태 Analysis 생성
    @Transactional
    public UUID createPendingAnalysis(UUID userId, UUID folderId, String title, String fileKey, int type) {
        // 존재 유무 & 본인 analysis 유무
        Folder folder = folderRepo.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 폴더를 찾을 수 없습니다."));
        if (!folder.getUserId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없는 폴더입니다.");
        }
        // 2. pending상태의 Analysis 엔티티 생성
        Analysis pendingAnalysis = Analysis.builder()
                .userId(userId)
                .folderId(folderId)
                .fileKey(fileKey)
                .status(PENDING)
                .title(title)
                .type(type)
                .build();

        // 3. DB에 저장 후 analysisId 반환
        return analysisRepo.save(pendingAnalysis).getId();
    }

    // ai 서버 analysis 분석 요청 실패시 실행
    // pending 상태의 analysis 및 관련 fileKey 삭제
    @Transactional
    public void rollbackPendingAnalysis(UUID analysisId, String fileKey) {
        try {
            // TODO : 추후 업그레이드 한다면 실패 로직에 대해 어떻게 처리할지 고민해보기 (실패하면 영상 삭제 or 재시도)
            // 만약 삭제한다면
            // 1. 대기 상태(Pending) 지우기
            // analysisRepo.deleteById(analysisId);
            // 2. S3 원본 파일 삭제
            // s3Service.deleteFile(fileKey);

            // 1. 테스트 단계에선 남겨두기로 결정
            Analysis pendingAnalysis = analysisRepo.findById(analysisId)
                    .orElseThrow(() -> new IllegalArgumentException("대기 상태의 분석 기록이 없습니다."));

            pendingAnalysis.setStatus(FAILED);

            // 2. 프론트엔드에 실패 알림을 보내고 연결 끊기(추후 삭제로 로직 바껴도 이건 유지)
            SseEmitter emitter = emitterRepo.get(fileKey);
            if (emitter != null) {
                try {
                    // ANALYSIS_FAILED 이벤트 전송
                    emitter.send(SseEmitter.event()
                            .name("ANALYSIS_FAILED")
                            .data("AI 서버 요청에 실패했습니다. (서버 오류)"));

                    // close
                    emitter.complete();
                } catch (IOException e) {
                    log.error("롤백 중 프론트엔드 에러 알림 전송 실패: ", e);
                }
            }

            // 3. SseEmitter 연결 해제 및 삭제
            emitterRepo.delete(fileKey);

            log.info("롤백 완료");
        } catch (Exception e) {
            log.error("롤백 처리 중 추가 에러 발생 (수동 확인 필요): {}", e.getMessage(), e);
        }
    }

    // COMPLETED analysis 생성
    @Async
    @Transactional
    public void processAndSaveResultAsync(AnalysisDto.ResultInput resultInput) {
        String fileKey = resultInput.getFileKey();
        try {
            log.info("비동기 분석 데이터 가공 시작 - fileKey: {}", fileKey);

            // 1. 대기 상태 Analysis 조회
            Analysis analysis = analysisRepo.findById(resultInput.getAnalysisId())
                    .orElseThrow(() -> new RuntimeException("대기 중인 분석 기록이 없습니다."));

            // 2. Folder의 updatedAt 갱신
            UUID folderId = analysis.getFolderId();
            Folder folder = folderRepo.findById(folderId)
                    .orElseThrow(() -> new RuntimeException("해당하는 폴더가 없습니다."));
            folder.setUpdatedAt(LocalDateTime.now());
            folderRepo.save(folder);

            // 3. LLM 가공 (요약, 총평, 점수)
            String summary = openAiService.summarizeTranscript(resultInput.getTranscript());

            String finalFeedback = "";
            int finalScore = resultInput.getFinalScore();

            if (resultInput.getType() == 1) {
                // 면접 : 포트폴리오 기반
                String question = folder.getOutputText();
                String pdfFileKey = folder.getFileKey(); // 폴더에 저장된 PDF fileKey

                try {
                    String advancedResult = openAiService.generateAdvancedInterviewFeedback(pdfFileKey, question, resultInput);

                    // GPT 응답 파싱
                    String[] parts = advancedResult.split("<q>");
                    if (parts.length >= 6) {
                        // 숫자만 추출
                        finalScore = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
                        // <q>로 묶어서 finalFeedback 생성
                        finalFeedback = parts[1].trim() + "<q>" + parts[2].trim() + "<q>" + parts[3].trim() + "<q>" + parts[4].trim() + "<q>" + parts[5].trim();
                    } else {
                        throw new RuntimeException("GPT 응답 포맷 오류: " + advancedResult);
                    }

                } catch (Exception e) {
                    log.warn("고급 면접 분석 실패. 기본 분석으로 대체합니다.", e);
                    // S3에 파일이 없거나 에러가 나면, 자소서 기반 로직으로 대체
                    finalFeedback = openAiService.generateFinalFeedback(resultInput, question);
                    finalScore = openAiService.generateInterviewScore(resultInput.getTranscript(), question);
                }
            }

            // 4. Analysis 엔티티 업데이트
            analysis.updateAnalysisResult(
                    resultInput.getGazeScore(),
                    resultInput.getGazeDistribution(),
                    resultInput.getFluencyLevel(),
                    resultInput.getFluencyFeedback(),
                    resultInput.getSpeedScore(),
                    resultInput.getSpeedDistribution(),
                    resultInput.getGestureFeedbackWord(),
                    resultInput.getGestureFeedbackSentence(),
                    finalScore,
                    resultInput.getTranscript(),
                    summary,
                    finalFeedback
            );

            // 5. 프론트엔드로 SSE 알림 전송
            SseEmitter emitter = emitterRepo.get(fileKey);
            if (emitter != null) {
                emitter.send(SseEmitter.event()
                        .name("ANALYSIS_COMPLETE")
                        .data("{\"id\":\"" + analysis.getId() + "\"}"));
                emitter.complete();
                log.info("프론트엔드 알림 전송 성공: {}", analysis.getId());
            }

        } catch (Exception e) {
            log.error("비동기 분석 처리 중 에러: {}", e.getMessage(), e);
            SseEmitter emitter = emitterRepo.get(fileKey);
            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event().name("ANALYSIS_FAILED").data("에러 발생"));
                    emitter.completeWithError(e);
                } catch (IOException ignored) {}
            }
        } finally {
            // 6. 성공하든 실패하든 쓰레기 청소
            // TODO : 이것도 ai 실패했을 때 처럼 바로 삭제할지 로직을 고려를 해봐야함
            s3Service.deleteFile(fileKey);
            emitterRepo.delete(fileKey);
        }
    }
}
