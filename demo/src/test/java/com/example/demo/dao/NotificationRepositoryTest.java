package com.example.demo.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.example.demo.entity.Notification;
import com.example.demo.entity.NotificationType;

/** U5:T9 Unit test ส่วน DAO: NotificationRepository */
@DataJpaTest
class NotificationRepositoryTest {

    private static final Long STUDENT = 3L;
    private static final Long OTHER = 4L;

    @Autowired
    private NotificationRepository repository;

    private Notification oldest;

    @BeforeEach
    void setUp() {
        oldest = save(STUDENT, "ยื่นใบสมัครสำเร็จ", LocalDateTime.of(2026, 10, 15, 10, 24));
        save(STUDENT, "ใบสมัครได้รับการอนุมัติ", LocalDateTime.of(2026, 10, 17, 9, 10));
        save(STUDENT, "เจ้าหน้าที่ตรวจใบสมัครแล้ว", LocalDateTime.of(2026, 10, 16, 11, 0));
        save(OTHER, "ของคนอื่น", LocalDateTime.of(2026, 10, 17, 12, 0));
    }

    private Notification save(Long userId, String title, LocalDateTime at) {
        Notification n = new Notification(userId, title, "-", NotificationType.APPLICATION_FORWARDED, 1L);
        n.setCreatedAt(at);
        return repository.save(n);
    }

    @Test
    @DisplayName("DAO-N01 การแจ้งเตือนของผู้ใช้เรียงจากใหม่ไปเก่า")
    void newestFirst() {
        assertThat(repository.findByUserIdOrderByCreatedAtDescIdDesc(STUDENT))
                .extracting(Notification::getTitle)
                .containsExactly("ใบสมัครได้รับการอนุมัติ", "เจ้าหน้าที่ตรวจใบสมัครแล้ว", "ยื่นใบสมัครสำเร็จ");
    }

    @Test
    @DisplayName("DAO-N02 ไม่เห็นการแจ้งเตือนของผู้ใช้คนอื่น")
    void onlyOwn() {
        assertThat(repository.findByUserIdOrderByCreatedAtDescIdDesc(OTHER))
                .extracting(Notification::getTitle).containsExactly("ของคนอื่น");
    }

    @Test
    @DisplayName("DAO-N03 การแจ้งเตือนใหม่ยังไม่ถูกอ่าน กดอ่านแล้วจำนวนที่ยังไม่อ่านลดลง")
    void markAsRead() {
        assertThat(oldest.getIsRead()).isFalse();
        assertThat(repository.countByUserIdAndIsReadFalse(STUDENT)).isEqualTo(3);

        oldest.markAsRead();
        repository.saveAndFlush(oldest);

        assertThat(repository.findById(oldest.getId()).orElseThrow().getIsRead()).isTrue();
        assertThat(repository.countByUserIdAndIsReadFalse(STUDENT)).isEqualTo(2);
    }
}
