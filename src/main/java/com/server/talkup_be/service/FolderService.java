package com.server.talkup_be.service;

import com.server.talkup_be.dto.FolderDto;
import com.server.talkup_be.entity.EyeCalibration;
import com.server.talkup_be.entity.Folder;
import com.server.talkup_be.repo.AnalysisRepo;
import com.server.talkup_be.repo.FolderRepo;
import com.server.talkup_be.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class FolderService {
    private final FolderRepo folderRepo;
    private final AnalysisRepo analysisRepo;
    private final UserRepo userRepo;
    private final S3Service s3Service;
    private final OpenAiService openAiService;

    //폴더 생성
    public UUID saveFolderData(UUID userId, FolderDto.FolderInput folderInput) {
        String newOutputText = "";
        // type=1이면? (면접이면)
        if (folderInput.getType() == 1) {
            try {
                // 1. 최근 분석 요약본 3개 (생성중에는 없으니까 빈 리스트 반환)
                List<String> recentSummaries = List.of();

                // 2. OpenAI 호출
                newOutputText = openAiService.generateInterviewQuestionsWithFile(
                        folderInput.getFileKey(),
                        folderInput.getCompanyName(),
                        folderInput.getInputText(),
                        recentSummaries,
                        null
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("면접 질문 생성 중 서버 지연이 발생했습니다.", e);
            } catch (Exception e) {
                // S3 통신 실패 등 기타 에러 처리
                throw new RuntimeException("면접 질문 생성 실패: " + e.getMessage(), e);
            }
        }
        
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

        Folder result = folderRepo.save(newFolder);
        return result.getId();
    }

    //폴더 미리보기 반환
    public List<FolderDto.FolderCardnewsInfo> getFolderCardnewsData(UUID userId, Integer type, Integer limit, Integer page, Integer how, String keyWord) {
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

    // 폴더 상세정보 반환
    public FolderDto.FolderInfo getFolderData(UUID userId, UUID folderId) {
        // folderId로 폴더 존재 여부 검사 (없으면 404 에러)
        Folder folder = folderRepo.findById(folderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 폴더입니다."));
        //folder의 userId로 권한 검사
        if (!folder.getUserId().equals(userId)) {
            throw new IllegalStateException("접근 권한이 없는 폴더입니다.");
        }
        return FolderDto.FolderInfo.builder()
                .title(folder.getTitle())
                .fileName(folder.getFileName())
                .extraInfo(folder.getExtraInfo())
                .companyName(folder.getCompanyName())
                .inputText(folder.getInputText())
                .build();
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

        // fileKey 리스트 만들기
        List<String> fileKeys = folders.stream()
                .map(Folder::getFileKey)
                .filter(key -> key != null && !key.isBlank())
                .collect(Collectors.toList());

        // fileKey로 s3파일 한 번에 삭제
        if (!fileKeys.isEmpty()) {
            s3Service.deleteFiles(fileKeys);
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

    // folder의 연습기록들 통계 조회
    public FolderDto.FolderStatistics getFolderStatistics(UUID userId, UUID folderId) {
        // 해당 폴더가 가진 연습기록의 피드백 평균값
        return analysisRepo.findStatisticsByFolderId(folderId);
    }

    // folder의 세팅 조회
    @Transactional(readOnly = true)
    public FolderDto.FolderSettingRes getFolderSetting(UUID userId, UUID folderId) {
        // folderId로 폴더 존재 여부 검사 (없으면 404 에러)
        Folder folder = folderRepo.findById(folderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 폴더입니다."));
        //folder의 userId로 권한 검사
        if (!folder.getUserId().equals(userId)) {
            throw new IllegalStateException("접근 권한이 없는 폴더입니다.");
        }

        String resultContents = "";

        // 폴더의 type에 따른 분기 처리
        if (folder.getType() == 0) {
            // 발표(type=0): 10분짜리 다운로드 임시 URL 발급
            String fileKey = folder.getFileKey();
            if (fileKey == null || fileKey.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "등록된 발표 자료(PDF)가 없습니다.");
            }
            resultContents = s3Service.getPresignedDownloadUrl(fileKey);

        } else if (folder.getType() == 1) {
            // 면접 질문 반환
            resultContents = folder.getOutputText();
            if (resultContents == null || resultContents.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "등록된 면접 질문이 없습니다.");
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 폴더 타입입니다.");
        }

        EyeCalibration userEyeCalibration = userRepo.findById(userId).get().getEyeCalibration();

        return FolderDto.FolderSettingRes.builder()
                .set(resultContents)
                .eyeCalibration(userEyeCalibration)
                .build();
    }

}
