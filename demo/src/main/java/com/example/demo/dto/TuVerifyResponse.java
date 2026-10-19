package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * U1: ผลจาก TU REST API (POST /api/v1/auth/Ad/verify) ใช้ภายใน Backend เท่านั้น ไม่ส่งออกไปที่หน้าเว็บ
 * ชื่อ field ตรงกับ JSON ของมหาวิทยาลัย (displayname_th ฯลฯ)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TuVerifyResponse(
        boolean status,
        String message,
        String type,
        String username,
        String displayname_th,
        String displayname_en,
        String email,
        String department,
        String faculty) {

    /** ใช้แทนกรณีที่ TU REST API ตอบว่า username หรือ password ไม่ถูกต้อง */
    public static TuVerifyResponse invalid(String message) {
        return new TuVerifyResponse(false, message, null, null, null, null, null, null, null);
    }
}