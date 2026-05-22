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
}
