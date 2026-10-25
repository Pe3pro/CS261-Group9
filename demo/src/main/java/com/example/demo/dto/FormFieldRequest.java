package com.example.demo.dto;

import com.example.demo.entity.FieldType;

/** รับข้อมูลช่อง 1 ช่องตอนสร้าง/แก้ไขแบบฟอร์ม (options ใช้กับ SELECT คั่นด้วยบรรทัดใหม่) */
public record FormFieldRequest(String label, FieldType fieldType, Boolean required, String options) {
}
