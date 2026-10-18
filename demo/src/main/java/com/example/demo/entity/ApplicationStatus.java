package com.example.demo.entity;

/** สถานะคำร้อง (ใช้ร่วมกับ U3)
 * SUBMITTED → (เจ้าหน้าที่อนุมัติ) UNDER_REVIEW → (อาจารย์อนุมัติ) APPROVED
 * ส่งกลับแก้ไข → NEED_CORRECTION, ปฏิเสธ → REJECTED */
public enum ApplicationStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    NEED_CORRECTION,
    APPROVED,
    REJECTED
}
