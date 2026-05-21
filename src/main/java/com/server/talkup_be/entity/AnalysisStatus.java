package com.server.talkup_be.entity;

public enum AnalysisStatus {
    PENDING,   // AI 분석 대기 중
    COMPLETED, // AI 분석 완료
    FAILED     // AI 분석 실패 (에러)
}