package com.example.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.demo.entity.Company;
import com.example.demo.entity.CompanyStatus;

/** U3:T9 Unit test ส่วน DAO: CompanyRepository */
@DataJpaTest
class CompanyRepositoryTest {

    @Autowired
    private CompanyRepository companyRepository;

    private Company scb;
    private Company agoda;
    private Company ptt;

    @BeforeEach
    void setUp() {
        scb = companyRepository.save(new Company("SCB TechX", CompanyStatus.OPEN));
        agoda = companyRepository.save(new Company("Agoda Services", CompanyStatus.OPEN));
        ptt = companyRepository.save(new Company("PTT Digital", CompanyStatus.CLOSED));
    }

    @Test
    @DisplayName("DAO-C01 บันทึกบริษัทใหม่แล้วค้นด้วย id ได้ข้อมูลครบ")
    void saveAndFindById() {
        Company c = new Company("KBTG", CompanyStatus.OPEN);
        c.setAddress("Bangkok");
        c.updateContactInfo("HR KBTG", "hr@kbtg.tech", "020000000");

        Company saved = companyRepository.saveAndFlush(c);
        Optional<Company> found = companyRepository.findById(saved.getId());

        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getCompanyName()).isEqualTo("KBTG");
        assertThat(found.get().getAddress()).isEqualTo("Bangkok");
        assertThat(found.get().getEmail()).isEqualTo("hr@kbtg.tech");
        assertThat(found.get().getStatus()).isEqualTo(CompanyStatus.OPEN);
    }

    @Test
    @DisplayName("DAO-C02 ค้นด้วย id ที่ไม่มีอยู่ ได้ค่าว่าง")
    void findByIdNotFound() {
        assertThat(companyRepository.findById(999_999L)).isEmpty();
    }

    @Test
    @DisplayName("DAO-C03 findByStatus(OPEN) ได้เฉพาะบริษัทที่เปิดรับ")
    void findOpenCompanies() {
        List<Company> open = companyRepository.findByStatus(CompanyStatus.OPEN);

        assertThat(open).extracting(Company::getCompanyName)
                .containsExactlyInAnyOrder("SCB TechX", "Agoda Services");
    }

    @Test
    @DisplayName("DAO-C04 findByStatus(CLOSED) ได้เฉพาะบริษัทที่ปิดรับ")
    void findClosedCompanies() {
        List<Company> closed = companyRepository.findByStatus(CompanyStatus.CLOSED);

        assertThat(closed).extracting(Company::getCompanyName).containsExactly("PTT Digital");
    }

    @Test
    @DisplayName("DAO-C05 ไม่มีบริษัทเปิดรับเลย findByStatus(OPEN) ได้ list ว่าง")
    void findOpenCompaniesWhenNone() {
        companyRepository.deleteAll();
        companyRepository.save(new Company("Closed Co", CompanyStatus.CLOSED));

        assertThat(companyRepository.findByStatus(CompanyStatus.OPEN)).isEmpty();
    }

    @Test
    @DisplayName("DAO-C06 ค้นหาชื่อบางส่วนแบบไม่สนตัวพิมพ์ใหญ่/เล็ก")
    void searchByNameIgnoreCase() {
        List<Company> result = companyRepository.findByCompanyNameContainingIgnoreCase("tech");

        assertThat(result).extracting(Company::getCompanyName).containsExactly("SCB TechX");
    }

    @Test
    @DisplayName("DAO-C07 ค้นหาชื่อที่ไม่มีในระบบ ได้ list ว่าง")
    void searchByNameNoMatch() {
        assertThat(companyRepository.findByCompanyNameContainingIgnoreCase("Google")).isEmpty();
    }

    @Test
    @DisplayName("DAO-C08 ปิดรับสมัครแล้วบันทึก สถานะใหม่ถูกเก็บลงฐานข้อมูล")
    void updateStatus() {
        agoda.closeRecruitment();
        companyRepository.saveAndFlush(agoda);

        assertThat(companyRepository.findById(agoda.getId()).orElseThrow().getStatus())
                .isEqualTo(CompanyStatus.CLOSED);
        assertThat(companyRepository.findByStatus(CompanyStatus.OPEN))
                .extracting(Company::getCompanyName).containsExactly("SCB TechX");
    }

    @Test
    @DisplayName("DAO-C09 ลบบริษัทแล้วค้นหาไม่พบ")
    void deleteCompany() {
        companyRepository.delete(scb);
        companyRepository.flush();

        assertThat(companyRepository.findById(scb.getId())).isEmpty();
        assertThat(companyRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("DAO-C10 บันทึกบริษัทที่ไม่มีชื่อ ต้องถูกปฏิเสธ")
    void saveWithoutNameFails() {
        assertThatThrownBy(() -> companyRepository.saveAndFlush(new Company(null, CompanyStatus.OPEN)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
