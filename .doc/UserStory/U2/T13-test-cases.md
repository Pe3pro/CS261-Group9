# U2:T13 Design Integration Test ระหว่าง UI กับ API

## 1. วัตถุประสงค์

ออกแบบ Integration Test Cases เพื่อตรวจสอบการเชื่อมต่อระหว่างหน้า UI ตรวจสอบคุณสมบัติกับ REST API ตั้งแต่การสร้าง Request การส่งข้อมูล การรับ Response และการแสดงผลบนหน้า UI

## 2. ขอบเขตการทดสอบ

ส่วนประกอบที่ทดสอบร่วมกัน:

- UI: `frontend/check.html`
- JavaScript: `frontend/js/script.js`
- API Controller: `EligibilityController`
- Endpoint: `POST /api/eligibility/check`

ลำดับการทำงาน:

1. ผู้ใช้กรอกข้อมูลบนหน้า UI
2. JavaScript ตรวจรูปแบบข้อมูลเบื้องต้น
3. UI สร้าง JSON Request
4. UI ส่ง HTTP POST ไปยัง API
5. API ส่ง JSON Response กลับ
6. UI แสดงผลผ่าน ไม่ผ่าน หรือข้อความผิดพลาด

## 3. ข้อมูลมาตรฐานที่ใช้ทดสอบ

ข้อมูลที่ผ่านการตรวจสอบ:

```json
{
  "studentId": "6410000001",
  "curriculum": "61",
  "gpax": "3.10",
  "grades": {
    "คพ.101": "B",
    "คพ.102": "B",
    "คพ.111": "B",
    "คพ.213": "B",
    "คพ.251": "B",
    "คพ.264": "B"
  },
  "courseStatuses": {
    "คพ.384": "กำลังศึกษา",
    "คพ.266": "กำลังศึกษา"
  },
  "acknowledgements": [
    true,
    true,
    true
  ]
}
```

---

## 4. Integration Test Cases

### UI-API-01: ส่งข้อมูลถูกต้องและผ่านคุณสมบัติ

| รายการ | รายละเอียด |
|---|---|
| Test Case ID | UI-API-01 |
| Input | รหัสนักศึกษา `6410000001`, หลักสูตร `61`, GPA `3.10`, เกรดทุกวิชา `B`, สถานะวิชา `กำลังศึกษา`, ยืนยันครบ 3 ข้อ |
| ขั้นตอน | กรอกข้อมูลบน UI และกดปุ่มตรวจสอบคุณสมบัติ |
| Expected Request | UI ส่ง `POST /api/eligibility/check` พร้อม JSON ที่กรอก |
| Expected API Result | HTTP `200 OK`, `valid = true`, `eligible = true`, `message = ผ่านคุณสมบัติ` |
| Expected UI Result | UI แสดงข้อความ `ผ่านคุณสมบัติ` |

### UI-API-02: ส่งข้อมูลถูกต้องแต่ไม่ผ่านคุณสมบัติ

| รายการ | รายละเอียด |
|---|---|
| Test Case ID | UI-API-02 |
| Input | ใช้ข้อมูลมาตรฐาน แต่กำหนด GPA เป็น `2.74` |
| ขั้นตอน | กรอกข้อมูลบน UI และกดปุ่มตรวจสอบคุณสมบัติ |
| Expected Request | UI ส่ง `POST /api/eligibility/check` โดยมี `gpax = 2.74` |
| Expected API Result | HTTP `200 OK`, `valid = true`, `eligible = false`, เหตุผลมี `GPA ต้องไม่ต่ำกว่า 2.75` |
| Expected UI Result | UI แสดงข้อความ `ไม่ผ่านคุณสมบัติ` และแสดงเหตุผลว่า GPA ต่ำกว่าเกณฑ์ |

### UI-API-03: กรอกข้อมูลไม่ครบ

| รายการ | รายละเอียด |
|---|---|
| Test Case ID | UI-API-03 |
| Input | รหัสนักศึกษาเป็นค่าว่าง และข้อมูลอื่นกรอกครบ |
| ขั้นตอน | กดปุ่มตรวจสอบคุณสมบัติ |
| Expected Request | UI ไม่ส่ง Request ไปยัง API เพราะการตรวจสอบข้อมูลเบื้องต้นไม่ผ่าน |
| Expected API Result | ไม่มีการเรียก API |
| Expected UI Result | UI แสดงข้อความ `กรอกข้อมูลให้ครบถ้วน` |

### UI-API-04: กรอกข้อมูลผิดรูปแบบ

| รายการ | รายละเอียด |
|---|---|
| Test Case ID | UI-API-04 |
| Input | GPA เป็น `3.10abc` |
| ขั้นตอน | กดปุ่มตรวจสอบคุณสมบัติ |
| Expected Request | UI ไม่ส่ง Request ไปยัง API เพราะ GPA มีรูปแบบไม่ถูกต้อง |
| Expected API Result | ไม่มีการเรียก API |
| Expected UI Result | UI แสดงข้อความว่า GPA ต้องเป็นตัวเลขระหว่าง 0.00–4.00 |

### UI-API-05: API ปฏิเสธข้อมูลที่ไม่ถูกต้อง

| รายการ | รายละเอียด |
|---|---|
| Test Case ID | UI-API-05 |
| Input | API ตอบกลับ HTTP `400 Bad Request` พร้อม `valid = false` และรายการ `errors` |
| ขั้นตอน | จำลอง Response ข้อมูลไม่ถูกต้องจาก API |
| Expected API Result | Response มีข้อความ `ข้อมูลไม่ถูกต้อง` พร้อมรายการข้อผิดพลาด |
| Expected UI Result | UI แสดงข้อความจาก `errors` ให้ผู้ใช้เห็น และไม่แสดงว่าผ่านคุณสมบัติ |

### UI-API-06: ไม่สามารถเชื่อมต่อ API

| รายการ | รายละเอียด |
|---|---|
| Test Case ID | UI-API-06 |
| Input | ปิด Backend หรือจำลองให้ `fetch()` เกิด Network Error |
| ขั้นตอน | กรอกข้อมูลถูกต้องแล้วกดปุ่มตรวจสอบคุณสมบัติ |
| Expected Request | UI พยายามส่ง Request ไปยัง API |
| Expected API Result | ไม่ได้รับ Response เนื่องจากเชื่อมต่อ API ไม่สำเร็จ |
| Expected UI Result | UI แสดงข้อความแจ้งว่าไม่สามารถเชื่อมต่อระบบได้ และไม่ค้างอยู่ในสถานะกำลังโหลด |

## 5. เกณฑ์การผ่าน

Integration Test ผ่านเมื่อ:

- UI ส่ง Method, Endpoint และ JSON Request ถูกต้อง
- UI อ่านค่า `valid`, `eligible`, `message`, `errors`, `reasons` และ `warnings` จาก Response ได้
- UI แสดงผลผ่านและไม่ผ่านได้ตรงกับ Response
- UI ป้องกันการส่งข้อมูลไม่ครบและข้อมูลผิดรูปแบบ
- UI แสดงข้อความผิดพลาดเมื่อ API ใช้งานไม่ได้

## 6. สรุป

ออกแบบ Integration Test ระหว่าง UI กับ API จำนวน 6 กรณี ครอบคลุมกรณีผ่านคุณสมบัติ ไม่ผ่านคุณสมบัติ กรอกข้อมูลไม่ครบ กรอกข้อมูลผิดรูปแบบ API ปฏิเสธข้อมูล และไม่สามารถเชื่อมต่อ API