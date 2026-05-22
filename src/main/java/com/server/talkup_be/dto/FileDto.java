package com.server.talkup_be.dto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FileDto {
    private String uploadUrl; // 임시 주소
    private String fileKey;   // 파일 식별용 고유 키
}