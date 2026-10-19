package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Entity ตาราง FORM_FIELDS: ช่องแต่ละช่องในแบบฟอร์ม */
@Entity
@Table(name = "form_fields")
public class FormField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "form_template_id", nullable = false)
    private Long formTemplateId;

    /** ชื่อช่องสำหรับระบบ เช่น field_1 */
    @Column(name = "field_name", nullable = false)
    private String fieldName;

    /** ข้อความที่นักศึกษาเห็น เช่น "เบอร์โทรศัพท์" */
    @Column(nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false)
    private FieldType fieldType;

    @Column(nullable = false)
    private Boolean required;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    /** ตัวเลือกของช่อง SELECT คั่นด้วยบรรทัดใหม่ */
    @Column(length = 2000)
    private String options;

    protected FormField() {
        // สำหรับ JPA
    }

    public FormField(Long formTemplateId, String label, FieldType fieldType, Boolean required, Integer displayOrder) {
        this.formTemplateId = formTemplateId;
        this.label = label;
        this.fieldType = fieldType;
        this.required = required;
        this.displayOrder = displayOrder;
        this.fieldName = "field_" + displayOrder;
    }

    public Long getId() { return id; }
    public Long getFormTemplateId() { return formTemplateId; }
    public String getFieldName() { return fieldName; }
    public String getLabel() { return label; }
    public FieldType getFieldType() { return fieldType; }
    public Boolean getRequired() { return required; }
    public Integer getDisplayOrder() { return displayOrder; }
    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }
}
