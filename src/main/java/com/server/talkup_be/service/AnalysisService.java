package com.server.talkup_be.service;

import com.server.talkup_be.dto.AnalysisDto;
import com.server.talkup_be.repo.AnalysisRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

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
    public List<AnalysisDto.AnalysisCardnews> getAnalysisData(UUID userId, String folderId, Integer type, Integer limit, Integer page, Integer how, String keyWord) {
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
}
