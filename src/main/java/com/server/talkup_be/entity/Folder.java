package com.server.talkup_be.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private String fileName;

    @Column(nullable = false)
    private String fileKey;

    @Column(nullable = false)
    private String extraInfo;

    private String companyName;

    private String outputText;

    private String inputText;

    @Column(nullable = false)
    private Integer type;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    //랜덤 초기 id 세팅
    @PrePersist
    public void prePersist() {
        this.id = UUID.randomUUID();
        //날짜 자동 세팅
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }
}

