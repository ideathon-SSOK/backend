package com.ms_ideaton_be.controller;

import com.ms_ideaton_be.dto.AnalysisRequestDto;
import com.ms_ideaton_be.service.LiteracyTutorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/literacy")
public class LiteracyController {

    private final LiteracyTutorService literacyTutorService;

    public LiteracyController(LiteracyTutorService literacyTutorService) {
        this.literacyTutorService = literacyTutorService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeText(@RequestBody AnalysisRequestDto requestDto) {
        // 기존 analyzeTextProcess에서 analyzeAndSave로 메서드명 변경
        String analysisResult = literacyTutorService.analyzeAndSave(
                requestDto.getText(),
                requestDto.getTargetLevel()
        );
        return ResponseEntity.ok(analysisResult);
    }
}