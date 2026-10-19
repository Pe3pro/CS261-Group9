package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "eligibility_records")
public class EligibilityRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, length = 10)
    private String studentId;

    @Column(nullable = false)
    private boolean eligible;

    @Column(nullable = false, length = 255)
    private String message;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    protected EligibilityRecord() {
    }

    public EligibilityRecord(
        String studentId,
        boolean eligible,
        String message,
        LocalDateTime checkedAt
    ) {
        this.studentId = studentId;
        this.eligible = eligible;
        this.message = message;
        this.checkedAt = checkedAt;
    }

    public Long getId() {
        return id;
    }

    public String getStudentId() {
        return studentId;
    }

    public boolean isEligible() {
        return eligible;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }
}