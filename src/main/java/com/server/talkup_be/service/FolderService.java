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
}
