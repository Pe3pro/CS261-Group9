package com.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.NotificationResponse;
import com.example.demo.service.NotificationService;

/** U5: API การแจ้งเตือนของผู้ใช้ที่ login อยู่ */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> getNotifications(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return notificationService.getNotifications(userId);
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(@PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return notificationService.markAsRead(id, userId);
    }
}
