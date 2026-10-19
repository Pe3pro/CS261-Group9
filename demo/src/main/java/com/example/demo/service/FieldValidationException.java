package com.example.demo.service;

import java.util.List;

import com.example.demo.dto.FieldErrorResponse.FieldError;

/** U3: ข้อมูลในแบบฟอร์มไม่ครบหรือผิดรูปแบบ → 400 พร้อมรายชื่อช่องที่ผิด */
public class FieldValidationException extends ApiException {

    private static final long serialVersionUID = 1L;

    private final transient List<FieldError> fields;

    public FieldValidationException(List<FieldError> fields) {
        super(400, "VALIDATION_ERROR", "กรุณากรอกข้อมูลที่จำเป็นให้ครบและถูกต้อง");
        this.fields = List.copyOf(fields);
    }

    public List<FieldError> getFields() {
        return fields;
    }
}
