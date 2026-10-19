package com.example.demo.dto;

/** รูปแบบ error ที่ทุก API ส่งกลับ เช่น {"code":"NOT_FOUND","message":"ไม่พบคำร้อง"} */
public record ErrorResponse(String code, String message) {
}
