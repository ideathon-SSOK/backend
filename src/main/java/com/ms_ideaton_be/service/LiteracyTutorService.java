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

    @Transactional
    public String analyzeAndSave(String title, String text, String targetLevel) {
        String prompt = String.format(
                "당신은 일반 국민 누구나 글을 쉽게 이해할 수 있도록 돕는 전문 튜터입니다.\n" +
                        "대상 독자 수준: %s\n\n" +
                        "[제목]\n%s\n\n" +
                        "[원문]\n%s\n\n" +
                        "[지시사항]\n" +
                        "1. 원문에서 어려울 만한 핵심 단어 2~3개를 뽑아 실생활 예시로 설명하세요.\n" +
                        "2. 필요한 맥락과 배경지식을 아주 쉽게 설명하세요.\n" +
                        "3. 스스로 이해를 유도하는 질문을 던지세요.\n" +
                        "4. 친절하게 응원하세요.\n" +
                        "5. 중요!! 답변에 ** (별표) 같은 마크다운 특수기호를 절대 사용하지 마세요.\n" +
                        "6. 답변 전체는 반드시 다음 태그로 감싸주세요: <div style=\"line-height: 1.65; letter-spacing: -0.02em; word-break: keep-all; color: #444;\">\n" +
                        "7. 전체 제목은 상단 [제목]을 가져와 <h3 style=\"color: #111827; font-weight: bold; font-size: 24px; margin-bottom: 24px; line-height: 1.3;\">내용</h3>으로 작성하세요.\n" +
                        "8. 소제목은 <h4 style=\"color: #6A82FB; font-weight: bold; font-size: 20px; margin-top: 32px; margin-bottom: 0px;\">내용</h4>으로 작성하세요.\n" +
                        "9. 줄바꿈 규칙: 일반적인 문장 사이에는 줄바꿈을 하지 마세요. 오직 '1.', '2.', '3.' 처럼 번호가 매겨진 목록이 시작될 때만 바로 앞에 <br> 태그를 하나 넣어 줄을 나누세요.\n" +
                        "10. 소제목 태그(</h4>) 직후에는 엔터 없이 바로 본문을 시작하세요.\n" +
                        "11. 마지막은 </div> 로 닫으세요.",
                targetLevel, title, text
        );

        String result = callGeminiApi(prompt);
        result = result.replace("**", "").replace("*", "").replace("#", "");

        LearningRecord record = LearningRecord.builder()
                .type("TEXT")
                .title(title)
                .originalText(text)
                .analysisResult(result)
                .targetLevel(targetLevel)
                .build();
        learningRecordRepository.save(record);

        return result;
    }

    @Transactional
    public String explainWord(String word, String contextText, String targetLevel) {
        String prompt = String.format(
                "당신은 일반 국민 누구나 글을 쉽게 이해할 수 있도록 돕는 친절한 전문 튜터입니다.\n" +
                        "대상 독자 수준: %s\n" +
                        "[문맥]: %s\n" +
                        "[선택한 단어]: %s\n\n" +
                        "[지시사항]\n" +
                        "1. 반드시 아래의 HTML 구조와 순서대로만 대답하세요.\n" +
                        "2. 전체 감싸기: <div style=\"line-height: 1.65; letter-spacing: -0.02em; word-break: keep-all; color: #444;\">\n" +
                        "3. 제목: <h3 style=\"color: #111827; font-weight: bold; font-size: 24px; margin-bottom: 24px; line-height: 1.3;\">%s</h3>\n" +
                        "4. 섹션 1: <h4 style=\"color: #6A82FB; font-weight: bold; font-size: 20px; margin-top: 0px; margin-bottom: 0px;\">이 단어, 여기선 이런 뜻이에요</h4>\n" +
                        "   - 설명글은 줄바꿈 없이 이어서 쓰되, 예시 문장 등에 번호(1., 2.)가 붙는다면 그 앞에만 <br>을 넣으세요.\n" +
                        "5. 섹션 2: <h4 style=\"color: #6A82FB; font-weight: bold; font-size: 20px; margin-top: 32px; margin-bottom: 0px;\">왜 이 말이 여기 쓰였을까요?</h4>\n" +
                        "   - 줄바꿈 규칙: 번호가 붙은 목록이 시작될 때만 앞에 <br>을 넣어 줄을 나누세요.\n" +
                        "6. 섹션 3: <h4 style=\"color: #6A82FB; font-weight: bold; font-size: 20px; margin-top: 32px; margin-bottom: 0px;\">실제로는 이렇게 쓰여요 !</h4>\n" +
                        "   - 예문들 앞에 번호가 있다면 번호마다 <br>을 넣어 구분하세요.\n" +
                        "7. 마지막은 </div> 로 닫으세요.\n" +
                        "8. 중요!! 답변에 ** (별표) 같은 마크다운 특수기호를 절대 사용하지 마세요.",
                targetLevel, contextText, word, word
        );

        String result = callGeminiApi(prompt);
        result = result.replace("**", "").replace("*", "").replace("#", "");

        LearningRecord record = LearningRecord.builder()
                .type("WORD")
                .title(word)
                .originalText(contextText)
                .analysisResult(result)
                .targetLevel(targetLevel)
                .build();
        learningRecordRepository.save(record);

        return result;
    }

    // 공통 Gemini API 호출 및 파싱 로직 (기존과 동일)
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
            throw new RuntimeException("AI 튜터 응답을 불러오는 데 실패했습니다.", e);
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