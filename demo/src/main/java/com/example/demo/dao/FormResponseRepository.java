package com.example.demo.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.FormResponse;

/** DAO ของตาราง FORM_RESPONSES (ใช้ร่วมกับ U3) */
public interface FormResponseRepository extends JpaRepository<FormResponse, Long> {

    List<FormResponse> findByApplicationId(Long applicationId);

    /** U3: คำตอบของช่องหนึ่งในคำร้อง ใช้แก้ไขค่าเดิมแทนการเพิ่มแถวซ้ำ */
    Optional<FormResponse> findByApplicationIdAndFormFieldId(Long applicationId, Long formFieldId);
}
