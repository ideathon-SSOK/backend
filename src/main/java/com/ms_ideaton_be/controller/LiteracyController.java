package com.ms_ideaton_be.controller;

import com.ms_ideaton_be.dto.AnalysisRequestDto;
import com.ms_ideaton_be.dto.WordExplanationRequestDto;
import com.ms_ideaton_be.service.LiteracyTutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/literacy")
@RequiredArgsConstructor
public class LiteracyController {

    private final LiteracyTutorService literacyTutorService;

    // 1. 전체 텍스트 분석 API
    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeText(@RequestBody AnalysisRequestDto requestDto) {
        String analysisResult = literacyTutorService.analyzeAndSave(
                requestDto.getTitle(), // 새로 추가된 파라미터
                requestDto.getText(),
                requestDto.getTargetLevel()
        );
        return ResponseEntity.ok(analysisResult);
    }

    // 2. [신규] 특정 단어 문맥 기반 설명 API
    @PostMapping("/explain-word")
    public ResponseEntity<String> explainWord(@RequestBody WordExplanationRequestDto requestDto) {
        String wordExplanation = literacyTutorService.explainWord(
                requestDto.getWord(),
                requestDto.getContextText(),
                requestDto.getTargetLevel()
        );
        return ResponseEntity.ok(wordExplanation);
    }
}