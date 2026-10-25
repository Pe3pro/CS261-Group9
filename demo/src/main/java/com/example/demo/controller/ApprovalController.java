package com.example.demo.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApprovalRequest;
import com.example.demo.dto.ApprovalResponse;
import com.example.demo.service.ApprovalService;

/** U5: API บันทึกผลการพิจารณาคำร้อง ({id} = id ของคำร้อง) */
@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PostMapping("/{id}/decision")
    public ApprovalResponse decide(@PathVariable Long id, @RequestBody ApprovalRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return approvalService.decide(id, userId, request);
    }
}
