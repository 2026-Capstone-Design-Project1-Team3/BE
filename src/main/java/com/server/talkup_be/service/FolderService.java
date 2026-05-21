package com.server.talkup_be.service;

import com.server.talkup_be.dto.FolderDto;
import com.server.talkup_be.entity.Folder;
import com.server.talkup_be.repo.AnalysisRepo;
import com.server.talkup_be.repo.FolderRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FolderService {
    private final FolderRepo folderRepo;
    private final AnalysisRepo analysisRepo;

    public FolderService(FolderRepo folderRepo, AnalysisRepo analysisRepo) {
        this.folderRepo = folderRepo;
        this.analysisRepo = analysisRepo;
    }

    //폴더 생성
    public void saveFolderData(UUID userId, FolderDto.FolderInput folderInput) {
        String newOutputText = "";
//        if(folderInput.getType() == 1)
//            newOutputText = createQuestions();  //면접 질문 생성 로직 만들고 주석 풀기
        
        Folder newFolder =  Folder.builder()
                .userId(userId)
                .title(folderInput.getTitle())
                .fileName(folderInput.getFileName())
                .fileKey(folderInput.getFileKey())
                .extraInfo(folderInput.getExtraInfo())
                .companyName(folderInput.getCompanyName())
                .outputText(newOutputText)
                .inputText(folderInput.getInputText())
                .type(folderInput.getType())
                .build();

        folderRepo.save(newFolder);
    }

    //폴더 미리보기 반환
    public List<FolderDto.FolderInfo> getFolderData(UUID userId, Integer type, Integer limit, Integer page, Integer how, String keyWord) {
        // 키워드 % 추가
        String processedKeyWord = null;
        if (keyWord != null && !keyWord.isEmpty()) {
            processedKeyWord = "%" + keyWord + "%";
        }

        int pageSize = (limit == null || limit <= 0) ? Integer.MAX_VALUE : limit;
        int pageNum = (page == null || page <= 0) ? 1 : page;

        // how == 0이면 최신순, how==1이면 날짜순
        Sort sort = (how != null && how == 1)
                ? Sort.by(Sort.Direction.ASC, "updatedAt")
                : Sort.by(Sort.Direction.DESC, "updatedAt");

        // Pageable 생성
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);

        return folderRepo.findFolders(userId, type, processedKeyWord, pageable);
    }

    //폴더 삭제
    @Transactional
    public void deleteFolder(UUID userId, List<UUID> folderIds) {
        //빈 배열일 경우
        if (folderIds == null || folderIds.isEmpty()) {
            throw new IllegalArgumentException("삭제할 폴더가 선택되지 않았습니다.");
        }

        // 관련된 folder 전부 호출
        List<Folder> folders = folderRepo.findAllById(folderIds);

        // 없는 folder라면?
        if (folders.size() != folderIds.size()) {
            throw new IllegalArgumentException("존재하지 않거나 이미 삭제된 폴더가 포함되어 있습니다.");
        }

        // user의 폴더들을 하나씩 검사
        for (Folder folder : folders) {
            // 남의 폴더 삭제 요청시 차단
            if (!folder.getUserId().equals(userId)) {
                throw new IllegalStateException("삭제 권한이 없는 폴더가 포함되어 있습니다.");
            }
        }

        // 관련 analysis 지우기
        if (!folderIds.isEmpty()) {
            analysisRepo.deleteByFolderIdIn(folderIds);
        }

        // folder 지우기
        folderRepo.deleteAll(folders);
    }

    // userId와 관련된 Folder들의 id
    @Transactional
    public void deleteAllFoldersByUserId(UUID userId) {
        List<Folder> userFolders = folderRepo.findAllByUserId(userId);

        if (!userFolders.isEmpty()) {
            // Entity의 Id(UUID)로 list
            List<UUID> folderIds = userFolders.stream()
                    .map(Folder::getId)
                    .toList();

            this.deleteFolder(userId, folderIds);
        }
    }
}
