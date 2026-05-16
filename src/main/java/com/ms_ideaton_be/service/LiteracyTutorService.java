package com.ms_ideaton_be.service;

import com.ms_ideaton_be.domain.LearningRecord;
import com.ms_ideaton_be.repository.LearningRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LiteracyTutorService {

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final LearningRecordRepository learningRecordRepository;

    // 1. 전체 글 분석 및 저장 (SSOK 페르소나 적용)
    @Transactional
    public String analyzeAndSave(String title, String text, String targetLevel) {
        String prompt = String.format(
                "당신은 사용자의 문해력을 책임지는 스마트한 도우미 'SSOK(쏙)'입니다.\n" +
                        "어려운 글 내용을 사용자의 머릿속에 쏙쏙 들어오도록 쉽고 친절하게 분석해줘야 합니다.\n" +
                        "대상 독자 수준: %s\n\n" +
                        "[제목]\n%s\n\n" +
                        "[원문]\n%s\n\n" +
                        "[SSOK의 필수 분석 항목 - 절대 생략 금지]\n" +
                        "1. 쏙쏙 뽑은 핵심 단어: 원문에서 어려울 만한 단어 2~3개를 선정해 뜻과 실생활 예시를 상세히 설명하세요.\n" +
                        "2. 한눈에 보는 배경지식: 원문을 이해하기 위해 꼭 필요한 맥락을 일반인 수준에서 아주 쉽게 풀어내세요.\n" +
                        "3. 생각 쏙쏙 질문: 사용자가 내용을 제대로 이해했는지 스스로 돌아보게 만드는 질문을 던지세요.\n" +
                        "4. SSOK의 응원: 학습을 마친 사용자에게 'SSOK'다운 따뜻하고 비타민 같은 격려 메시지를 남기세요.\n\n" +
                        "[출력 형식 지시사항]\n" +
                        "1. 모든 답변은 반드시 아래의 HTML 구조를 엄격히 지켜서 시작하세요.\n" +
                        "2. 전체 감싸기 태그: <div style=\"line-height: 1.65; letter-spacing: -0.02em; word-break: keep-all; color: #444;\">\n" +
                        "3. 최상단 대제목: <div> 태그가 열리자마자 [제목] 내용을 가져와 <h3 style=\"color: #111827; font-weight: bold; font-size: 24px; margin-bottom: 24px; line-height: 1.3;\">진짜 제목</h3> 형태로 가장 먼저 작성하세요.\n" +
                        "4. 각 항목의 소제목은 <h4 style=\"color: #6A82FB; font-weight: bold; font-size: 20px; margin-top: 32px; margin-bottom: 0px;\">항목 이름</h4> 형태로 작성하세요.\n" +
                        "5. 줄바꿈 및 간격 규칙:\n" +
                        "   - 소제목 태그(</h4>) 직후에는 엔터 없이 바로 본문을 시작하세요.\n" +
                        "   - 일반 문장 사이에는 줄바꿈을 하지 마세요.\n" +
                        "   - '1.', '2.', '3.' 처럼 번호가 매겨진 목록이 시작될 때만 바로 앞에 <br> 태그를 넣어 줄을 나누세요.\n" +
                        "6. 중요!! 답변에 ** (별표) 같은 마크다운 특수기호를 절대 사용하지 마세요.\n" +
                        "7. 마지막은 반드시 </div> 로 닫아주세요.",
                targetLevel, title, text
        );

        return processAiResponse(prompt, title, text, targetLevel, "TEXT");
    }

    // 2. 특정 단어 문맥 맞춤 설명 및 저장 (SSOK 페르소나 적용)
    @Transactional
    public String explainWord(String word, String contextText, String targetLevel) {
        String prompt = String.format(
                "당신은 단어의 의미를 머릿속에 쏙 넣어주는 도우미 'SSOK(쏙)'입니다.\n" +
                        "대상 독자 수준: %s\n" +
                        "[문맥]: %s\n" +
                        "[선택한 단어]: %s\n\n" +
                        "[지시사항]\n" +
                        "1. 반드시 아래의 HTML 구조와 순서대로만 대답하세요.\n" +
                        "2. 전체 감싸기: <div style=\"line-height: 1.65; letter-spacing: -0.02em; word-break: keep-all; color: #444;\">\n" +
                        "3. 대제목: <div> 태그 바로 뒤에 <h3 style=\"color: #111827; font-weight: bold; font-size: 24px; margin-bottom: 24px; line-height: 1.3;\">%s</h3>를 작성하세요.\n" +
                        "4. 섹션 1: <h4 style=\"color: #6A82FB; font-weight: bold; font-size: 20px; margin-top: 0px; margin-bottom: 0px;\">SSOK이 알려주는 단어 뜻</h4>\n" +
                        "   - 중요 </h4> 태그 직후 줄바꿈 없이 바로 비유와 함께 단어 뜻 풀이를 시작하세요. 번호(1., 2.) 목록 앞에만 <br>을 넣으세요.\n" +
                        "5. 섹션 2: <h4 style=\"color: #6A82FB; font-weight: bold; font-size: 20px; margin-top: 32px; margin-bottom: 0px;\">이 문맥에 쏙! 들어맞는 이유</h4>\n" +
                        "   - 중요 </h4> 태그 직후 줄바꿈 없이 본문을 작성하세요. 번호 목록 앞에만 <br>을 넣으세요.\n" +
                        "6. 섹션 3: <h4 style=\"color: #6A82FB; font-weight: bold; font-size: 20px; margin-top: 32px; margin-bottom: 0px;\">일상에서 쏙쏙 활용하기</h4>\n" +
                        "   - 예문 번호 앞에 <br>을 넣어 구분하세요.\n" +
                        "7. 마지막은 </div> 로 닫으세요.\n" +
                        "8. 중요!! 답변에 ** (별표) 같은 마크다운 특수기호를 절대 사용하지 마세요.",
                targetLevel, contextText, word, word
        );

        return processAiResponse(prompt, word, contextText, targetLevel, "WORD");
    }

    // AI 응답 처리 및 DB 저장 공통 로직
    private String processAiResponse(String prompt, String title, String originalText, String targetLevel, String type) {
        String result = callGeminiApi(prompt);
        result = result.replace("**", "").replace("*", "").replace("#", "");

        LearningRecord record = LearningRecord.builder()
                .type(type)
                .title(title)
                .originalText(originalText)
                .analysisResult(result)
                .targetLevel(targetLevel)
                .build();
        learningRecordRepository.save(record);

        return result;
    }

    // Gemini API 호출 로직 (기존과 동일)
    private String callGeminiApi(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> parts = new HashMap<>();
        parts.put("text", prompt);
        Map<String, Object> contents = new HashMap<>();
        contents.put("parts", List.of(parts));
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(contents));
        try {
            String urlWithKey = apiUrl + "?key=" + apiKey;
            Map<String, Object> response = restTemplate.postForObject(urlWithKey, new HttpEntity<>(requestBody, headers), Map.class);
            return extractTextFromResponse(response);
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Gemini API 호출 에러: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("SSOK 응답을 불러오는 데 실패했습니다.", e);
        }
    }

    private String extractTextFromResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            return "분석 결과를 읽어오는 중 오류가 발생했습니다.";
        }
    }
}