package com.ms_ideaton_be.dto;

public class AnalysisRequestDto {
    private String text;        // 분석할 원문
    private String targetLevel; // 사용자 수준 (예: "고등학생", "사회초년생")

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(String targetLevel) {
        this.targetLevel = targetLevel;
    }
}