package com.server.talkup_be.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {

    private final String apiKey;
    private final String assistantId;
    private final String bucketName;
    private final S3Client s3Client;
    private final RestTemplate restTemplate = new RestTemplate();

    public OpenAiService(@Value("${openai.api.key}") String apiKey,
                         @Value("${openai.api.assistant-id}") String assistantId,
                         @Value("${cloud.aws.s3.bucket}") String bucketName,
                         @Value("${cloud.aws.region.static}") String region) {
        this.apiKey = apiKey;
        this.assistantId = assistantId;
        this.bucketName = bucketName;
        this.s3Client = S3Client.builder().region(Region.of(region)).build();
    }

    public String generateOrModifyScript(String fileKey, String extraInfo) throws InterruptedException {
        // S3에서 PDF 파일 바이트로 가져오기
        byte[] pdfBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucketName).key(fileKey).build()).asByteArray();

        // OpenAI에 파일 업로드하고 file_id 받기
        String fileId = uploadFileToOpenAi(pdfBytes);

        // 대본 작성 요청 실행하고 thread_id, run_id 받기
        Map<String, String> ids = createThreadAndRun(fileId, extraInfo);
        String threadId = ids.get("thread_id");
        String runId = ids.get("run_id");

        // 대본이 다 써질 때까지 1초마다 확인
        waitForCompletion(threadId, runId);

        // 최종 완성된 대본 텍스트
        return getLatestMessage(threadId);
    }

    // --- 내부에서 사용하는 통신 메서드 ---

    private String uploadFileToOpenAi(byte[] pdfBytes) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(apiKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("purpose", "assistants");
        body.add("file", new ByteArrayResource(pdfBytes) {
            @Override
            public String getFilename() { return "material.pdf"; } // 이름이 pdf로 끝나야 인식함
        });

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.openai.com/v1/files", new HttpEntity<>(body, headers), Map.class);
        return (String) response.getBody().get("id");
    }

    private Map<String, String> createThreadAndRun(String fileId, String extraInfo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        headers.set("OpenAI-Beta", "assistants=v2"); // 필수 헤더

        String prompt = (extraInfo == null || extraInfo.trim().isEmpty())
                ? "첨부된 PDF 발표 자료의 내용을 파악해서 훌륭한 발표 대본을 작성해 줘."
                : "첨부된 PDF 발표 자료를 참고해서, 다음 대본을 더 자연스럽게 수정해 줘:\n\n" + extraInfo;

        Map<String, Object> body = Map.of(
                "assistant_id", assistantId,
                "thread", Map.of(
                        "messages", List.of(Map.of(
                                "role", "user",
                                "content", prompt,
                                "attachments", List.of(Map.of(
                                        "file_id", fileId,
                                        "tools", List.of(Map.of("type", "file_search"))
                                ))
                        ))
                )
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.openai.com/v1/threads/runs", new HttpEntity<>(body, headers), Map.class);
        return Map.of(
                "thread_id", (String) response.getBody().get("thread_id"),
                "run_id", (String) response.getBody().get("id")
        );
    }

    private void waitForCompletion(String threadId, String runId) throws InterruptedException {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.set("OpenAI-Beta", "assistants=v2");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://api.openai.com/v1/threads/" + threadId + "/runs/" + runId;

        // 상태가 'completed'가 될 때까지 1초씩 쉬면서 계속 물어봄
        while (true) {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            String status = (String) response.getBody().get("status");

            if ("completed".equals(status)) {
                break;
            } else if ("failed".equals(status) || "cancelled".equals(status) || "expired".equals(status)) {
                throw new RuntimeException("GPT 대본 생성 실패: " + status);
            }
            Thread.sleep(1000); // 1초 대기
        }
    }

    private String getLatestMessage(String threadId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.set("OpenAI-Beta", "assistants=v2");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://api.openai.com/v1/threads/" + threadId + "/messages";
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        // 응답 배열 중 가장 최신(첫 번째) 메시지의 텍스트를 추출
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        List<Map<String, Object>> contentList = (List<Map<String, Object>>) data.get(0).get("content");
        Map<String, Object> textObj = (Map<String, Object>) contentList.get(0).get("text");
        return (String) textObj.get("value");
    }
}