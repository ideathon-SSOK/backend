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

    private String type;           // [신규] "TEXT"(전체글 분석) 또는 "WORD"(단어 분석) 구분 컬럼

    private String title;          // 글 제목 또는 찾은 단어 이름

    @Column(columnDefinition = "TEXT")
    private String originalText;   // 입력한 원문 또는 단어가 포함되어 있던 문맥(Context)

    @Column(columnDefinition = "TEXT")
    private String analysisResult; // AI의 분석 결과 (HTML 태그 포함)

    private String targetLevel;    // 사용자 맞춤 수준

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}