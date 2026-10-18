package com.example.demo.service;

import java.util.Arrays;

import org.springframework.stereotype.Component;

import com.example.demo.dao.UserRepository;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;

/**
 * ตรวจว่าผู้เรียก API คือใครและมีสิทธิ์ไหม
 * ตอนนี้รับ userId จาก header X-User-Id ไปก่อน เมื่อ U1 (login) เสร็จให้เปลี่ยนมาอ่านจาก token แทน
 */
@Component
public class AccessGuard {

    private final UserRepository userRepository;

    public AccessGuard(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** คืน User ที่เรียก API ถ้าบทบาทอยู่ใน allowed (ไม่ส่ง allowed = ทุกบทบาท) */
    public User require(Long userId, Role... allowed) {
        if (userId == null) {
            throw ApiException.unauthorized();
        }
        User user = userRepository.findById(userId).orElseThrow(ApiException::unauthorized);
        if (Boolean.FALSE.equals(user.getActive())) {
            throw ApiException.unauthorized();
        }
        if (allowed.length > 0 && Arrays.stream(allowed).noneMatch(r -> r == user.getRole())) {
            throw ApiException.forbidden();
        }
        return user;
    }
}
