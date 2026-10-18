package com.example.demo.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.demo.entity.FormStatus;
import com.example.demo.entity.FormTemplate;

/** U5:T9 Unit test ส่วน DAO: FormTemplateRepository */
@DataJpaTest
class FormTemplateRepositoryTest {

    private static final Long STAFF_ID = 1L;

    @Autowired
    private FormTemplateRepository repository;

    private FormTemplate v1;
    private FormTemplate v2;

    @BeforeEach
    void setUp() {
        v1 = new FormTemplate("คำร้องสหกิจ 1/2569", 1, STAFF_ID);
        v1.publish(LocalDateTime.of(2026, 6, 1, 9, 0));
        v1 = repository.save(v1);
        v2 = repository.save(new FormTemplate("คำร้องสหกิจ 2/2569", 2, STAFF_ID));
    }

    @Test
    @DisplayName("DAO-F01 บันทึกแบบฟอร์มใหม่แล้วค้นด้วย id ได้ข้อมูลครบ สถานะเริ่มต้นเป็น DRAFT")
    void saveAndFindById() {
        FormTemplate f = new FormTemplate("คำร้องสหกิจ 3/2569", 3, STAFF_ID);
        f.setDescription("แนบ Transcript");
        f.setDeadline(LocalDateTime.of(2026, 11, 30, 23, 59, 59));

        FormTemplate saved = repository.saveAndFlush(f);
        FormTemplate found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getName()).isEqualTo("คำร้องสหกิจ 3/2569");
        assertThat(found.getStatus()).isEqualTo(FormStatus.DRAFT);
        assertThat(found.getDeadline()).isEqualTo(LocalDateTime.of(2026, 11, 30, 23, 59, 59));
        assertThat(found.getCreatedBy()).isEqualTo(STAFF_ID);
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("DAO-F02 findByStatus(PUBLISHED) ได้เฉพาะฉบับที่เปิดใช้งาน")
    void findPublished() {
        List<FormTemplate> published = repository.findByStatus(FormStatus.PUBLISHED);

        assertThat(published).extracting(FormTemplate::getVersion).containsExactly(1);
    }

    @Test
    @DisplayName("DAO-F03 รายการแบบฟอร์มเรียงจาก version ล่าสุด")
    void findAllNewestFirst() {
        assertThat(repository.findAllByOrderByVersionDesc())
                .extracting(FormTemplate::getVersion).containsExactly(2, 1);
    }

    @Test
    @DisplayName("DAO-F04 findTopByOrderByVersionDesc ได้ฉบับ version สูงสุด")
    void findLatestVersion() {
        assertThat(repository.findTopByOrderByVersionDesc()).get()
                .extracting(FormTemplate::getVersion).isEqualTo(2);
    }

    @Test
    @DisplayName("DAO-F05 ยังไม่มีแบบฟอร์มเลย findTopByOrderByVersionDesc ได้ค่าว่าง")
    void findLatestVersionWhenEmpty() {
        repository.deleteAll();

        assertThat(repository.findTopByOrderByVersionDesc()).isEmpty();
    }

    @Test
    @DisplayName("DAO-F06 เปิดใช้ฉบับใหม่และเลิกใช้ฉบับเก่า สถานะใหม่ถูกบันทึก")
    void publishAndArchive() {
        v1.archive();
        v2.publish(LocalDateTime.of(2026, 10, 17, 10, 0));
        repository.saveAndFlush(v1);
        repository.saveAndFlush(v2);

        assertThat(repository.findByStatus(FormStatus.PUBLISHED))
                .extracting(FormTemplate::getVersion).containsExactly(2);
        assertThat(repository.findById(v1.getId()).orElseThrow().getStatus()).isEqualTo(FormStatus.ARCHIVED);
        assertThat(repository.findById(v2.getId()).orElseThrow().getPublishedAt())
                .isEqualTo(LocalDateTime.of(2026, 10, 17, 10, 0));
    }

    @Test
    @DisplayName("DAO-F07 บันทึกแบบฟอร์มที่ไม่มีชื่อ ต้องถูกปฏิเสธ")
    void saveWithoutNameFails() {
        assertThatThrownBy(() -> repository.saveAndFlush(new FormTemplate(null, 9, STAFF_ID)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
