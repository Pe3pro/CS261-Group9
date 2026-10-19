package com.example.demo.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.FormStatus;
import com.example.demo.entity.FormTemplate;

/** DAO ของตาราง FORM_TEMPLATES */
public interface FormTemplateRepository extends JpaRepository<FormTemplate, Long> {

    /** แบบฟอร์มทั้งหมด เรียงจากฉบับล่าสุด (หน้ารายการแบบฟอร์มของเจ้าหน้าที่) */
    List<FormTemplate> findAllByOrderByVersionDesc();

    /** แบบฟอร์มตามสถานะ เช่น PUBLISHED = ฉบับที่นักศึกษาใช้ยื่นอยู่ */
    List<FormTemplate> findByStatus(FormStatus status);

    Optional<FormTemplate> findByVersion(Integer version);

    /** ฉบับที่ version สูงสุด ใช้คำนวณ version ถัดไป */
    Optional<FormTemplate> findTopByOrderByVersionDesc();
}
