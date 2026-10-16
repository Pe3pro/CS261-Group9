package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.CompanyPosition;

/** DAO ของตาราง COMPANY_POSITIONS */

public interface CompanyPositionRepository extends JpaRepository<CompanyPosition, Long> {

    /** ดึงทุกตำแหน่งของบริษัท */
    List<CompanyPosition> findByCompanyId(Long companyId);

    /** ดึงเฉพาะตำแหน่งที่เปิดรับและยังมีที่ว่าง (GET /api/companies/{id}/positions) */
    @Query("""
            SELECT p FROM CompanyPosition p
            WHERE p.companyId = :companyId
              AND p.status = com.example.demo.entity.PositionStatus.OPEN
              AND p.availableSlots > 0
            ORDER BY p.positionName
            """)
    List<CompanyPosition> findOpenPositions(@Param("companyId") Long companyId);
}
