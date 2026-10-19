# U2:T12 Design Unit Test Cases ส่วน DAO

## 1. วัตถุประสงค์

ออกแบบ Unit Test Cases สำหรับตรวจสอบการทำงานของ DAO ที่ใช้จัดเก็บ ค้นหา และลบประวัติผลการตรวจสอบคุณสมบัติของนักศึกษา พร้อมระบุ Input และ Expected Result ของแต่ละกรณี

## 2. DAO ที่ทดสอบ

ไฟล์:

`demo/src/main/java/com/example/demo/dao/EligibilityRecordDao.java`

ชื่อ DAO:

`EligibilityRecordDao`

DAO สืบทอดจาก `JpaRepository<EligibilityRecord, Long>` และรองรับการทำงานดังนี้:

- บันทึกข้อมูลผลการตรวจสอบ
- ค้นหาข้อมูลด้วย ID
- ค้นหาประวัติด้วยรหัสนักศึกษา
- เรียงประวัติจากรายการล่าสุดไปเก่าสุด
- ลบข้อมูลด้วย ID

## 3. ข้อมูลที่ใช้ทดสอบ

ข้อมูลตัวอย่างรายการที่ 1:

| Field | Input |
|---|---|
| studentId | `6410000001` |
| eligible | `true` |
| message | `ผ่านคุณสมบัติ` |
| checkedAt | `2026-10-18 10:00:00` |

ข้อมูลตัวอย่างรายการที่ 2:

| Field | Input |
|---|---|
| studentId | `6410000001` |
| eligible | `false` |
| message | `ไม่ผ่านคุณสมบัติ` |
| checkedAt | `2026-10-18 11:00:00` |

---

## 4. Test Cases

### DAO-01: บันทึกและค้นหาข้อมูลด้วย ID

| รายการ | รายละเอียด |
|---|---|
| Test Case ID | DAO-01 |
| DAO Method | `save()` และ `findById()` |
| Input | `studentId = 6410000001`, `eligible = true`, `message = ผ่านคุณสมบัติ` |
| ขั้นตอน | สร้าง EligibilityRecord บันทึกด้วย `save()` แล้วค้นหาด้วย ID ที่ได้รับ |
| Expected Result | บันทึกข้อมูลสำเร็จ มี ID และค่าที่ค้นหาได้ตรงกับข้อมูลที่บันทึก |

### DAO-02: ค้นหาประวัติด้วยรหัสนักศึกษา

| รายการ | รายละเอียด |
|---|---|
| Test Case ID | DAO-02 |
| DAO Method | `findByStudentIdOrderByCheckedAtDesc()` |
| Input | บันทึกข้อมูล 2 รายการที่มี `studentId = 6410000001` |
| ขั้นตอน | เรียกค้นหาข้อมูลด้วยรหัสนักศึกษา `6410000001` |
| Expected Result | DAO คืนข้อมูลของนักศึกษารหัส `6410000001` ครบทั้ง 2 รายการ |

### DAO-03: เรียงประวัติจากล่าสุดไปเก่าสุด

| รายการ | รายละเอียด |
|---|---|
| Test Case ID | DAO-03 |
| DAO Method | `findByStudentIdOrderByCheckedAtDesc()` |
| Input | รายการแรกเวลา `10:00:00` และรายการที่สองเวลา `11:00:00` |
| ขั้นตอน | ค้นหาประวัติด้วยรหัสนักศึกษา |
| Expected Result | รายการเวลา `11:00:00` อยู่ก่อนรายการเวลา `10:00:00` |

### DAO-04: ค้นหารหัสนักศึกษาที่ไม่มีในฐานข้อมูล

| รายการ | รายละเอียด |
|---|---|
| Test Case ID | DAO-04 |
| DAO Method | `findByStudentIdOrderByCheckedAtDesc()` |
| Input | `studentId = 9999999999` |
| ขั้นตอน | ค้นหาประวัติด้วยรหัสนักศึกษาที่ไม่มีข้อมูล |
| Expected Result | DAO คืน List ว่าง และไม่เกิด Exception |

### DAO-05: ลบข้อมูลด้วย ID

| รายการ | รายละเอียด |
|---|---|
| Test Case ID | DAO-05 |
| DAO Method | `deleteById()` และ `findById()` |
| Input | ID ของ EligibilityRecord ที่บันทึกไว้ |
| ขั้นตอน | ลบข้อมูลด้วย `deleteById()` แล้วค้นหาข้อมูลด้วย ID เดิม |
| Expected Result | ไม่พบข้อมูลหลังจากลบ และ `findById()` คืนค่า Empty |

### DAO-06: ข้อมูลของนักศึกษาคนอื่นไม่ปะปนกัน

| รายการ | รายละเอียด |
|---|---|
| Test Case ID | DAO-06 |
| DAO Method | `findByStudentIdOrderByCheckedAtDesc()` |
| Input | บันทึกข้อมูลของ `6410000001` และ `6410000002` |
| ขั้นตอน | ค้นหาด้วย `studentId = 6410000001` |
| Expected Result | DAO คืนเฉพาะข้อมูลของ `6410000001` และไม่คืนข้อมูลของ `6410000002` |

---

## 5. สรุป

ออกแบบ Unit Test Cases สำหรับ `EligibilityRecordDao` จำนวน 6 กรณี ครอบคลุมการบันทึก การค้นหาด้วย ID การค้นหาด้วยรหัสนักศึกษา การเรียงลำดับตามเวลา กรณีไม่พบข้อมูล การลบข้อมูล และการแยกข้อมูลระหว่างนักศึกษา