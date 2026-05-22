package com.server.talkup_be.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class FolderDto {
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class FolderPageCount {
        private Integer totalElements;
        private Integer totalPages;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class FolderInfo {
        private UUID folderId;
        private String title;
        private Integer type;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd", timezone = "Asia/Seoul")
        private LocalDateTime updatedAt;

        private Long totalAnalyses;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class FolderInput {
        private String title;
        private String fileName;
        private String fileKey;
        private String extraInfo;
        private String companyName;
        private String inputText;
        private Integer type;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class FolderDeleteList {
        private List<UUID> folderId;
    }
    @Getter
    @NoArgsConstructor
    public static class FolderStatistics {
        private Integer gazeScore;
        private Integer speedScore;
        private Integer finalScore;

        public FolderStatistics(Double gazeScore, Double speedScore, Double finalScore) {
            this.gazeScore = (int) Math.round(gazeScore);
            this.speedScore = (int) Math.round(speedScore);
            this.finalScore = (int) Math.round(finalScore);
        }
    }
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FolderSettingRes {
        private String set; // type=0 일 때는 PDF 다운로드 URL, type=1 일 때는 면접 질문 텍스트
    }
    @Getter
    @NoArgsConstructor
    public class FolderScript {
        private String fileKey;
        private String extraInfo; // 새로 제작 시 "", 수정 시 기존 대본 텍스트
    }
    @Getter
    @AllArgsConstructor
    public static class FolderScriptRes {
        private String extraInfo;
    }
}
