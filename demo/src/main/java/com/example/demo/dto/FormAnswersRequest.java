package com.example.demo.dto;

import java.util.Map;

/** U3: PUT /api/requests/{id}/form · answers = { fieldName: ค่า } เช่น {"field_1": "สมชาย ใจดี"} */
public record FormAnswersRequest(Map<String, String> answers) {
}
