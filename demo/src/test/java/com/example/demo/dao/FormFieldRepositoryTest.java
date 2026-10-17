package com.example.demo.dao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.example.demo.entity.FieldType;
import com.example.demo.entity.FormField;

/** U5:T9 Unit test ส่วน DAO: FormFieldRepository */
@DataJpaTest
class FormFieldRepositoryTest {

    private static final Long FORM_A = 10L;
    private static final Long FORM_B = 11L;

    @Autowired
    private FormFieldRepository repository;

    @BeforeEach
    void setUp() {
        // บันทึกสลับลำดับ เพื่อตรวจว่าตอนอ่านเรียงตาม displayOrder จริง
        repository.save(new FormField(FORM_A, "อีเมล", FieldType.EMAIL, true, 3));
        repository.save(new FormField(FORM_A, "ชื่อ-นามสกุล", FieldType.TEXT, true, 1));
        repository.save(new FormField(FORM_A, "เบอร์โทรศัพท์", FieldType.PHONE, false, 2));
        repository.save(new FormField(FORM_B, "Transcript", FieldType.FILE, true, 1));
    }

    @Test
    @DisplayName("DAO-FF01 อ่านช่องของแบบฟอร์มเรียงตามลำดับที่แสดง")
    void findOrdered() {
        assertThat(repository.findByFormTemplateIdOrderByDisplayOrderAsc(FORM_A))
                .extracting(FormField::getLabel)
                .containsExactly("ชื่อ-นามสกุล", "เบอร์โทรศัพท์", "อีเมล");
    }

    @Test
    @DisplayName("DAO-FF02 ไม่ปนช่องของแบบฟอร์มอื่น")
    void notMixedWithOtherForm() {
        assertThat(repository.findByFormTemplateIdOrderByDisplayOrderAsc(FORM_B))
                .extracting(FormField::getLabel).containsExactly("Transcript");
    }

    @Test
    @DisplayName("DAO-FF03 ลบช่องทั้งหมดของแบบฟอร์ม A แล้ว ช่องของแบบฟอร์ม B ยังอยู่")
    void deleteByTemplate() {
        repository.deleteByFormTemplateId(FORM_A);
        repository.flush();

        assertThat(repository.findByFormTemplateIdOrderByDisplayOrderAsc(FORM_A)).isEmpty();
        assertThat(repository.countByFormTemplateId(FORM_B)).isEqualTo(1);
    }

    @Test
    @DisplayName("DAO-FF04 นับจำนวนช่องของแบบฟอร์ม")
    void countFields() {
        assertThat(repository.countByFormTemplateId(FORM_A)).isEqualTo(3);
        assertThat(repository.countByFormTemplateId(99L)).isZero();
    }

    @Test
    @DisplayName("DAO-FF05 ช่องชนิดตัวเลือกเก็บตัวเลือกได้ และตั้ง fieldName ตามลำดับ")
    void selectOptionsSaved() {
        FormField f = new FormField(FORM_B, "ภาคการศึกษาที่ฝึก", FieldType.SELECT, true, 2);
        f.setOptions("1/2570\n2/2570");
        FormField saved = repository.saveAndFlush(f);

        FormField found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getOptions()).isEqualTo("1/2570\n2/2570");
        assertThat(found.getFieldName()).isEqualTo("field_2");
        assertThat(found.getRequired()).isTrue();
    }
}
