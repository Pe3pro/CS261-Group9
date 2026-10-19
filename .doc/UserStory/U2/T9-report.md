# U2:T9 DAO Unit Test Report

## 1. วัตถุประสงค์

ทดสอบการทำงานของ DAO สำหรับจัดเก็บและค้นหาผลการตรวจสอบคุณสมบัติของนักศึกษา โดยตรวจสอบการเพิ่ม อ่าน ค้นหา และลบข้อมูลผ่าน Spring Data JPA

## 2. ส่วนประกอบที่ทดสอบ

### Entity

ไฟล์:

`demo/src/main/java/com/example/demo/entity/EligibilityRecord.java`

ข้อมูลที่จัดเก็บประกอบด้วย:

- `id` หมายเลขประจำรายการ
- `studentId` รหัสนักศึกษา
- `eligible` สถานะผ่านหรือไม่ผ่านคุณสมบัติ
- `message` ข้อความผลการตรวจสอบ
- `checkedAt` วันและเวลาที่ตรวจสอบ

### DAO

ไฟล์:

`demo/src/main/java/com/example/demo/dao/EligibilityRecordDao.java`

DAO สืบทอดจาก `JpaRepository` และรองรับการทำงานดังนี้:

- บันทึกข้อมูลผลการตรวจสอบ
- ค้นหาข้อมูลด้วย ID
- ลบข้อมูลด้วย ID
- ค้นหาประวัติด้วยรหัสนักศึกษา
- เรียงประวัติจากรายการล่าสุดไปเก่าสุด

## 3. เครื่องมือที่ใช้

- Java 21
- Spring Boot 4.1.1
- Spring Data JPA
- JUnit
- AssertJ
- H2 In-Memory Database
- Maven

การทดสอบใช้ H2 เป็นฐานข้อมูลสำหรับ Test เพื่อไม่ให้ข้อมูลทดสอบกระทบกับฐานข้อมูล MySQL จริง

## 4. Test Cases

| ลำดับ | Test Case | ผลลัพธ์ที่คาดหวัง |
|---|---|---|
| 1 | `shouldSaveAndFindRecordById` | บันทึกข้อมูลและค้นหากลับด้วย ID ได้ |
| 2 | `shouldFindRecordsByStudentIdNewestFirst` | ค้นหาด้วยรหัสนักศึกษาได้ และเรียงข้อมูลล่าสุดก่อน |
| 3 | `shouldReturnEmptyListWhenStudentIdNotFound` | คืนรายการว่างเมื่อไม่พบรหัสนักศึกษา |
| 4 | `shouldDeleteRecordById` | ลบข้อมูลแล้วไม่สามารถค้นหาข้อมูลเดิมได้ |

## 5. คำสั่งที่ใช้ทดสอบ

```powershell
.\mvnw.cmd test "-Dtest=EligibilityRecordDaoTest"
```

## 6. ผลการทดสอบ

- Tests run: 4
- Failures: 0
- Errors: 0
- Skipped: 0
- Build result: `BUILD SUCCESS`

สรุปผล: DAO ผ่านการทดสอบครบทั้ง 4 Test Cases

## 7. ไฟล์หลักฐาน

- ไฟล์ DAO Test: [`EligibilityRecordDaoTest.java`](../../../demo/src/test/java/com/example/demo/dao/EligibilityRecordDaoTest.java)
- ผลการรันทดสอบ: [`T9-test-output.txt`](evidence/T9-test-output.txt)