package com.example.demo.dto;

import java.time.LocalDateTime;

/**
 * U3: GET /api/forms/published · แบบฟอร์มที่เปิดให้ยื่น พร้อมเวลาของ server
 * deadlinePassed คำนวณจากเวลา server ไม่ใช่เวลาเครื่องนักศึกษา
 */
public record PublishedFormResponse(FormTemplateResponse form, LocalDateTime serverTime, boolean deadlinePassed) {
}
