package com.example.demo.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Approval;

/** DAO ของตาราง APPROVALS */
public interface ApprovalRepository extends JpaRepository<Approval, Long> {

    /** ประวัติการพิจารณาของคำร้อง เรียงตามลำดับที่บันทึก (เก่าไปใหม่) */
    List<Approval> findByApplicationIdOrderByIdAsc(Long applicationId);
}
