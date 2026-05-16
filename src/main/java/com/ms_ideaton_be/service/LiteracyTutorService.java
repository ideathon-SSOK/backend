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
    public String analyzeAndSave(String title, String text, String targetLevel) {
        String prompt = String.format(
                "당신은 일반 국민 누구나 글을 쉽게 이해할 수 있도록 돕는 전문 튜터입니다.\n" +
                        "절대로 글을 단순히 한 줄로 요약해서 정답만 주지 마세요.\n" +
                        "대상 독자 수준: %s\n\n" +
                        "[제목]\n%s\n\n" +
                        "[원문]\n%s\n\n" +
                        "[지시사항]\n" +
                        "1. 원문에서 일반 국민이 어려워할 만한 핵심 단어 2~3개를 뽑아 실생활 예시로 설명하세요.\n" +
                        "2. 필요한 맥락과 배경지식을 아주 쉽게 설명하세요.\n" +
                        "3. 스스로 이해를 유도하는 질문을 던지세요.\n" +
                        "4. 친절하게 응원하세요.\n" +
                        "5. 중요!! 답변에 ** (별표) 같은 마크다운 특수기호를 절대 사용하지 마세요.\n" +
                        // 단어 분석과 완벽하게 통일된 디자인 적용 (색상, 여백 모두 동일하게 맞춤)
                        "6. 시각적 강조!! 글의 전체 제목은 <h3 style=\"color: #111827; font-weight: bold; font-size: 24px; margin-bottom: 16px;\">제목</h3> 형태로, 각 문단의 핵심 소제목은 <br><h4 style=\"color: #6A82FB; font-weight: bold; font-size: 20px; margin-bottom: 8px;\">소제목</h4> 형태의 HTML 태그로 묶어서 작성해 주세요. 본문 내용은 태그 없이 일반 텍스트로 작성하세요.",
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

    // 2. 특정 단어 문맥 맞춤 설명 및 저장
    @Transactional
    public String explainWord(String word, String contextText, String targetLevel) {
        String prompt = String.format(
                "당신은 일반 국민 누구나 글을 쉽게 이해할 수 있도록 돕는 친절한 전문 튜터입니다.\n" +
                        "대상 독자 수준: %s\n" +
                        "[문맥]: %s\n" +
                        "[선택한 단어]: %s\n\n" +
                        "[지시사항]\n" +
                        "1. 반드시 아래의 HTML 구조와 순서대로만 대답해 주세요. 서론이나 결론 등 불필요한 말은 절대 하지 마세요.\n" +
                        // font-size 속성 추가 (큰 제목 24px)
                        "2. 맨 위 제목: <h3 style=\"color: #111827; font-weight: bold; font-size: 24px; margin-bottom: 16px;\">%s</h3>\n" +
                        // font-size 속성 추가 (소제목 20px)
                        "3. 첫 번째 섹션: <h4 style=\"color: #6A82FB; font-weight: bold; font-size: 20px; margin-bottom: 8px;\">이 단어, 여기선 이런 뜻이에요</h4>\n" +
                        "   - 내용: 누구나 바로 이해할 수 있도록, 사전적 정의를 피하고 아주 쉬운 비유와 함께 단어의 본래 뜻을 풀이해주세요.\n" +
                        "4. 두 번째 섹션: <br><h4 style=\"color: #6A82FB; font-weight: bold; font-size: 20px; margin-bottom: 8px;\">왜 이 말이 여기 쓰였을까요?</h4>\n" +
                        "   - 내용: 주어진 [문맥] 안에서 이 단어가 어떤 역할을 하고 있는지, 전체 문장이 말하고자 하는 핵심은 무엇인지 설명해주세요.\n" +
                        "5. 세 번째 섹션: <br><h4 style=\"color: #6A82FB; font-weight: bold; font-size: 20px; margin-bottom: 8px;\">실제로는 이렇게 쓰여요 !</h4>\n" +
                        "   - 내용: 이 단어를 일상생활에서 어떻게 쓸 수 있는지 짧고 자연스러운 예문 1~2개를 만들어주세요. 예문 속 해당 단어는 따옴표로 강조해주세요.\n" +
                        "6. 중요!! 답변에 ** (별표) 같은 마크다운 특수기호를 절대 사용하지 마세요. 오직 지정된 HTML 태그와 순수 텍스트만 사용하세요.",
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