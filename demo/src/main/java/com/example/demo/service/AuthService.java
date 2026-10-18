package com.example.demo.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dao.StudentRepository;
import com.example.demo.dao.UserRepository;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.TuVerifyResponse;
import com.example.demo.entity.Role;
import com.example.demo.entity.Student;
import com.example.demo.entity.User;

/**
 * U1: Login ด้วยบัญชี มธ.
 * 1) ตรวจ Username / Password กับ TU REST API (ระบบไม่เก็บรหัสผ่านเอง)
 * 2) บันทึกผู้ใช้ใหม่ หรืออัปเดตชื่อ/อีเมลของผู้ใช้เดิมในตาราง users (+ students ถ้าเป็นนักศึกษา)
 * 3) ส่ง token, userId และ role กลับให้หน้าเว็บ
 */
@Service
public class AuthService {

    static final Duration TOKEN_TTL = Duration.ofHours(8);
    static final String INVALID_MESSAGE = "Username หรือ Password ไม่ถูกต้อง";

    private final TuApiClient tuApiClient;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final Clock clock;
    private final String demoPassword;
    private final Set<String> demoUsers;

    public AuthService(TuApiClient tuApiClient, UserRepository userRepository, StudentRepository studentRepository,
            Clock clock,
            @Value("${app.auth.demo-password:}") String demoPassword,
            @Value("${app.auth.demo-users:}") String demoUsers) {
        this.tuApiClient = tuApiClient;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.clock = clock;
        this.demoPassword = demoPassword == null ? "" : demoPassword;
        this.demoUsers = demoUsers == null ? Set.of()
                : Arrays.stream(demoUsers.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                        .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String username = request == null || request.username() == null ? "" : request.username().trim();
        String password = request == null || request.password() == null ? "" : request.password();
        if (username.isEmpty() || password.isEmpty()) {
            throw ApiException.badRequest("กรุณากรอก Username และ Password");
        }

        User user;
        if (isDemoLogin(username, password)) {
            user = userRepository.findByUsername(username).orElseThrow(AuthService::invalidCredentials);
            requireActive(user);
        } else {
            TuVerifyResponse tu = tuApiClient.verify(username, password);
            if (tu == null || !tu.status()) {
                throw invalidCredentials();
            }
            user = upsertUser(tu, username);
        }

        Instant expiresAt = clock.instant().plus(TOKEN_TTL);
        return new LoginResponse(UUID.randomUUID().toString(), "Bearer", expiresAt,
                user.getId(), user.getUsername(), user.getFullName(), user.getRole());
    }

    /** ผู้ใช้เดิม: ใช้ role เดิม อัปเดตชื่อ/อีเมล · ผู้ใช้ใหม่: กำหนด role จาก type ของ TU REST API */
    User upsertUser(TuVerifyResponse tu, String typedUsername) {
        String username = notBlank(tu.username()) ? tu.username().trim() : typedUsername;
        String fullName = firstNotBlank(tu.displayname_th(), tu.displayname_en(), username);
        String email = notBlank(tu.email()) ? tu.email().trim() : null;

        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            requireActive(user);
            user.setFullName(fullName);
            if (email != null) {
                user.setEmail(email);
            }
        } else {
            user = new User(username, fullName, email, toRole(tu.type()));
        }
        user = userRepository.save(user);

        if (user.getRole() == Role.STUDENT
                && studentRepository.findByUserId(user.getId()).isEmpty()
                && studentRepository.findByStudentId(username).isEmpty()) {
            studentRepository.save(new Student(user.getId(), username, tu.faculty(), tu.department(), null));
        }
        return user;
    }

    /** type ของ TU REST API: student → STUDENT, employee → STAFF (ADVISOR/ADMIN กำหนดในฐานข้อมูลเท่านั้น) */
    static Role toRole(String type) {
        if ("student".equalsIgnoreCase(type)) {
            return Role.STUDENT;
        }
        if ("employee".equalsIgnoreCase(type)) {
            return Role.STAFF;
        }
        throw new ApiException(403, "FORBIDDEN", "บัญชีประเภทนี้ยังไม่สามารถใช้งานระบบได้ กรุณาติดต่อเจ้าหน้าที่");
    }

    /** บัญชีทดลองจาก DemoDataSeeder ไม่มีในระบบของมหาวิทยาลัย จึง login ด้วยรหัสผ่านทดลอง (ตั้งค่าใน application.properties) */
    private boolean isDemoLogin(String username, String password) {
        return !demoPassword.isEmpty() && demoUsers.contains(username) && demoPassword.equals(password);
    }

    private static void requireActive(User user) {
        if (Boolean.FALSE.equals(user.getActive())) {
            throw new ApiException(403, "FORBIDDEN", "บัญชีนี้ถูกปิดการใช้งาน กรุณาติดต่อเจ้าหน้าที่");
        }
    }

    static ApiException invalidCredentials() {
        return new ApiException(401, "INVALID_CREDENTIALS", INVALID_MESSAGE);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String firstNotBlank(String... values) {
        for (String v : values) {
            if (notBlank(v)) {
                return v.trim();
            }
        }
        return "";
    }
}