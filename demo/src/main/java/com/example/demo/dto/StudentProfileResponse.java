package com.example.demo.dto;

/** U3: ข้อมูลนักศึกษาจากฐานข้อมูล ใช้เติมหน้ายื่นคำร้อง (รหัส คณะ สาขา เกรดเฉลี่ย แก้ไขไม่ได้) */
public record StudentProfileResponse(String studentId, String fullName, String email, String faculty, String major,
        Double gpa) {
}
