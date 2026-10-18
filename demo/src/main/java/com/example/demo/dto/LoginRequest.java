package com.example.demo.dto;

/** U1: ข้อมูลที่หน้า login.html ส่งมา (body ของ POST /api/auth/login) */
public record LoginRequest(String username, String password) {

    /** ไม่แสดงรหัสผ่านใน log หรือข้อความ error */
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=***]";
    }
}