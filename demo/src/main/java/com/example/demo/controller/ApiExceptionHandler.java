package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.example.demo.dto.ErrorResponse;
import com.example.demo.service.ApiException;

/** แปลง error เป็น JSON รูปแบบเดียวกันทุก API: {"code": "...", "message": "..."} */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(new ErrorResponse(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBadJson(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", "รูปแบบข้อมูลไม่ถูกต้อง"));
    }

    /** มีคนบันทึกคำร้องเดียวกันไปก่อนหน้า (ดู @Version ใน Application) */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrentUpdate(ObjectOptimisticLockingFailureException e) {
        return ResponseEntity.status(409).body(
                new ErrorResponse("INVALID_STATUS", "มีผู้บันทึกผลคำร้องนี้ไปก่อนแล้ว กรุณาโหลดหน้าใหม่"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleBadParam(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(
                new ErrorResponse("VALIDATION_ERROR", "ค่า " + e.getName() + " ไม่ถูกต้อง"));
    }
}
