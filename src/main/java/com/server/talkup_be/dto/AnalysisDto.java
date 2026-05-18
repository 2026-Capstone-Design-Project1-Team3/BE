package com.server.talkup_be.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.server.talkup_be.entity.GazeDistribution;
import com.server.talkup_be.entity.SpeedDistribution;
import jakarta.persistence.Column;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AnalysisDto {
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class AnalysisPageCount {
        private Integer totalElements;
        private Integer totalPages;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class AnalysisCardnews {
        private UUID analysisId;
        private String title;
        private String description;
        private Integer type;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd", timezone = "Asia/Seoul")
        private LocalDateTime createdAt;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class ResultInput {
        private String testId;
        private Integer gazeScore;

        private GazeDistribution gazeDistribution;

        private Integer fluencyLevel;
        private Integer speedScore;

        private SpeedDistribution speedDistribution;

        private String speedFeedback;
        private Integer finalScore;
        private String transcript;
        private String fileKey;
        private Integer type;

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        @ToString
        public static class GazeDistribution {
            private Float screen;
            private Float camera;
        }

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        @ToString
        public static class SpeedDistribution {
            private Float fast;
            private Float optimal;
            private Float slow;
        }
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class AnalysisInfo {
        private UUID analysisId;
        private String folderId;
        private String title;
        private String description;
        private Integer type;
        private String summary;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd", timezone = "Asia/Seoul")
        private LocalDateTime createdAt;

        private Integer gazeScore;
        private GazeDistribution gazeDistribution;
        private String gazeFeedback;
        private Integer fluencyLevel;
        private String fluencyFeedback;
        private Integer speedScore;
        private SpeedDistribution speedDistribution;
        private String speedFeedback;
        private String nonverbalFeedback;
        private Integer finalScore;
        private String finalFeedback;
        private String transcript;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class AnalysisDeleteList {
        private List<UUID> analysisId;
    }
}
