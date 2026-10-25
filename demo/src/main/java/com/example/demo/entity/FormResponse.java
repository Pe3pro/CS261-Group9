package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** Entity ตาราง FORM_RESPONSES: คำตอบของนักศึกษาในแต่ละช่องของแบบฟอร์ม (ใช้ร่วมกับ U3) */
@Entity
@Table(name = "form_responses")
public class FormResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "form_field_id", nullable = false)
    private Long formFieldId;

    @Column(name = "field_value", length = 2000)
    private String value;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected FormResponse() {
        // สำหรับ JPA
    }

    public FormResponse(Long applicationId, Long formFieldId, String value) {
        this.applicationId = applicationId;
        this.formFieldId = formFieldId;
        this.value = value;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getApplicationId() { return applicationId; }
    public Long getFormFieldId() { return formFieldId; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
