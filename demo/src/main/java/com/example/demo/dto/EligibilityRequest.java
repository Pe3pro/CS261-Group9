package com.example.demo.dto;

import java.util.List;
import java.util.Map;

public record EligibilityRequest(
    String studentId,
    String curriculum,
    String gpax,
    Map<String, String> grades,
    Map<String, String> courseStatuses,
    List<Boolean> acknowledgements
) {
}