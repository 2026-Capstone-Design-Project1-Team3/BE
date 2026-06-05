package com.server.talkup_be.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.server.talkup_be.entity.EyeCalibration;
import com.server.talkup_be.entity.GazeDistribution;
import com.server.talkup_be.entity.SpeedDistribution;
import lombok.*;
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
    public static class AnalysisCardnewsInfo {
        private Integer total;

        private List<AnalysisCardnews> cardnews;

        @Builder
        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        @ToString
        public static class AnalysisCardnews {
            private UUID analysisId;
            private String title;
            private Integer type;

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd", timezone = "Asia/Seoul")
            private LocalDateTime createdAt;
        }
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class ResultInput {
        private UUID analysisId;
        private Integer gazeScore;
        private String gazeFeedback;
        private GazeDistribution gazeDistribution;
        private Integer fluencyLevel;
        private String fluencyFeedback;
        private Integer speedScore;
        private Float speedSpm;
        private SpeedDistribution speedDistribution;
        private String gestureFeedbackWord;
        private String gestureFeedbackSentence;
        private Integer finalScore;
        private String transcript;
        private String fileKey;
        private Integer type;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class AnalysisInfo {
        private UUID analysisId;
        private UUID folderId;
        private String title;
        private Integer type;
        private String summary;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd", timezone = "Asia/Seoul")
        private LocalDateTime createdAt;

        private Integer gazeScore;
        private String gazeFeedback;
        private GazeDistribution gazeDistribution;
        private Integer fluencyLevel;
        private String fluencyFeedback;
        private Integer speedScore;
        private Float speedSpm;
        private SpeedDistribution speedDistribution;
        private String gestureFeedbackWord;
        private String gestureFeedbackSentence;
        private Integer finalScore;
        private String finalFeedback;
        private String transcript;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class AnalysisStatistics {
        private Integer total;

        private List<StatisticData> statistics;

        @Builder
        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        @ToString
        public static class StatisticData {
            private Integer gazeScore;
            private Integer speedScore;
        }
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class AnalysisDeleteList {
        private List<UUID> analysisId;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class PendingAnalysisResult {
        private UUID analysisId;
        private String extraInfo;
        private EyeCalibration eyeCalibration;
    }
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class ResultFail {
        private UUID analysisId;
        private String fileKey;
    }
}
