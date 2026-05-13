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

    // 1. 전체 글 분석 및 저장
    @Transactional
    public String analyzeAndSave(String text, String targetLevel) {
        String prompt = String.format(
                "당신은 사용자의 문해력을 길러주는 전문 튜터입니다.\n" +
                        "절대로 글을 단순히 한 줄로 요약해서 정답만 주지 마세요.\n" +
                        "대상 독자 수준: %s\n\n" +
                        "[원문]\n%s\n\n" +
                        "[지시사항]\n" +
                        "1. 원문에서 어려울 만한 핵심 단어 2~3개를 뽑아 실생활 예시로 설명하세요.\n" +
                        "2. 필요한 맥락과 배경지식을 설명하세요.\n" +
                        "3. 스스로 이해를 유도하는 질문을 던지세요.\n" +
                        "4. 친절하게 응원하세요.",
                targetLevel, text
        );

        String result = callGeminiApi(prompt);

        // DB에 전체 글 분석 기록 저장
        LearningRecord record = LearningRecord.builder()
                .originalText(text)
                .analysisResult(result)
                .targetLevel(targetLevel)
                .build();
        learningRecordRepository.save(record);

        return result;
    }

    // 2. [신규 기능] 특정 단어 문맥 맞춤 설명
    public String explainWord(String word, String contextText, String targetLevel) {
        String prompt = String.format(
                "당신은 사용자의 문해력을 길러주는 친절한 전문 튜터입니다.\n" +
                        "대상 독자 수준: %s\n" +
                        "[문맥]: %s\n" +
                        "[선택한 단어]: %s\n\n" +
                        "[지시사항]\n" +
                        "1. 사전적 정의를 딱딱하게 읊지 마세요.\n" +
                        "2. 주어진 [문맥] 안에서 저 단어가 어떤 의미로 쓰였는지 대상 독자의 눈높이에 맞춰 아주 쉽게 설명해주세요.\n" +
                        "3. 이해를 돕기 위해 실생활과 밀접한 짧은 예시를 하나 들어주세요.",
                targetLevel, contextText, word
        );

        return callGeminiApi(prompt);
    }

    // 공통 Gemini API 호출 로직
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

    // JSON 응답 파싱 로직
    @SuppressWarnings("unchecked")
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