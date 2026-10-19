package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * U3: บันทึก Email ลง log ของ server แทนการส่งจริง (บรรทัดขึ้นต้นด้วย [EMAIL])
 * เมื่อมี mail server ให้เพิ่ม implementation ที่ใช้ JavaMailSender แทน
 */
@Component
public class LoggingEmailNotifier implements EmailNotifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailNotifier.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("[EMAIL] to={} subject={} body={}", to, subject, body);
    }
}
