package com.example.demo.dto;

import java.util.List;

/** U3: error 400 ที่บอกรายชื่อช่องที่ผิด เช่น {"code":"VALIDATION_ERROR","message":"...","fields":[{"field":"field_2",...}]} */
public record FieldErrorResponse(String code, String message, List<FieldError> fields) {

    public record FieldError(String field, String label, String message) {
    }
}
