package com.ms_ideaton_be.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // /api/로 시작하는 모든 엔드포인트에 대해 CORS 적용
                .allowedOrigins(
                        "http://localhost:3000",   // 프론트엔드 로컬 개발 환경 (React/Next.js 기본 포트)
                        "http://localhost:8080",   // (필요 시) Vue 등 기타 환경
                        "https://ideathon-ssok.netlify.app" // 프론트엔드 배포 링크
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 허용할 HTTP 메서드
                .allowedHeaders("*") // 모든 헤더 허용
                .allowCredentials(true) // 쿠키나 인증 정보(세션 등)를 포함한 요청 허용
                .maxAge(3600); // Preflight 요청의 결과를 3600초(1시간) 동안 캐시
    }
}