package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.demo.entity.Role;
import com.example.demo.entity.Student;
import com.example.demo.entity.User;

/** U5: ใครเห็นคำร้องของนักศึกษาคนไหนได้ (ApplicationReviewService.canSee) */
class ApplicationReviewServiceTest {

    private static User user(Long id, Role role) {
        User u = new User("u" + id, "ชื่อ " + id, null, role);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private static Student studentOf(Long advisorId) {
        Student s = new Student(30L, "6609650001", null, null, null);
        s.setAdvisorId(advisorId);
        return s;
    }

    @Test
    @DisplayName("RV-01 อาจารย์ที่ปรึกษาเห็นคำร้องของนักศึกษาตนเอง")
    void ownAdvisorSees() {
        assertThat(ApplicationReviewService.canSee(user(2L, Role.ADVISOR), studentOf(2L))).isTrue();
    }

    @Test
    @DisplayName("RV-02 อาจารย์คนอื่นไม่เห็น และนักศึกษาที่ยังไม่มีอาจารย์ที่ปรึกษาก็ไม่แสดงให้อาจารย์")
    void otherAdvisorDoesNotSee() {
        assertThat(ApplicationReviewService.canSee(user(4L, Role.ADVISOR), studentOf(2L))).isFalse();
        assertThat(ApplicationReviewService.canSee(user(2L, Role.ADVISOR), studentOf(null))).isFalse();
    }

    @Test
    @DisplayName("RV-03 เจ้าหน้าที่เห็นทุกคำร้อง")
    void staffSeesAll() {
        assertThat(ApplicationReviewService.canSee(user(1L, Role.STAFF), studentOf(null))).isTrue();
    }
}
