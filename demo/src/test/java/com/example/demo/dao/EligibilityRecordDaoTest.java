package com.example.demo.dao;

import com.example.demo.entity.EligibilityRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EligibilityRecordDaoTest {

    @Autowired
    private EligibilityRecordDao dao;

    @Test
    void shouldSaveAndFindRecordById() {
        EligibilityRecord record = new EligibilityRecord(
            "6809650119",
            true,
            "ผ่านคุณสมบัติ",
            LocalDateTime.of(2026, 10, 17, 10, 0)
        );

        EligibilityRecord saved = dao.saveAndFlush(record);

        var result = dao.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getStudentId())
            .isEqualTo("6809650119");
        assertThat(result.get().isEligible()).isTrue();
        assertThat(result.get().getMessage())
            .isEqualTo("ผ่านคุณสมบัติ");
    }

    @Test
    void shouldFindRecordsByStudentIdNewestFirst() {
        EligibilityRecord older = new EligibilityRecord(
            "6809650119",
            false,
            "ไม่ผ่านคุณสมบัติ",
            LocalDateTime.of(2026, 10, 16, 9, 0)
        );

        EligibilityRecord newer = new EligibilityRecord(
            "6809650119",
            true,
            "ผ่านคุณสมบัติ",
            LocalDateTime.of(2026, 10, 17, 10, 0)
        );

        EligibilityRecord anotherStudent = new EligibilityRecord(
            "6809650366",
            true,
            "ผ่านคุณสมบัติ",
            LocalDateTime.of(2026, 10, 17, 11, 0)
        );

        dao.saveAllAndFlush(
            List.of(older, newer, anotherStudent)
        );

        var results =
            dao.findByStudentIdOrderByCheckedAtDesc("6809650119");

        assertThat(results).hasSize(2);
        assertThat(results)
            .extracting(EligibilityRecord::getId)
            .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void shouldReturnEmptyListWhenStudentIdNotFound() {
        var results =
            dao.findByStudentIdOrderByCheckedAtDesc("9999999999");

        assertThat(results).isEmpty();
    }

    @Test
    void shouldDeleteRecordById() {
        EligibilityRecord record = new EligibilityRecord(
            "6809650119",
            true,
            "ผ่านคุณสมบัติ",
            LocalDateTime.of(2026, 10, 17, 10, 0)
        );

        EligibilityRecord saved = dao.saveAndFlush(record);
        Long id = saved.getId();

        dao.deleteById(id);
        dao.flush();

        assertThat(dao.findById(id)).isEmpty();
    }
}