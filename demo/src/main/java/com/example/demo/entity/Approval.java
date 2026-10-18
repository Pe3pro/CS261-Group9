package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** Entity ตาราง APPROVALS: ผลการพิจารณาคำร้องแต่ละครั้ง (เจ้าหน้าที่ 1 ครั้ง อาจารย์ 1 ครั้ง) */
@Entity
@Table(name = "approvals")
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    /** userId ของผู้พิจารณา */
    @Column(name = "approver_id", nullable = false)
    private Long approverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_level", nullable = false)
    private ApprovalLevel approvalLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalDecision decision;

    @Column(length = 1000)
    private String comment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    protected Approval() {
        // สำหรับ JPA
    }

    public Approval(Long applicationId, Long approverId, ApprovalLevel approvalLevel,
            ApprovalDecision decision, String comment, LocalDateTime decidedAt) {
        this.applicationId = applicationId;
        this.approverId = approverId;
        this.approvalLevel = approvalLevel;
        this.decision = decision;
        this.comment = comment;
        this.decidedAt = decidedAt;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getApplicationId() { return applicationId; }
    public Long getApproverId() { return approverId; }
    public ApprovalLevel getApprovalLevel() { return approvalLevel; }
    public ApprovalDecision getDecision() { return decision; }
    public String getComment() { return comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
}
