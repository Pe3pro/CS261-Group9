package com.example.demo.service;

/**
 * Error ทางธุรกิจที่ Controller แปลงเป็น HTTP response
 * (ดู controller/ApiExceptionHandler)
 */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;
    private final String code;

    public ApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(400, "VALIDATION_ERROR", message);
    }

    public static ApiException unauthorized() {
        return new ApiException(401, "UNAUTHORIZED", "กรุณาเข้าสู่ระบบก่อนใช้งาน");
    }

    public static ApiException forbidden() {
        return new ApiException(403, "FORBIDDEN", "คุณไม่มีสิทธิ์ใช้งานส่วนนี้");
    }

    public static ApiException notFound(String message) {
        return new ApiException(404, "NOT_FOUND", message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(409, "INVALID_STATUS", message);
    }

    public int getStatus() { return status; }
    public String getCode() { return code; }
}
