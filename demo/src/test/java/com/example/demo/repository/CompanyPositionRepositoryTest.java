package com.example.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.demo.entity.Company;
import com.example.demo.entity.CompanyPosition;
import com.example.demo.entity.CompanyStatus;
import com.example.demo.entity.PositionStatus;

/** U3:T9 Unit test ส่วน DAO: CompanyPositionRepository */
@DataJpaTest
class CompanyPositionRepositoryTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyPositionRepository positionRepository;

    private Long scbId;
    private Long agodaId;
    private CompanyPosition backend;

    @BeforeEach
    void setUp() {
        scbId = companyRepository.save(new Company("SCB TechX", CompanyStatus.OPEN)).getId();
        agodaId = companyRepository.save(new Company("Agoda Services", CompanyStatus.OPEN)).getId();

        backend = positionRepository.save(new CompanyPosition(scbId, "Backend Developer", 3, PositionStatus.OPEN));
        positionRepository.save(new CompanyPosition(scbId, "Data Analyst", 2, PositionStatus.OPEN));
        positionRepository.save(new CompanyPosition(scbId, "QA Tester", 1, PositionStatus.CLOSED));

        CompanyPosition full = new CompanyPosition(scbId, "Frontend Developer", 2, PositionStatus.OPEN);
        full.setAvailableSlots(0);
        positionRepository.save(full);

        positionRepository.save(new CompanyPosition(agodaId, "Software Engineer", 5, PositionStatus.OPEN));
    }

    @Test
    @DisplayName("DAO-P01 บันทึกตำแหน่งใหม่แล้วค้นด้วย id ได้ข้อมูลครบ")
    void saveAndFindById() {
        CompanyPosition p = new CompanyPosition(agodaId, "DevOps Intern", 4, PositionStatus.OPEN);
        p.setDescription("ดูแล CI/CD");

        CompanyPosition saved = positionRepository.saveAndFlush(p);
        CompanyPosition found = positionRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getCompanyId()).isEqualTo(agodaId);
        assertThat(found.getPositionName()).isEqualTo("DevOps Intern");
        assertThat(found.getDescription()).isEqualTo("ดูแล CI/CD");
        assertThat(found.getQuota()).isEqualTo(4);
        assertThat(found.getAvailableSlots()).isEqualTo(4);
        assertThat(found.getStatus()).isEqualTo(PositionStatus.OPEN);
    }

    @Test
    @DisplayName("DAO-P02 findByCompanyId ได้ทุกตำแหน่งของบริษัทนั้นเท่านั้น")
    void findByCompanyId() {
        List<CompanyPosition> result = positionRepository.findByCompanyId(scbId);

        assertThat(result).hasSize(4)
                .allMatch(p -> p.getCompanyId().equals(scbId));
    }

    @Test
    @DisplayName("DAO-P03 findByCompanyId ของบริษัทที่ไม่มีอยู่ ได้ list ว่าง")
    void findByCompanyIdUnknown() {
        assertThat(positionRepository.findByCompanyId(999_999L)).isEmpty();
    }

    @Test
    @DisplayName("DAO-P04 findOpenPositions ได้เฉพาะตำแหน่ง OPEN ที่ยังมีที่ว่าง เรียงตามชื่อ")
    void findOpenPositions() {
        List<CompanyPosition> open = positionRepository.findOpenPositions(scbId);

        assertThat(open).extracting(CompanyPosition::getPositionName)
                .containsExactly("Backend Developer", "Data Analyst");
    }

    @Test
    @DisplayName("DAO-P05 findOpenPositions ไม่ปนตำแหน่งของบริษัทอื่น")
    void findOpenPositionsOtherCompany() {
        List<CompanyPosition> open = positionRepository.findOpenPositions(agodaId);

        assertThat(open).extracting(CompanyPosition::getPositionName)
                .containsExactly("Software Engineer");
    }

    @Test
    @DisplayName("DAO-P06 ปิดตำแหน่งแล้ว findOpenPositions จะไม่แสดงตำแหน่งนั้น")
    void closedPositionIsHidden() {
        backend.setStatus(PositionStatus.CLOSED);
        positionRepository.saveAndFlush(backend);

        assertThat(positionRepository.findOpenPositions(scbId))
                .extracting(CompanyPosition::getPositionName)
                .containsExactly("Data Analyst");
    }

    @Test
    @DisplayName("DAO-P07 แก้โควตาแล้วบันทึก quota และ availableSlots ถูกอัปเดต")
    void updateQuota() {
        backend.setAvailableSlots(1); // รับไปแล้ว 2 จาก 3
        backend.updateQuota(5);
        positionRepository.saveAndFlush(backend);

        CompanyPosition found = positionRepository.findById(backend.getId()).orElseThrow();
        assertThat(found.getQuota()).isEqualTo(5);
        assertThat(found.getAvailableSlots()).isEqualTo(3);
    }

    @Test
    @DisplayName("DAO-P08 บันทึกตำแหน่งที่ไม่มี companyId ต้องถูกปฏิเสธ")
    void saveWithoutCompanyFails() {
        assertThatThrownBy(() -> positionRepository.saveAndFlush(
                new CompanyPosition(null, "Ghost", 1, PositionStatus.OPEN)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
