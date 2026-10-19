package com.example.demo.dto;

import com.example.demo.entity.Role;

/** ข้อมูลผู้ใช้ที่หน้าเว็บต้องรู้หลัง login (id ใช้ส่งใน header X-User-Id, role ใช้เลือกเมนู) */
public record UserResponse(Long id, String username, String name, Role role) {
}
