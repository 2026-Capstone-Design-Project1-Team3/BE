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

    // 면접 예상 질문 5개 생성 메서드
    public String generateInterviewQuestionsWithFile(String fileKey, String companyName, String inputText, List<String> recentSummaries) throws InterruptedException {
        // 1. S3에서 파일 바이트로 가져오기
        byte[] pdfBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucketName).key(fileKey).build()).asByteArray();

        // 2. OpenAI에 파일 업로드하고 fileId 받기
        String fileId = uploadFileToOpenAi(pdfBytes);

        // 3. 동적 프롬프트 조립
        StringBuilder userPromptBuilder = new StringBuilder();
        userPromptBuilder.append(String.format("첨부된 포트폴리오/이력서 문서와 아래 정보를 완벽하게 분석해서 면접 질문을 생성해.\n지원 회사: %s\n자기소개서: %s\n\n",
                companyName != null ? companyName : "알 수 없음",
                inputText != null ? inputText : "일반적인 직무 역량 기반"));

        if (recentSummaries == null || recentSummaries.isEmpty()) {
            userPromptBuilder.append("첨부된 파일을 바탕으로, 실전 면접 예상 질문 딱 5개만 생성해 줘.");
        } else {
            userPromptBuilder.append("다음은 지원자의 최근 면접 연습 답변 요약 기록이야:\n");
            for (int i = 0; i < recentSummaries.size(); i++) {
                userPromptBuilder.append(String.format("- 이전 연습 답변 %d: %s\n", i + 1, recentSummaries.get(i)));
            }
            userPromptBuilder.append("\n첨부된 파일의 세부 내용과 위의 이전 답변 요약들을 종합적으로 분석해서, 지원자의 주장을 더 깊게 파고들거나 문서에 적힌 경험을 구체적으로 검증하는 날카로운 심층 압박 질문(꼬리 질문) 딱 5개를 생성해 줘.");
        }

        // 파싱을 위한 절대 규칙
        userPromptBuilder.append("\n\n조건: 반드시 각 질문 사이에만 '<q>' 구분자를 넣어서 한 줄로 출력해. 숫자(1., 2.), 줄바꿈, 【4:6†material.pdf】 같은 출처 주석 기호나 인사말은 절대 넣지 마.");

        // 4. 면접 질문 전용 Thread 생성 및 Run
        Map<String, String> ids = createInterviewThreadAndRun(fileId, userPromptBuilder.toString());
        String threadId = ids.get("thread_id");
        String runId = ids.get("run_id");

        // 5. 완료될 때까지 대기
        waitForCompletion(threadId, runId);

        // 6. 결과 반환
        return getLatestMessage(threadId);
    }

    // 면접 질문 매서드
    private Map<String, String> createInterviewThreadAndRun(String fileId, String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        headers.set("OpenAI-Beta", "assistants=v2");

        Map<String, Object> body = Map.of(
                "assistant_id", assistantId, // (기존에 만든 gpt-4o 기반 assistant 재활용)
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
}