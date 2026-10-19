# U2:T18 รายงานผล Integration Test ระหว่าง API กับ DAO

## 1. วัตถุประสงค์

ทดสอบการทำงานร่วมกันระหว่าง REST API, Service และ DAO
โดยตรวจสอบว่า API สามารถรับข้อมูล ประเมินคุณสมบัติ
และบันทึกผลผ่าน DAO ลงฐานข้อมูลได้อย่างถูกต้อง

การทดสอบอ้างอิง Test Cases ที่ออกแบบไว้ใน U2:T14
และครอบคลุมกรณีกรอกข้อมูลไม่ครบและกรอกข้อมูลผิดรูปแบบ

## 2. ส่วนประกอบที่ทดสอบ

- API: `EligibilityController`
- Service: `EligibilityService`
- DAO: `EligibilityRecordDao`
- Entity: `EligibilityRecord`
- Endpoint: `POST /api/eligibility/check`
- Database สำหรับทดสอบ: H2 In-Memory Database
- Test framework: JUnit และ Spring Boot Test
- HTTP Test: MockMvc

## 3. วิธีการทดสอบ

การทดสอบโหลด Spring Application Context จริง และใช้ MockMvc
ส่ง HTTP Request ไปยัง Controller จากนั้นตรวจสอบผลตอบกลับของ API
และตรวจสอบข้อมูลที่ถูกบันทึกผ่าน `EligibilityRecordDao`

ฐานข้อมูลจะถูกล้างก่อนแต่ละ Test Case เพื่อไม่ให้ผลของแต่ละกรณี
กระทบต่อกัน

## 4. ผลการทดสอบ

| Test Case ID | กรณีทดสอบ | ผลลัพธ์ที่ตรวจสอบ | ผล |
|---|---|---|---|
| API-DAO-01 | ข้อมูลถูกต้องและผ่านคุณสมบัติ | API ตอบ HTTP 200 และ DAO บันทึก `eligible = true` | ผ่าน |
| API-DAO-02 | ข้อมูลถูกต้องแต่ GPA ต่ำกว่า 2.75 | API ตอบ HTTP 200 และ DAO บันทึก `eligible = false` | ผ่าน |
| API-DAO-03 | กรอกข้อมูลไม่ครบ | API ตอบ HTTP 400 และจำนวนข้อมูลในฐานข้อมูลไม่เพิ่ม | ผ่าน |
| API-DAO-04 | กรอกข้อมูลผิดรูปแบบ | API ตอบ HTTP 400 พร้อม errors และจำนวนข้อมูลไม่เพิ่ม | ผ่าน |
| API-DAO-05 | ส่ง Request ที่ถูกต้องหนึ่งครั้ง | ฐานข้อมูลเพิ่มขึ้นเพียงหนึ่งรายการ | ผ่าน |
| API-DAO-06 | ข้อมูลที่ DAO บันทึกตรงกับ API | `studentId`, `eligible` และ `message` ตรงกับ Response | ผ่าน |

## 5. คำสั่งที่ใช้ทดสอบ

```powershell
Set-Location P:\demo

.\mvnw.cmd "-Dtest=EligibilityApiDaoIntegrationTest" test 2>&1 |
    Tee-Object -FilePath 'P:\.doc\UserStory\U2\evidence\T18-api-dao-test-output.txt'
```

## 6. ผลการรัน

```text
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

ผลการทดสอบผ่านครบทั้ง 6 กรณี

## 7. Bug ที่พบ

ไม่พบ Bug จาก Test Cases ที่ดำเนินการในรอบนี้

ข้อจำกัด: การทดสอบอัตโนมัติใช้ H2 In-Memory Database
เพื่อแยกข้อมูลทดสอบออกจากฐานข้อมูล MySQL จริง

## 8. ไฟล์หลักฐาน

- [Test Case Specification U2:T14](T14-test-cases.md)
- [Integration Test Source](../../../demo/src/test/java/com/example/demo/integration/EligibilityApiDaoIntegrationTest.java)
- [Test Output](evidence/T18-api-dao-test-output.txt)

## 9. สรุป

Integration Test ระหว่าง API, Service และ DAO ผ่านครบทั้งหมด
API บันทึกผลผ่าน DAO เมื่อข้อมูลมีรูปแบบถูกต้องทั้งกรณีผ่าน
และไม่ผ่านคุณสมบัติ

กรณีกรอกข้อมูลไม่ครบและกรอกข้อมูลผิดรูปแบบ
API ตอบ HTTP 400 และไม่มีข้อมูลถูกบันทึกลงฐานข้อมูล
ข้อมูลที่ DAO บันทึกตรงกับผลลัพธ์ที่ API ส่งกลับ