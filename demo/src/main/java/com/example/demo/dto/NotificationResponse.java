package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.entity.Notification;

/** ส่งการแจ้งเตือน */
public record NotificationResponse(Long id, String title, String message, Long applicationId, Boolean isRead,
        LocalDateTime createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getTitle(), n.getMessage(), n.getApplicationId(), n.getIsRead(),
                n.getCreatedAt());
    }
}
