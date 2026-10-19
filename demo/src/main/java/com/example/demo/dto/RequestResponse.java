package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.example.demo.entity.ApplicationStatus;

/**
 * U3: ข้อมูลคำร้องที่ส่งให้นักศึกษา
 * answers = { fieldName: ค่า } · correctionReason = เหตุผลล่าสุดที่เจ้าหน้าที่ส่งกลับให้แก้ (ถ้ามี)
 */
public record RequestResponse(Long id, String studentId, Long formTemplateId, String formName,
        ApplicationStatus status, LocalDateTime createdAt, LocalDateTime submittedAt, LocalDateTime updatedAt,
        LocalDateTime deadline, Map<String, String> answers, List<DocumentResponse> documents,
        String correctionReason) {
}
