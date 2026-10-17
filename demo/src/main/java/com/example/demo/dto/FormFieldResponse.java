package com.example.demo.dto;

import com.example.demo.entity.FieldType;
import com.example.demo.entity.FormField;

/** ส่งข้อมูลช่องในแบบฟอร์ม */
public record FormFieldResponse(Long id, String fieldName, String label, FieldType fieldType, Boolean required,
        Integer displayOrder, String options) {

    public static FormFieldResponse from(FormField f) {
        return new FormFieldResponse(f.getId(), f.getFieldName(), f.getLabel(), f.getFieldType(), f.getRequired(),
                f.getDisplayOrder(), f.getOptions());
    }
}
