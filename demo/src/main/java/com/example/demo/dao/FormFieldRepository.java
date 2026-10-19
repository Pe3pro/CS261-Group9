package com.example.demo.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.FormField;

/** DAO ของตาราง FORM_FIELDS */
public interface FormFieldRepository extends JpaRepository<FormField, Long> {

    /** ช่องทั้งหมดของแบบฟอร์ม เรียงตามลำดับที่แสดง */
    List<FormField> findByFormTemplateIdOrderByDisplayOrderAsc(Long formTemplateId);

    /** ลบช่องเดิมทั้งหมดก่อนบันทึกชุดใหม่ (ตอนแก้ไขแบบฟอร์มฉบับร่าง) */
    void deleteByFormTemplateId(Long formTemplateId);

    long countByFormTemplateId(Long formTemplateId);
}
