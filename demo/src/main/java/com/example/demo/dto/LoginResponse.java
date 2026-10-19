package com.example.demo.dto;

import java.time.Instant;

import com.example.demo.entity.Role;

/**
 * U1: ผล login สำเร็จ
 * userId ใช้ส่งเป็น header X-User-Id ให้ API ของ U3/U5 (AccessGuard) จนกว่าจะเปลี่ยนไปตรวจจาก accessToken
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        Long userId,
        String username,
        String fullName,
        Role role) {
}