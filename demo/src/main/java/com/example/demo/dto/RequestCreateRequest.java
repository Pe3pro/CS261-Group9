package com.example.demo.dto;

/** U3: POST /api/requests · ไม่ส่ง formTemplateId = ใช้แบบฟอร์มที่เผยแพร่ล่าสุด */
public record RequestCreateRequest(Long formTemplateId) {
}
