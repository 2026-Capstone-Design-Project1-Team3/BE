package com.server.talkup_be.entity;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Folder {
    @Id
    @Column(columnDefinition = "BINARY(16)", updatable = false, nullable = false, unique = true)
    private UUID id;
    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileKey;

    @Column(nullable = false)
    private String extraInfo;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String outputText;

    @Column(nullable = false)
    private String inputText;

    @Column(nullable = false)
    private Integer type;

    @Column(nullable = false)
    private LocalDateTime createdAt;

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

