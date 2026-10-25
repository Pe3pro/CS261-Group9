package com.example.demo.dto;

import com.example.demo.entity.ApprovalDecision;

/** รับผลการพิจารณา: APPROVE / RETURN / REJECT (ถ้าไม่ใช่ APPROVE ต้องมี comment) */
public record ApprovalRequest(ApprovalDecision decision, String comment) {
}
