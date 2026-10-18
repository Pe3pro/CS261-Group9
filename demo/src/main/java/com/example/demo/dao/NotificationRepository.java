package com.example.demo.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Notification;

/** DAO ของตาราง NOTIFICATIONS */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** การแจ้งเตือนของผู้ใช้ ใหม่สุดอยู่บน */
    List<Notification> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);
}
