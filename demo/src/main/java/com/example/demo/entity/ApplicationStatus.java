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
    REJECTED;

    /**
     * U3: เส้นทางสถานะที่อนุญาต (ตาม state diagram ของคำร้อง)
     * DRAFT → SUBMITTED, NEED_CORRECTION → SUBMITTED, SUBMITTED → UNDER_REVIEW / NEED_CORRECTION / REJECTED,
     * UNDER_REVIEW → APPROVED / NEED_CORRECTION / REJECTED · APPROVED และ REJECTED เป็นสถานะสุดท้าย
     */
    public boolean canMoveTo(ApplicationStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case DRAFT, NEED_CORRECTION -> target == SUBMITTED;
            case SUBMITTED -> target == UNDER_REVIEW || target == NEED_CORRECTION || target == REJECTED;
            case UNDER_REVIEW -> target == APPROVED || target == NEED_CORRECTION || target == REJECTED;
            case APPROVED, REJECTED -> false;
        };
    }

    /** U3: นักศึกษายังแก้ไขคำตอบ / แนบไฟล์ / ส่งได้ */
    public boolean isEditableByStudent() {
        return this == DRAFT || this == NEED_CORRECTION;
    }

    /** U3: คำร้องที่ยังไม่จบ (ห้ามสร้างฉบับที่สอง) · REJECTED เท่านั้นที่ยื่นใหม่ได้ */
    public boolean blocksNewRequest() {
        return this != REJECTED;
    }
}
