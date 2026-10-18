package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dao.UserRepository;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.User;

/**
 * หาผู้ใช้จาก username ที่กรอกในหน้า login.html (U1)
 * ใช้เชื่อมหน้า login ของทีมกับหน้า U5 จนกว่า U1 จะมี API login + token จริง
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse findByUsername(String username) {
        String name = username == null ? "" : username.trim();
        if (name.isEmpty()) {
            throw ApiException.badRequest("กรุณาระบุ username");
        }
        User user = userRepository.findByUsername(name)
                .filter(u -> !Boolean.FALSE.equals(u.getActive()))
                .orElseThrow(() -> ApiException.notFound("ไม่พบบัญชีผู้ใช้ " + name + " ในระบบ"));
        return new UserResponse(user.getId(), user.getUsername(), user.getFullName(), user.getRole());
    }
}
