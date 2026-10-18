package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

/** รับข้อมูลแบบฟอร์มตอนสร้าง (POST /api/forms) หรือแก้ไข (PUT /api/forms/{id}) */
public record FormTemplateRequest(String name, String description, LocalDateTime deadline, List<FormFieldRequest> fields) {
}
