package com.example.demo.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dao.ApplicationRepository;
import com.example.demo.dao.ApprovalRepository;
import com.example.demo.dto.ApprovalRequest;
import com.example.demo.dto.ApprovalResponse;
import com.example.demo.entity.Application;
import com.example.demo.entity.ApplicationStatus;
import com.example.demo.entity.Approval;
import com.example.demo.entity.ApprovalDecision;
import com.example.demo.entity.ApprovalLevel;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;

/**
 * U5: เจ้าหน้าที่ / อาจารย์ พิจารณาคำร้อง
 * เจ้าหน้าที่ตัดสินได้เฉพาะคำร้อง SUBMITTED, อาจารย์ตัดสินได้เฉพาะคำร้อง UNDER_REVIEW
 */
@Service
@Transactional
public class ApprovalService {

    private final ApplicationRepository applicationRepository;
    private final ApprovalRepository approvalRepository;
    private final NotificationService notificationService;
    private final AccessGuard guard;
    private final Clock clock;

    public ApprovalService(ApplicationRepository applicationRepository, ApprovalRepository approvalRepository,
            NotificationService notificationService, AccessGuard guard, Clock clock) {
        this.applicationRepository = applicationRepository;
        this.approvalRepository = approvalRepository;
        this.notificationService = notificationService;
        this.guard = guard;
        this.clock = clock;
    }

    public ApprovalResponse decide(Long applicationId, Long userId, ApprovalRequest request) {
        User approver = guard.require(userId, Role.STAFF, Role.ADVISOR);
        if (request == null || request.decision() == null) {
            throw ApiException.badRequest("กรุณาเลือกผลการพิจารณา");
        }
        ApprovalDecision decision = request.decision();
        String comment = request.comment() == null ? null : request.comment().trim();
        if (comment != null && comment.isEmpty()) {
            comment = null;
        }
        if (decision != ApprovalDecision.APPROVE && comment == null) {
            throw ApiException.badRequest("กรุณากรอกเหตุผลเมื่อส่งกลับแก้ไขหรือปฏิเสธ");
        }

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> ApiException.notFound("ไม่พบคำร้อง"));
        ApprovalLevel level = approver.getRole() == Role.STAFF ? ApprovalLevel.STAFF : ApprovalLevel.ADVISOR;
        ApplicationStatus expected = level == ApprovalLevel.STAFF
                ? ApplicationStatus.SUBMITTED : ApplicationStatus.UNDER_REVIEW;
        if (app.getStatus() != expected) {
            throw ApiException.conflict("คำร้องนี้ไม่ได้อยู่ในขั้นที่คุณพิจารณาได้ (สถานะปัจจุบัน: "
                    + app.getStatus() + ")");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Approval approval = approvalRepository.save(
                new Approval(app.getId(), approver.getId(), level, decision, comment, now));
        ApplicationStatus next = nextStatus(level, decision);
        app.changeStatus(next);
        applicationRepository.save(app);
        notificationService.notifyAfterDecision(app, level, decision, comment);

        return new ApprovalResponse(approval.getId(), app.getId(), level, decision, approver.getFullName(),
                comment, now, next);
    }

    static ApplicationStatus nextStatus(ApprovalLevel level, ApprovalDecision decision) {
        return switch (decision) {
            case APPROVE -> level == ApprovalLevel.STAFF ? ApplicationStatus.UNDER_REVIEW : ApplicationStatus.APPROVED;
            case RETURN -> ApplicationStatus.NEED_CORRECTION;
            case REJECT -> ApplicationStatus.REJECTED;
        };
    }
}
