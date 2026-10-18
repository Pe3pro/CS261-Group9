package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.entity.ApplicationStatus;
import com.example.demo.entity.ApprovalDecision;
import com.example.demo.entity.ApprovalLevel;

/** ส่งผลการพิจารณา และสถานะคำร้องล่าสุด */
public record ApprovalResponse(Long id, Long applicationId, ApprovalLevel approvalLevel, ApprovalDecision decision,
        String approverName, String comment, LocalDateTime decidedAt, ApplicationStatus applicationStatus) {
}
