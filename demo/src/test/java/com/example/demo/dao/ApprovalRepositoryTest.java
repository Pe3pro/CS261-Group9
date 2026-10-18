package com.example.demo.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.demo.entity.Approval;
import com.example.demo.entity.ApprovalDecision;
import com.example.demo.entity.ApprovalLevel;

/** U5:T9 Unit test ส่วน DAO: ApprovalRepository */
@DataJpaTest
class ApprovalRepositoryTest {

    private static final Long APP_ID = 50L;
    private static final Long STAFF_ID = 1L;
    private static final Long ADVISOR_ID = 2L;

    @Autowired
    private ApprovalRepository repository;

    @Test
    @DisplayName("DAO-AP01 บันทึกผลการพิจารณาแล้วค้นกลับได้ข้อมูลครบ")
    void saveAndFind() {
        LocalDateTime at = LocalDateTime.of(2026, 10, 16, 11, 0);
        Approval saved = repository.saveAndFlush(new Approval(APP_ID, STAFF_ID, ApprovalLevel.STAFF,
                ApprovalDecision.RETURN, "Transcript ไม่ชัด", at));

        Approval found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getApprovalLevel()).isEqualTo(ApprovalLevel.STAFF);
        assertThat(found.getDecision()).isEqualTo(ApprovalDecision.RETURN);
        assertThat(found.getComment()).isEqualTo("Transcript ไม่ชัด");
        assertThat(found.getDecidedAt()).isEqualTo(at);
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("DAO-AP02 ประวัติการพิจารณาเรียงตามลำดับ เจ้าหน้าที่ก่อน อาจารย์ทีหลัง")
    void historyInOrder() {
        repository.save(new Approval(APP_ID, STAFF_ID, ApprovalLevel.STAFF, ApprovalDecision.APPROVE, null,
                LocalDateTime.of(2026, 10, 16, 11, 0)));
        repository.save(new Approval(APP_ID, ADVISOR_ID, ApprovalLevel.ADVISOR, ApprovalDecision.APPROVE, null,
                LocalDateTime.of(2026, 10, 17, 9, 10)));

        assertThat(repository.findByApplicationIdOrderByIdAsc(APP_ID))
                .extracting(Approval::getApprovalLevel)
                .containsExactly(ApprovalLevel.STAFF, ApprovalLevel.ADVISOR);
    }

    @Test
    @DisplayName("DAO-AP03 ไม่ปนประวัติของคำร้องอื่น และคำร้องที่ยังไม่ถูกพิจารณาได้รายการว่าง")
    void notMixed() {
        repository.save(new Approval(APP_ID, STAFF_ID, ApprovalLevel.STAFF, ApprovalDecision.APPROVE, null,
                LocalDateTime.now()));

        assertThat(repository.findByApplicationIdOrderByIdAsc(APP_ID)).hasSize(1);
        assertThat(repository.findByApplicationIdOrderByIdAsc(51L)).isEmpty();
    }

    @Test
    @DisplayName("DAO-AP04 บันทึกโดยไม่มีผลการพิจารณา ต้องถูกปฏิเสธ")
    void saveWithoutDecisionFails() {
        assertThatThrownBy(() -> repository.saveAndFlush(
                new Approval(APP_ID, STAFF_ID, ApprovalLevel.STAFF, null, null, LocalDateTime.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
