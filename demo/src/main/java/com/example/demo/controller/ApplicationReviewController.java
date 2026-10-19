package com.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApplicationDetailResponse;
import com.example.demo.dto.ApplicationReviewResponse;
import com.example.demo.entity.ApplicationStatus;
import com.example.demo.service.ApplicationReviewService;

/**
 * U5: API รับคำร้องของนักศึกษา (เจ้าหน้าที่ / อาจารย์)
 * ใช้ params = "status" เพื่อไม่ชนกับ GET /api/applications?studentId=&formId= ของ U3
 */
@RestController
@RequestMapping("/api/applications")
public class ApplicationReviewController {

    private final ApplicationReviewService reviewService;

    public ApplicationReviewController(ApplicationReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping(params = "status")
    public List<ApplicationReviewResponse> getByStatus(@RequestParam ApplicationStatus status,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return reviewService.getByStatus(status, userId);
    }

    @GetMapping("/{id}/detail")
    public ApplicationDetailResponse getDetail(@PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return reviewService.getDetail(id, userId);
    }
}
