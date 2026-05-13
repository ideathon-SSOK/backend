package com.ms_ideaton_be.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WordExplanationRequestDto {
    private String word;        // 사용자가 클릭한 단어
    private String contextText; // 단어가 포함된 원문 (문맥 파악용)
    private String targetLevel; // 사용자 수준 (예: "고등학생", "사회초년생")
}