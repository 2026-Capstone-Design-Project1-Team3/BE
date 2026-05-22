package com.server.talkup_be.service;

import com.server.talkup_be.dto.FileDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class S3Service {

    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final S3Client s3Client; // 파일 제어(삭제)를 위한 클라이언트

    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiKey;

    // 생성자 주입 및 S3 Presigner 초기화 (IAM Role을 자동으로 인식함)
    public S3Service(@Value("${cloud.aws.s3.bucket}") String bucketName,
                     @Value("${cloud.aws.region.static}") String region,
                     @Value("${openai.api.key}") String apiKey) {
        this.bucketName = bucketName;
        this.apiKey = apiKey;

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

    // s3의 파일을 읽어 openAI에 업로드 후 file_id 반환
    private String uploadPdfToOpenAi(String fileKey) {
        // S3에서 PDF 파일 가져오기
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
        byte[] pdfBytes = objectBytes.asByteArray();

        // OpenAI Files API 호출 준비
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(apiKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("purpose", "assistants");
        body.add("file", new ByteArrayResource(pdfBytes) {
            @Override
            public String getFilename() {
                return "presentation_material.pdf"; // 확장자가 pdf여야 인식함
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.openai.com/v1/files", requestEntity, Map.class);

        if (response.getBody() != null && response.getBody().containsKey("id")) {
            return (String) response.getBody().get("id");
        }

        throw new RuntimeException("OpenAI 파일 업로드 실패");
    }
}