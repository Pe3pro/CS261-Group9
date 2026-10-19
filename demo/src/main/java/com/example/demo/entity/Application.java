package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Entity ตาราง APPLICATIONS: คำร้องขอฝึกสหกิจ (U3 สร้าง/ยื่น, U5 รับและพิจารณา) */
@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** อ้างอิง STUDENTS.id */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /** U3: ใช้สร้าง Foreign Key applications.student_id → students.id เท่านั้น (อ่านอย่างเดียว ไม่ใช้บันทึก) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_applications_student"))
    private Student student;

    @Column(name = "form_template_id", nullable = false)
    private Long formTemplateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** กันการบันทึกทับกัน: ถ้าสองคนตัดสินคำร้องเดียวกันพร้อมกัน คนที่สองจะได้ OptimisticLockException */
    @Version
    private Long version;

    protected Application() {
        // สำหรับ JPA
    }

    public Application(Long studentId, Long formTemplateId, ApplicationStatus status) {
        this.studentId = studentId;
        this.formTemplateId = formTemplateId;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void submit(LocalDateTime now) {
        this.status = ApplicationStatus.SUBMITTED;
        this.submittedAt = now;
    }

    /** U3: บันทึกว่าคำร้องถูกแก้ไข (เช่น แก้คำตอบในแบบฟอร์ม) */
    public void markUpdated(LocalDateTime now) {
        this.updatedAt = now;
    }

    public void changeStatus(ApplicationStatus newStatus) {
        this.status = newStatus;
    }

    public Long getId() { return id; }
    public Long getStudentId() { return studentId; }
    public Long getFormTemplateId() { return formTemplateId; }
    public ApplicationStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
