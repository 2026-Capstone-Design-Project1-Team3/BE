package com.server.talkup_be.service;

import com.server.talkup_be.dto.AnalysisDto;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
        String rawText = (String) textObj.get("value");

        //【】및 마크다운 기호 지우기
        return rawText.replaceAll("【.*?】", "")
                .replaceAll("###", "")
                .replaceAll("\\*\\*", "")
                .trim();
    }


    // --- 면접 질문 매서드 ---
    private Map<String, String> createInterviewThreadAndRun(String fileId, String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        headers.set("OpenAI-Beta", "assistants=v2");

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

    // summary(요약) 생성 메서드
    public String summarizeTranscript(String transcript) {
        if (transcript == null || transcript.isBlank()) {
            return "발화 내용이 없습니다.";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        String systemPrompt = "너는 면접 답변 요약 전문가야. 사용자의 전체 발화 내용을 핵심만 파악하여 3문장 이내로 짧고 명확하게 요약해 줘.";

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", transcript)
                )
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.openai.com/v1/chat/completions",
                new HttpEntity<>(requestBody, headers),
                Map.class
        );

        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        }

        return "요약에 실패했습니다.";
    }

    // 텍스트 기반 finalFeedback 생성(주로 발표)
    public String generateFinalFeedback(AnalysisDto.ResultInput result, String backgroundInfo) {
        if (result.getTranscript() == null || result.getTranscript().isBlank()) {
            return "분석할 내용이 없습니다.<q>없음<q>없음<q>없음<q>없음";
        }

        // 1. 시스템 프롬프트 (조건 명확화)
        String systemPrompt = "너는 날카롭고 전문적인 면접/발표 평가관이야. 사용자의 발화 내용, AI 음성/비전 분석 데이터, 그리고 배경 상황(대본 또는 질문)을 종합적으로 분석해서 다음 5가지 항목을 추출해 줘.\n" +
                "조건 1: 반드시 각 항목 사이를 '<q>' 로만 구분해서 한 줄로 출력해.\n" +
                "조건 2: 첫 번째 항목은 1줄짜리 전체 총평이며, 무조건 '~다.' 로 끝나야 해.\n" +
                "조건 3: 나머지 4개 항목(강점 2개, 개선사항 2개)은 구체적인 '명사구' 형태로 작성해.\n" +
                "예시: 전체적인 전달력은 우수하지만, 결론부의 강조를 위한 완급 조절이 보완되면 완벽한 발표가 될 것입니다.<q>안정적인 시선 처리 및 청중 교감<q>자연스러운 제스처와 신체 언어<q>핵심 키워드 발음 시 강세 조절<q>대본의 결론부 구체성 추가\n" +
        "조건 4: 발표(대본이 있는 경우)라면 '원본 대본과 실제 발화의 내용 일치도(누락된 내용 등)'를 반드시 분석하여 총평이나 강점/개선사항에 반영해.\n" +
                "예시: 대본의 핵심 내용은 잘 전달했으나 시선 처리가 다소 불안정하여 신뢰감을 주는 연습이 필요합니다.<q>대본 내용의 완벽한 숙지<q>자연스러운 제스처와 신체 언어<q>핵심 키워드 발음 시 강세 조절<q>카메라 응시 비율 증가";
        // 2. 배경 정보 이름 설정
        String contextType = (result.getType() == 0) ? "원본 발표 대본 (비교용)" : "면접 질문";
        String safeBackgroundInfo = (backgroundInfo != null && !backgroundInfo.isBlank()) ? backgroundInfo : "제공되지 않음";

        // 3. AI 분석 데이터를 프롬프트에 주입 (동적 텍스트 생성)
        String userContent = String.format(
                "[%s]\n%s\n\n" +
                        "[사용자 실제 발화(Transcript)]\n%s\n\n" +
                        "[AI 비전/음성 분석 데이터 종합]\n" +
                        "- 시선 처리: %d점 (화면 응시 %d%%, 카메라 응시 %d%%)\n" +
                        "- 발화 속도: %d점 (빠름 %d%%, 적절 %d%%, 느림 %d%%)\n" +
                        "- 유창성 분석: %s\n" +
                        "- 제스처 분석: %s (%s)",
                contextType, safeBackgroundInfo, result.getTranscript(),
                result.getGazeScore(),
                result.getGazeDistribution() != null ? result.getGazeDistribution().getScreen() : 0,
                result.getGazeDistribution() != null ? result.getGazeDistribution().getCamera() : 0,
                result.getSpeedScore(),
                result.getSpeedDistribution() != null ? result.getSpeedDistribution().getFast() : 0,
                result.getSpeedDistribution() != null ? result.getSpeedDistribution().getOptimal() : 0,
                result.getSpeedDistribution() != null ? result.getSpeedDistribution().getSlow() : 0,
                result.getFluencyFeedback() != null ? result.getFluencyFeedback() : "특이사항 없음",
                result.getGestureFeedbackWord() != null ? result.getGestureFeedbackWord() : "없음",
                result.getGestureFeedbackSentence() != null ? result.getGestureFeedbackSentence() : "특이사항 없음"
        );

        return callChatApi(systemPrompt, userContent);
    }

    // 포트폴리오 기반 면접 최종 분석 (점수 + 피드백 통합)
    public String generateAdvancedInterviewFeedback(String pdfFileKey, String question, AnalysisDto.ResultInput result) throws Exception {

        // 1. S3에서 포트폴리오 PDF 다운로드
        byte[] pdfBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucketName).key(pdfFileKey).build()).asByteArray();

        // 2. OpenAI에 PDF 업로드
        String fileId = uploadFileToOpenAi(pdfBytes);

        // 3. 점수와 피드백 추출
        String userPrompt = String.format(
                "첨부된 지원자의 포트폴리오(이력서)와 아래의 면접 데이터를 완벽하게 대조 분석해서, 면접 평가 점수와 피드백을 생성해.\n\n" +
                        "[면접 질문]\n%s\n\n" +
                        "[지원자 실제 답변(Transcript)]\n%s\n\n" +
                        "[AI 비전/음성 분석 데이터]\n" +
                        "- 시선 처리: %d점\n" +
                        "- 발화 속도: %d점\n" +
                        "- 유창성: %s\n" +
                        "- 제스처: %s\n\n" +
                        "조건 1: 반드시 6개의 항목을 '<q>'로만 구분해서 한 줄로 출력해.\n" +
                        "조건 2: 첫 번째 항목은 포트폴리오 기반 직무 적합성과 논리성을 평가한 '면접 점수(0~100 정수)'야.\n" +
                        "조건 3: 두 번째 항목은 1줄짜리 전체 총평이며, 반드시 '~다.'로 끝나야 해. 가급적 이력서 경험과 비교해서 피드백해.\n" +
                        "조건 4: 나머지 4개 항목(강점 2개, 개선사항 2개)은 구체적인 '명사구' 형태로 작성해.\n" +
                        "예시: 85<q>포트폴리오의 웹 개발 경험을 잘 어필했으나, 기술적 깊이에 대한 설명이 다소 부족하다.<q>직무 경험 어필<q>자연스러운 시선 처리<q>경험의 구체성 보완<q>발화 속도 조절",
                (question != null && !question.isBlank()) ? question : "일반 면접 질문",
                result.getTranscript(),
                result.getGazeScore(),
                result.getSpeedScore(),
                result.getFluencyFeedback() != null ? result.getFluencyFeedback() : "특이사항 없음",
                result.getGestureFeedbackSentence() != null ? result.getGestureFeedbackSentence() : "특이사항 없음"
        );

        // 4. Thread 생성 및 실행 (기존 메서드 재활용)
        Map<String, String> ids = createInterviewThreadAndRun(fileId, userPrompt);
        String threadId = ids.get("thread_id");
        String runId = ids.get("run_id");

        // 5. 분석 완료 대기 후 텍스트 반환
        waitForCompletion(threadId, runId);
        return getLatestMessage(threadId);
    }

    // 면접 예비 finalScore 생성
    // 면접 final 생성 중 s3 연동이 잘안되면 이거 사용
    public int generateInterviewScore(String transcript, String question) {
        if (transcript == null || transcript.isBlank()) return 0;

        String systemPrompt = "너는 10년차 실무 면접관이야. 주어진 [면접 질문]과 지원자의 [실제 답변 발화]를 읽고, 동문서답을 하지는 않았는지, 논리성이 뛰어난지를 평가해서 0부터 100 사이의 '정수(숫자)'로만 점수를 매겨. 기호는 절대 출력하지 마.";

        String userContent = String.format(
                "[면접 질문]\n%s\n\n[실제 답변 발화]\n%s",
                (question != null && !question.isBlank()) ? question : "일반적인 인성/직무 면접 질문",
                transcript
        );

        try {
            String rawScoreStr = callChatApi(systemPrompt, userContent).trim();
            // 숫자만 남김
            String onlyNumberStr = rawScoreStr.replaceAll("[^0-9]", "");

            return Integer.parseInt(onlyNumberStr);
        } catch (NumberFormatException e) {
            return 0; // 파싱 실패 시 기본 점수
        }
    }

    // --- 중복 코드 제거용 Chat API 호출 메서드 ---
    private String callChatApi(String systemPrompt, String userContent) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o", // 빠르고 가성비 좋은 모델!
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userContent)
                )
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.openai.com/v1/chat/completions", new HttpEntity<>(requestBody, headers), Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        }
        return "";
    }
}