package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.entity.ApplicationStatus;

/** ส่งรายละเอียดคำร้องทั้งหมดสำหรับหน้าตรวจ (GET /api/applications/{id}/detail) */
public record ApplicationDetailResponse(Long id, String studentId, String studentName, String email, String faculty,
        String major, Double gpa, String formName, ApplicationStatus status, LocalDateTime submittedAt,
        List<AnswerResponse> answers, List<DocumentResponse> documents, List<ApprovalResponse> approvals) {
}
