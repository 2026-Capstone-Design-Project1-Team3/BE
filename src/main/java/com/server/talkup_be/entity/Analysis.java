package com.server.talkup_be.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "analysis",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_file_key_status",
                        columnNames = {"file_key", "status"} // 두 컬럼의 조합이 유니크해야 함
                )
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Analysis {
    @Id
    @Column(columnDefinition = "BINARY(16)", updatable = false, nullable = false, unique = true)
    private UUID id;
    @Column(nullable = false)
    private UUID userId;
    @Column(nullable = false)
    private UUID folderId;

    @Column(name = "file_key", nullable = false)
    private String fileKey;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus status; // PENDING, COMPLETED, FAILED

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Integer type;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // status가 pending일 때는 nullable = true

    @Column(columnDefinition = "LONGTEXT")
    private String summary;

    private Integer gazeScore;

    @Column(columnDefinition = "LONGTEXT")
    private String gazeFeedback;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private GazeDistribution gazeDistribution;

    private Float speedSpm;

    private Integer fluencyLevel;

    @Column(columnDefinition = "LONGTEXT")
    private String fluencyFeedback;

    private Integer speedScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private SpeedDistribution speedDistribution;

    private String gestureFeedbackWord;

    @Column(columnDefinition = "LONGTEXT")
    private String gestureFeedbackSentence;

    private Integer finalScore;

    @Column(columnDefinition = "LONGTEXT")
    private String finalFeedback;

    @Column(columnDefinition = "LONGTEXT")
    private String transcript;

    //랜덤 초기 id 세팅
    @PrePersist
    public void prePersist() {
        this.id = UUID.randomUUID();
        //날짜 자동 세팅
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public void updateAnalysisResult(Integer gazeScore, String gazeFeedback, GazeDistribution gazeDistribution, Integer fluencyLevel, String fluencyFeedback, Integer speedScore, SpeedDistribution speedDistribution, Float speedSpm, String gestureFeedbackWord, String gestureFeedbackSentence, int finalScore, String transcript, String summary, String finalFeedback) {
        this.gazeScore = gazeScore;
        this.gazeFeedback = gazeFeedback;
        this.gazeDistribution = gazeDistribution;
        this.fluencyLevel = fluencyLevel;
        this.fluencyFeedback = fluencyFeedback;
        this.speedScore = speedScore;
        this.speedDistribution = speedDistribution;
        this.speedSpm = speedSpm;
        this.gestureFeedbackWord = gestureFeedbackWord;
        this.gestureFeedbackSentence = gestureFeedbackSentence;
        this.finalScore = finalScore;
        this.transcript = transcript;
        this.summary = summary;
        this.finalFeedback = finalFeedback;

        this.status = AnalysisStatus.COMPLETED;
    }
}