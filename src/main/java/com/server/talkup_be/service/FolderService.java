package com.server.talkup_be.service;

import com.server.talkup_be.dto.FolderDto;
import com.server.talkup_be.entity.Folder;
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

    public FolderService(FolderRepo folderRepo) {
        this.folderRepo = folderRepo;
    }

    //폴더 생성
    public void saveFolderData(UUID userId, FolderDto.FolderInput folderInput) {
        String newOutputText = "";
//        if(folderInput.getType() == 1)
//            newOutputText = createQuestions();  //면접 질문 생성 로직 만들고 주석 풀기
        
        Folder newFolder =  Folder.builder()
                .userId(userId.toString())
                .title(folderInput.getTitle())
                .description(folderInput.getDescription())
                .description(folderInput.getDescription())
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

    //폴더 페이지 수 반환
    public FolderDto.FolderPageCount getFolderTotal(UUID userId, Integer type, Integer limit, String keyWord) {
        // 키워드 % 추가
        String processedKeyWord = null;
        if (keyWord != null && !keyWord.isEmpty()) {
            processedKeyWord = "%" + keyWord + "%";
        }

        // 전체 개수 및 총 페이지 수 계산
        int totalElements = folderRepo.countFilteredFolders(userId.toString(), type, processedKeyWord);
        int pageSize = (limit == null) ? totalElements : limit;
        int totalPages = (int)Math.ceil((double)totalElements / (double)pageSize);
        FolderDto.FolderPageCount findFolderpage = new FolderDto.FolderPageCount(totalElements,totalPages);
        return findFolderpage;
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
                ? Sort.by(Sort.Direction.ASC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");

        // Pageable 생성
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);

        List<FolderDto.FolderInfo> entityList;
        return folderRepo.findFolders(userId.toString(), type, processedKeyWord,pageable);
    }

    @Transactional
    public void deleteFolder(UUID userId, List<UUID> folderIds) {
        //빈 배열일 경우
        if (folderIds == null || folderIds.isEmpty()) {
            throw new IllegalArgumentException("삭제할 폴더가 선택되지 않았습니다.");
        }

        // userId와 관련된 folder 전부 호출
        List<Folder> folders = folderRepo.findAllById(folderIds);

        // user의 폴더들을 하나씩 검사
        for (Folder folder : folders) {
            // 남의 폴더 삭제 요청시 차단
            if (!folder.getUserId().equals(userId.toString())) {
                throw new IllegalStateException("삭제 권한이 없는 폴더가 포함되어 있습니다.");
            }
        }

        folderRepo.deleteAll(folders);
    }
}
