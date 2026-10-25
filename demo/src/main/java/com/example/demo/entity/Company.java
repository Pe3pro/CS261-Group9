package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Entity ตาราง COMPANIES (U3: ดูบริษัทที่เปิดรับ) */
@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    private String address;

    @Column(name = "contact_person")
    private String contactPerson;

    private String email;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyStatus status;

    protected Company() {
        // สำหรับ JPA
    }

    public Company(String companyName, CompanyStatus status) {
        this.companyName = companyName;
        this.status = status;
    }

    public void openRecruitment() {
        this.status = CompanyStatus.OPEN;
    }

    public void closeRecruitment() {
        this.status = CompanyStatus.CLOSED;
    }

    public void updateContactInfo(String contactPerson, String email, String phone) {
        this.contactPerson = contactPerson;
        this.email = email;
        this.phone = phone;
    }

    public Long getId() { return id; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getContactPerson() { return contactPerson; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public CompanyStatus getStatus() { return status; }
    public void setStatus(CompanyStatus status) { this.status = status; }
}
