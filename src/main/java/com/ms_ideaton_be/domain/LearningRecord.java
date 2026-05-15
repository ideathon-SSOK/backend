package com.ms_ideaton_be.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LearningRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;          // 글 제목 저장 컬럼

    @Column(columnDefinition = "TEXT")
    private String originalText;   // 사용자가 입력한 원문

    @Column(columnDefinition = "TEXT")
    private String analysisResult; // AI가 분석해준 내용

    private String targetLevel;    // 초/중/고/사회초년생 등 수준

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}