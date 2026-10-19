package com.example.demo.dao;

import com.example.demo.entity.EligibilityRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EligibilityRecordDao
        extends JpaRepository<EligibilityRecord, Long> {

    List<EligibilityRecord> findByStudentIdOrderByCheckedAtDesc(
        String studentId
    );
}