package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.entity.FormStatus;
import com.example.demo.entity.FormTemplate;

/** ส่งข้อมูลแบบฟอร์มพร้อมช่องทั้งหมด */
public record FormTemplateResponse(Long id, String name, String description, Integer version, FormStatus status,
        LocalDateTime deadline, LocalDateTime createdAt, LocalDateTime publishedAt, List<FormFieldResponse> fields) {

    public static FormTemplateResponse from(FormTemplate t, List<FormFieldResponse> fields) {
        return new FormTemplateResponse(t.getId(), t.getName(), t.getDescription(), t.getVersion(), t.getStatus(),
                t.getDeadline(), t.getCreatedAt(), t.getPublishedAt(), fields);
    }
}
