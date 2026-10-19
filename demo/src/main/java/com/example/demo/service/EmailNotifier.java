package com.example.demo.service;

/**
 * U3: ส่ง Email แจ้งนักศึกษา (Sequence Diagram: Service → Email: Send notification)
 * แยกเป็น interface เพื่อให้ test จำลองได้ · ตอนนี้ใช้ LoggingEmailNotifier (ยังไม่มี mail server)
 */
public interface EmailNotifier {

    void send(String to, String subject, String body);
}
