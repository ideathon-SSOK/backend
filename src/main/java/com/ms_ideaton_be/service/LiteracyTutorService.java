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
                "당신은 사용자의 문해력을 길러주는 전문 튜터입니다.\n" +
                        "대상 독자 수준: %s\n\n" +
                        "[제목]\n%s\n\n" +
                        "[원문]\n%s\n\n" +
                        "[지시사항]\n" +
                        "1. 원문에서 어려울 만한 핵심 단어 2~3개를 뽑아 실생활 예시로 설명하세요.\n" +
                        "2. 필요한 맥락과 배경지식을 설명하세요.\n" +
                        "3. 스스로 이해를 유도하는 질문을 던지세요.\n" +
                        "4. 친절하게 응원하세요.\n" +
                        "5. 중요!! 답변에 ** (별표) 같은 마크다운 특수기호를 절대 사용하지 마세요.\n" +
                        "6. 시각적 강조!! 글의 전체 제목은 <h3 style=\"color: #4F46E5; font-weight: bold;\">제목</h3> 형태로, 각 문단의 핵심 소제목은 <h4 style=\"color: #10B981; font-weight: bold;\">소제목</h4> 형태의 HTML 태그로 묶어서 작성해 주세요.",
                targetLevel, title, text
        );

        String result = callGeminiApi(prompt);

        // 마크다운 잔여 기호 지우기 (HTML 태그는 그대로 살아남음)
        result = result.replace("**", "").replace("*", "").replace("#", "");

        // DB 저장 (type 추가)
        LearningRecord record = LearningRecord.builder()
                .type("TEXT") // 전체 글 분석 기록임을 명시
                .title(title)
                .originalText(text)
                .analysisResult(result)
                .targetLevel(targetLevel)
                .build();
        learningRecordRepository.save(record);

        return result;
    }

    // 2. 특정 단어 문맥 맞춤 설명
    @Transactional
    public String explainWord(String word, String contextText, String targetLevel) {
        String prompt = String.format(
                "당신은 사용자의 문해력을 길러주는 친절한 전문 튜터입니다.\n" +
                        "대상 독자 수준: %s\n" +
                        "[문맥]: %s\n" +
                        "[선택한 단어]: %s\n\n" +
                        "[지시사항]\n" +
                        "1. 반드시 아래의 HTML 구조와 순서대로만 대답해 주세요. 서론이나 결론 등 불필요한 말은 절대 하지 마세요.\n" +
                        "2. 맨 위 제목: <h3 style=\"color: #111827; font-weight: bold; margin-bottom: 16px;\">%s</h3>\n" +
                        "3. 첫 번째 섹션: <h4 style=\"color: #6A82FB; font-weight: bold; margin-bottom: 8px;\">무슨 뜻인가요?</h4>\n" +
                        "   - 내용: 사전적 정의를 피하고, 대상 독자의 눈높이에 맞춰 아주 쉬운 비유와 함께 단어의 본래 뜻을 풀이해주세요.\n" +
                        "4. 두 번째 섹션: <br><h4 style=\"color: #6A82FB; font-weight: bold; margin-bottom: 8px;\">무슨 문맥인가요?</h4>\n" +
                        "   - 내용: 주어진 [문맥] 안에서 이 단어가 어떤 역할을 하고 있는지, 전체 문장이 말하고자 하는 핵심은 무엇인지 설명해주세요.\n" +
                        "5. 세 번째 섹션: <br><h4 style=\"color: #6A82FB; font-weight: bold; margin-bottom: 8px;\">사용 예시</h4>\n" +
                        "   - 내용: 이 단어를 일상생활에서 어떻게 쓸 수 있는지 짧고 자연스러운 예문 1~2개를 만들어주세요. 예문 속 해당 단어는 따옴표로 강조해주세요.\n" +
                        "6. 중요!! 답변에 ** (별표) 같은 마크다운 특수기호를 절대 사용하지 마세요. 오직 지정된 HTML 태그와 순수 텍스트만 사용하세요.",
                targetLevel, contextText, word, word // 마지막 인자에 word가 한 번 더 들어갑니다 (제목 렌더링용)
        );

        String result = callGeminiApi(prompt);

        // 👈 [신규] 단어 분석 결과도 테이블에 똑같이 저장합니다.
        LearningRecord record = LearningRecord.builder()
                .type("WORD")          // 👈 단어 분석 기록임을 명시
                .title(word)           // 물어본 단어 이름을 title에 저장
                .originalText(contextText) // 단어가 포함되어 있던 문맥을 저장
                .analysisResult(result)    // AI가 생성한 HTML 응답 전체를 저장
                .targetLevel(targetLevel)
                .build();
        learningRecordRepository.save(record); // 👈 Repository를 통해 DB에 insert

        // 마크다운 기호 강제 제거 (안전장치)
        return result.replace("**", "").replace("*", "").replace("#", "");

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