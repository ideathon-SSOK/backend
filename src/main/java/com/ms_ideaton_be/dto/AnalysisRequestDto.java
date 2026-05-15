package com.ms_ideaton_be.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnalysisRequestDto {
    private String title;       // 글 제목
    private String text;        // 분석할 원문
    private String targetLevel; // 사용자 수준 (예: "고등학생", "사회초년생")
}