package com.server.talkup_be.service;

import com.server.talkup_be.dto.AnalysisDto;
import com.server.talkup_be.dto.FolderDto;
import com.server.talkup_be.repo.AnalysisRepo;
import com.server.talkup_be.repo.FolderRepo;
import org.springframework.stereotype.Service;

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
        int totalElements = analysisRepo.countFilteredFolders(userId.toString(), folderId, type, processedKeyWord);
        int pageSize = (limit == null || limit == 0) ? totalElements : limit;
        int totalPages = (int)Math.ceil((double)totalElements / (double)pageSize);
        AnalysisDto.AnalysisPageCount findAnalysispage = new AnalysisDto.AnalysisPageCount(totalElements,totalPages);
        return findAnalysispage;
    }
}
