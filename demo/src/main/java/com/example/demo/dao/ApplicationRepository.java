package com.example.demo.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Application;
import com.example.demo.entity.ApplicationStatus;

/** DAO ของตาราง APPLICATIONS (ใช้ร่วมกับ U3) */
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    /** U5: คำร้องตามสถานะ เรียงตามเวลายื่น (ยื่นก่อนได้ตรวจก่อน) */
    List<Application> findByStatusOrderBySubmittedAtAsc(ApplicationStatus status);

    /** U3: คำร้องทั้งหมดของนักศึกษา */
    List<Application> findByStudentId(Long studentId);

    long countByStatus(ApplicationStatus status);

    /** U3: คำร้องของนักศึกษา ฉบับล่าสุดก่อน (GET /api/requests/me) */
    List<Application> findByStudentIdOrderByIdDesc(Long studentId);
}
