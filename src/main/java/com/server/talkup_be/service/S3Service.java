package com.server.talkup_be.service;

import com.server.talkup_be.dto.FileDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
public class S3Service {

    private final S3Presigner s3Presigner;
    private final String bucketName;

    // 생성자 주입 및 S3 Presigner 초기화 (IAM Role을 자동으로 인식함!)
    public S3Service(@Value("${cloud.aws.s3.bucket}") String bucketName,
                     @Value("${cloud.aws.region.static}") String region) {
        this.bucketName = bucketName;
        this.s3Presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create()) // 💡 EC2의 IAM Role을 알아서 찾아 쓰는 마법의 코드
                .build();
    }

    // 파일 업로드 링크 반환
    public FileDto getPresignedUploadUrl(String originalFileName) {
        // UUID를 붙여서 고유한 FileKey 생성
        String fileKey = UUID.randomUUID() + "_" + originalFileName;

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10)) // 10분간 유효한 URL
                .putObjectRequest(objectRequest)
                .build();

        String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();

        return new FileDto(uploadUrl, fileKey);
    }
}