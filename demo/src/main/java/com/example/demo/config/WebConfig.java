package com.example.demo.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** ตั้งค่าเว็บ: เปิด CORS ให้หน้าเว็บจาก Live Server (port 5500) เรียก API ที่ port 8080 ได้ */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    /** นาฬิกาของระบบ แยกเป็น Bean เพื่อให้ test กำหนดเวลาได้ */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
