package com.example.demo.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.dto.NotificationResponse;
import com.example.demo.service.ApiException;
import com.example.demo.service.NotificationService;

/** U5:T8 Unit test ส่วน API: /api/notifications */
@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private NotificationService service;

    @Test
    @DisplayName("API-19 นักศึกษาดูการแจ้งเตือนของตัวเอง ได้ 200")
    void list() throws Exception {
        when(service.getNotifications(3L)).thenReturn(List.of(
                new NotificationResponse(1L, "ใบสมัครได้รับการอนุมัติ", "ขั้นตอนต่อไปคือจับคู่บริษัท", 50L, false,
                        LocalDateTime.of(2026, 10, 17, 9, 10))));

        mvc.perform(get("/api/notifications").header("X-User-Id", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("ใบสมัครได้รับการอนุมัติ"))
                .andExpect(jsonPath("$[0].isRead").value(false));
    }

    @Test
    @DisplayName("API-20 กดอ่านการแจ้งเตือน ได้ isRead = true")
    void markAsRead() throws Exception {
        when(service.markAsRead(1L, 3L)).thenReturn(new NotificationResponse(1L, "ใบสมัครได้รับการอนุมัติ",
                "-", 50L, true, LocalDateTime.of(2026, 10, 17, 9, 10)));

        mvc.perform(patch("/api/notifications/1/read").header("X-User-Id", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    @DisplayName("API-21 กดอ่านการแจ้งเตือนของคนอื่น ได้ 403")
    void markOthersNotification() throws Exception {
        when(service.markAsRead(1L, 4L)).thenThrow(ApiException.forbidden());

        mvc.perform(patch("/api/notifications/1/read").header("X-User-Id", "4"))
                .andExpect(status().isForbidden());
    }
}
