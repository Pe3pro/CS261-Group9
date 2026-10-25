package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Company;
import com.example.demo.entity.CompanyStatus;

/** DAO ของตาราง COMPANIES */

public interface CompanyRepository extends JpaRepository<Company, Long> {

    /** ดึงบริษัทตามสถานะ เช่น OPEN = บริษัทที่เปิดรับ (GET /api/companies) */
    List<Company> findByStatus(CompanyStatus status);

    /** ค้นหาบริษัทจากบางส่วนของชื่อ ไม่สนตัวพิมพ์เล็ก/ใหญ่ */
    List<Company> findByCompanyNameContainingIgnoreCase(String keyword);
}
