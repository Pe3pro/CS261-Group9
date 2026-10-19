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

/** Entity ตาราง FORM_TEMPLATES: แบบฟอร์มคำร้องที่เจ้าหน้าที่สร้าง (U5) และนักศึกษาใช้ยื่น (U3) */
@Entity
@Table(name = "form_templates")
public class FormTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormStatus status;

    /** วันเวลาหมดเขตยื่นคำร้อง */
    private LocalDateTime deadline;

    /** userId ของเจ้าหน้าที่ที่สร้าง */
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    protected FormTemplate() {
        // สำหรับ JPA
    }

    public FormTemplate(String name, Integer version, Long createdBy) {
        this.name = name;
        this.version = version;
        this.createdBy = createdBy;
        this.status = FormStatus.DRAFT;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    /** เปิดใช้งานแบบฟอร์ม ให้นักศึกษาใช้ยื่นได้ */
    public void publish(LocalDateTime now) {
        this.status = FormStatus.PUBLISHED;
        this.publishedAt = now;
    }

    /** เลิกใช้แบบฟอร์ม (เมื่อมีฉบับใหม่เปิดใช้งาน) */
    public void archive() {
        this.status = FormStatus.ARCHIVED;
    }

    public boolean isDraft() {
        return status == FormStatus.DRAFT;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getVersion() { return version; }
    public FormStatus getStatus() { return status; }
    public void setStatus(FormStatus status) { this.status = status; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
}
