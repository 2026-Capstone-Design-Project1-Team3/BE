package com.server.talkup_be.service;

import com.server.talkup_be.dto.FileDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class S3Service {

    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final S3Client s3Client; // 파일 제어(삭제)를 위한 클라이언트

    // 생성자 주입 및 S3 Presigner 초기화 (IAM Role을 자동으로 인식함)
    public S3Service(@Value("${cloud.aws.s3.bucket}") String bucketName,
                     @Value("${cloud.aws.region.static}") String region) {
        this.bucketName = bucketName;
        this.s3Presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create()) // 💡 EC2의 IAM Role을 알아서 찾아 쓰는 마법의 코드
                .build();
        this.s3Client = S3Client.builder().region(Region.of(region)).build();
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

    // 파일 다운로드 링크 반환
    public String getPresignedDownloadUrl(String fileKey) {

        // S3에서 특정 키(이름)의 파일을 가져오겠다는 요청 객체 생성
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey) // DB에 저장되어 있던 그 고유한 파일명
                .build();

        // 10분 동안만 유효한 다운로드용 Pre-signed URL 생성
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(getObjectRequest)
                .build();

        // 발급된 URL을 String으로 변환해서 반환
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    // s3 파일 일괄 삭제
    public void deleteFiles(List<String> fileKeys) {
        if (fileKeys == null || fileKeys.isEmpty()) {
            return;
        }

        // S3가 인식할 수 있는 ObjectIdentifier 형태로 파일 키 목록 변환
        List<ObjectIdentifier> objectsToDelete = fileKeys.stream()
                .filter(key -> key != null && !key.isBlank())
                .map(key -> ObjectIdentifier.builder().key(key).build())
                .collect(Collectors.toList());

        if (objectsToDelete.isEmpty()) {
            return;
        }

        // 하나의 Delete 객체로 묶기
        Delete delete = Delete.builder()
                .objects(objectsToDelete)
                .build();

        // 일괄 삭제 요청 생성
        DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                .bucket(bucketName)
                .delete(delete)
                .build();

        s3Client.deleteObjects(deleteObjectsRequest);
    }
}