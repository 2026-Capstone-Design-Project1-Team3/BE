package com.server.talkup_be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
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

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private Integer type;

    @Column(nullable = false)
    private String summary;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Integer gazeScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private GazeDistribution gazeDistribution;

    @Column(nullable = false)
    private String gazeFeedback;

    @Column(nullable = false)
    private Integer fluencyLevel;

    @Column(nullable = false)
    private String fluencyFeedback;

    @Column(nullable = false)
    private Integer speedScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private SpeedDistribution speedDistribution;

    @Column(nullable = false)
    private String speedFeedback;

    @Column(nullable = false)
    private String nonverbalFeedback;

    @Column(nullable = false)
    private Integer finalScore;

    private String finalFeedback;

    @Column(nullable = false)
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