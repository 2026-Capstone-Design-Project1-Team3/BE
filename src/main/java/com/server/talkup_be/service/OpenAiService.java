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

    // 발표 대본 생성
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

    // 면접 예상 질문 5개 생성
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

    // --- 발표 내부에서 사용하는 통신 메서드 ---

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
                ? "너는 10년 차 전문 프레젠테이션 컨설턴트이자 스피치 라이터야. 첨부된 문서를 완벽하게 분석해서, 발표자가 당장 무대에서 사용할 수 있는 [매우 자연스럽고 내용이 풍부한 발표 대본]을 작성해 줘.\n\n" +
                "다음 4가지 지침을 무조건 엄수해:\n" +
                "1. 완벽한 기승전결 구조: \n" +
                "   - [오프닝]: 청중의 이목을 끄는 자연스러운 인사말과 발표 주제, 배경 소개로 시작해.\n" +
                "   - [본론]: 문서의 텍스트만 단순 나열하지 마. 각 핵심 내용마다 '왜 이 기술/기획을 선택했는지', '어떤 기대효과가 있는지' 등 발표자가 살을 붙여 설명하듯 부연 설명을 아주 구체적으로 추가해서 분량을 넉넉하게 채워.\n" +
                "   - [클로징]: 핵심 내용을 한 번 깔끔하게 요약하고, 청중에게 주는 가치 강조 후 질의응답(Q&A)을 유도하며 마무리해.\n" +
                "2. 자연스러운 구어체: 딱딱한 보고서 말투(문어체)는 절대 금지. 청중과 눈을 맞추고 대화하듯 아주 자연스러운 발표용 구어체(~습니다, ~합니다, ~해 보겠습니다)를 사용해. 문장과 문장 사이를 이어주는 부드러운 접속사도 적극 활용해.\n" +
                "3. 열정과 자신감: 발표자의 열정과 프로젝트에 대한 자신감이 느껴지는 전문적인 톤앤매너를 유지해.\n" +
                "4. 제한사항: '【4:6†material.pdf】'와 같은 출처 주석 기호나 마크다운 특수 기호(###, **)는 절대 포함하지 마. 오직 발표자가 바로 읽을 수 있는 순수 대본 텍스트만 단락을 나누어 예쁘게 출력해."
                : "너는 전문 스피치 라이터야. 첨부된 문서의 내용과 맥락을 참고해서, 아래 사용자가 제시한 대본을 훨씬 더 자연스럽고 내용이 풍성하게 [수정 및 보완]해 줘.\n\n" +
                "[지침]\n" +
                "1. 딱딱한 말투나 어색한 문장을 전문적이고 자연스러운 발표용 구어체(~습니다, ~합니다)로 다듬어 줘.\n" +
                "2. 내용이 너무 빈약하다면 첨부 문서를 바탕으로 구체적인 설명이나 예시를 덧붙여서 분량을 적절히 늘려 줘.\n" +
                "3. '【4:6†material.pdf】'와 같은 출처 주석 기호나 특수 기호는 절대 출력하지 마.\n\n" +
                "[수정할 기존 대본 정보]:\n" + extraInfo;

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


    // --- 면접 질문 매서드 ---
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