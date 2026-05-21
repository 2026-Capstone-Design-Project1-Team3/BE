package com.server.talkup_be.service;

import com.server.talkup_be.dto.AnalysisDto;
import com.server.talkup_be.entity.Analysis;
import com.server.talkup_be.entity.Folder;
import com.server.talkup_be.repo.AnalysisRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AnalysisService {
    private final AnalysisRepo analysisRepo;

    public AnalysisService(AnalysisRepo analysisRepo) {
        this.analysisRepo = analysisRepo;
    }

    //analysis 페이지 수 반환
    public AnalysisDto.AnalysisPageCount getAnalysisTotal(UUID userId, String folderId, Integer type, Integer limit, String keyWord) {
        // 키워드 % 추가
        String processedKeyWord = null;
        if (keyWord != null && !keyWord.isEmpty()) {
            processedKeyWord = "%" + keyWord + "%";
        }

        // 전체 개수 및 총 페이지 수 계산
        int totalElements = analysisRepo.countFilteredAnalyses(userId.toString(), folderId, type, processedKeyWord);
        int pageSize = (limit == null || limit == 0) ? totalElements : limit;
        int totalPages = (int)Math.ceil((double)totalElements / (double)pageSize);
        AnalysisDto.AnalysisPageCount findAnalysispage = new AnalysisDto.AnalysisPageCount(totalElements,totalPages);
        return findAnalysispage;
    }

    // 연습기록 간이(미리보기) 조회
    public List<AnalysisDto.AnalysisCardnews> getAnalysisCardnewsData(UUID userId, String folderId, Integer type, Integer limit, Integer page, Integer how, String keyWord) {
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

        List<AnalysisDto.AnalysisInfo> entityList;
        return analysisRepo.findAnalyses(userId.toString(), folderId, type, processedKeyWord, pageable);
    }

    // 연습기록 상세 보기
    public AnalysisDto.AnalysisInfo getAnalysisData(UUID userId, UUID analysisId) {

        Analysis analysis = analysisRepo.findById(analysisId).orElseThrow(() -> new IllegalArgumentException("해당 연습 기록을 찾을 수 없습니다."));

        // 남의 연습기록 조회 요청시 차단
        if (!analysis.getUserId().equals(userId.toString())) {
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
            if (!analysis.getUserId().equals(userId.toString())) {
                throw new IllegalStateException("삭제 권한이 없는 연습기록이 포함되어 있습니다.");
            }
        }

        analysisRepo.deleteAll(analyses);
    }
}
