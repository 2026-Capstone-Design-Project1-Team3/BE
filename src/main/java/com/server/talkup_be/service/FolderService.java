package com.server.talkup_be.service;

import com.server.talkup_be.dto.FolderDto;
import com.server.talkup_be.entity.Folder;
import com.server.talkup_be.repo.FolderRepo;
import org.springframework.stereotype.Service;

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
        int pageSize = (limit == null) ? 1 : limit;
        // 전체 개수 및 총 페이지 수 계산
        int totalElements = folderRepo.countFilteredFolders(userId.toString(), type, keyWord);
        int totalPages = (int)Math.ceil((double)totalElements / (double)pageSize);
        FolderDto.FolderPageCount findFolderpage = new FolderDto.FolderPageCount(totalElements,totalPages);
        return findFolderpage;
    }

    //폴더 미리보기 반환
//    public FolderDto.FolderInfo getFolderData(UUID userId, Integer type, Integer limit, Integer page, Integer how, String keyWord) {
//        int pageSize = (limit == null || page == null) ? 5 : limit;
//        // 전체 개수 및 총 페이지 수 계산
//        int totalElements = folderRepo.countFilteredFolders(type, keyWord);
//        int totalPages = (int)Math.ceil((double)totalElements / (double)pageSize);
//        FolderDto.FolderInfo findFolderInfo = new FolderDto.FolderInfo(?);
//        return findFolderInfo;
//    }
}
