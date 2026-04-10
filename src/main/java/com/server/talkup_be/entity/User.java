package com.server.talkup_be.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class User {
    @Id
    @Column(columnDefinition = "BINARY(16)", updatable = false, nullable = false, unique = true)
    private UUID id;
    @Column(nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String passWord;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private EyeCalibration eyeCalibration;

    //랜덤 초기 id 세팅
    @PrePersist
    public void prePersist() {
        this.id = UUID.randomUUID();
    }

    //id 반환
    public UUID getId() {
        return id;
    }

    //시선 캘리브레이션 업데이트
    public void updateEyeCalibration(EyeCalibration eyeCalibration) {this.eyeCalibration = eyeCalibration;}

    //비번 제외 수정
    public void  updateUser(String newName, String newEmail)
    {
        this.name= newName;
        this.email = newEmail;
    }

    public void updatePassword(@Nullable String encode) {
        this.passWord = encode;
    }
}

