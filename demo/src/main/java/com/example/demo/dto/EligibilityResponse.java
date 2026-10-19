package com.example.demo.dto;

import java.util.List;

public record EligibilityResponse(
    boolean valid,
    Boolean eligible,
    String message,
    List<String> errors,
    List<String> reasons,
    List<String> warnings
) {
}