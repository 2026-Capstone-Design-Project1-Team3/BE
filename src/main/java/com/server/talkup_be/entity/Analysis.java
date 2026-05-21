package com.server.talkup_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
    private String userId;
    @Column(nullable = false)
    private String folderId;

    @Column(name = "file_key", nullable = false)
    private String fileKey;

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

    private String summary;

    private Integer gazeScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private GazeDistribution gazeDistribution;

    private Integer fluencyLevel;

    private String fluencyFeedback;

    private Integer speedScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private SpeedDistribution speedDistribution;

    private String gestureFeedbackWord;

    private String gestureFeedbackSentence;

    private Integer finalScore;

    private String finalFeedback;

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
}