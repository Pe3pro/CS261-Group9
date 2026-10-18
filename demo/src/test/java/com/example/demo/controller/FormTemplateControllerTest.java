package com.example.demo.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.dto.FormFieldResponse;
import com.example.demo.dto.FormTemplateRequest;
import com.example.demo.dto.FormTemplateResponse;
import com.example.demo.entity.FieldType;
import com.example.demo.entity.FormStatus;
import com.example.demo.service.ApiException;
import com.example.demo.service.FormTemplateService;

/** U5:T8 Unit test ส่วน API: /api/forms */
@WebMvcTest(FormTemplateController.class)
class FormTemplateControllerTest {

    private static final String STAFF = "1";
    private static final String VALID_BODY = """
            {"name":"คำร้องขอฝึกสหกิจศึกษา 2/2569","description":"แนบ Transcript",
             "deadline":"2026-11-30T23:59:59",
             "fields":[{"label":"ชื่อ-นามสกุล","fieldType":"TEXT","required":true},
                       {"label":"เบอร์โทรศัพท์","fieldType":"PHONE","required":true}]}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private FormTemplateService service;

    private FormTemplateResponse form(FormStatus status) {
        return new FormTemplateResponse(10L, "คำร้องขอฝึกสหกิจศึกษา 2/2569", "แนบ Transcript", 1, status,
                LocalDateTime.of(2026, 11, 30, 23, 59, 59), LocalDateTime.of(2026, 10, 17, 9, 0), null,
                List.of(new FormFieldResponse(100L, "field_1", "ชื่อ-นามสกุล", FieldType.TEXT, true, 1, null),
                        new FormFieldResponse(101L, "field_2", "เบอร์โทรศัพท์", FieldType.PHONE, true, 2, null)));
    }

    @Test
    @DisplayName("API-01 สร้างแบบฟอร์มข้อมูลครบ ได้ 201 สถานะ DRAFT")
    void createForm() throws Exception {
        when(service.create(any(FormTemplateRequest.class), eq(1L))).thenReturn(form(FormStatus.DRAFT));

        mvc.perform(post("/api/forms").header("X-User-Id", STAFF)
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.fields", hasSize(2)))
                .andExpect(jsonPath("$.fields[1].fieldType").value("PHONE"));
    }

    @Test
    @DisplayName("API-02 สร้างแบบฟอร์มไม่มีชื่อ ได้ 400 VALIDATION_ERROR")
    void createFormWithoutName() throws Exception {
        when(service.create(any(), eq(1L))).thenThrow(ApiException.badRequest("กรุณาระบุชื่อแบบฟอร์ม"));

        mvc.perform(post("/api/forms").header("X-User-Id", STAFF)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"fields\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("กรุณาระบุชื่อแบบฟอร์ม"));
    }

    @Test
    @DisplayName("API-03 ไม่ส่ง X-User-Id ได้ 401 UNAUTHORIZED")
    void createFormWithoutLogin() throws Exception {
        when(service.create(any(), isNull())).thenThrow(ApiException.unauthorized());

        mvc.perform(post("/api/forms").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("API-04 นักศึกษาพยายามสร้างแบบฟอร์ม ได้ 403 FORBIDDEN")
    void createFormByStudent() throws Exception {
        when(service.create(any(), eq(3L))).thenThrow(ApiException.forbidden());

        mvc.perform(post("/api/forms").header("X-User-Id", "3")
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("API-05 ดูรายการแบบฟอร์ม ได้ 200")
    void listForms() throws Exception {
        when(service.getAll(1L)).thenReturn(List.of(form(FormStatus.PUBLISHED)));

        mvc.perform(get("/api/forms").header("X-User-Id", STAFF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("API-06 แก้ไขแบบฟอร์มที่เปิดใช้งานแล้ว ได้ 409 INVALID_STATUS")
    void updatePublishedForm() throws Exception {
        when(service.update(eq(10L), any(), eq(1L)))
                .thenThrow(ApiException.conflict("แก้ไขได้เฉพาะแบบฟอร์มที่ยังเป็นฉบับร่าง"));

        mvc.perform(put("/api/forms/10").header("X-User-Id", STAFF)
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS"));
    }

    @Test
    @DisplayName("API-07 เปิดใช้งานแบบฟอร์ม ได้ 200 สถานะ PUBLISHED")
    void publishForm() throws Exception {
        when(service.publish(10L, 1L)).thenReturn(form(FormStatus.PUBLISHED));

        mvc.perform(post("/api/forms/10/publish").header("X-User-Id", STAFF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("API-08 ดูแบบฟอร์มที่ไม่มีอยู่ ได้ 404 NOT_FOUND")
    void formNotFound() throws Exception {
        when(service.getById(999L, 1L)).thenThrow(ApiException.notFound("ไม่พบแบบฟอร์ม"));

        mvc.perform(get("/api/forms/999").header("X-User-Id", STAFF))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("API-09 body ไม่ใช่ JSON ได้ 400 และไม่เรียก service")
    void invalidJson() throws Exception {
        mvc.perform(post("/api/forms").header("X-User-Id", STAFF)
                .contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verify(service, never()).create(any(), any());
    }

    @Test
    @DisplayName("API-22 สร้างฉบับใหม่จากแบบฟอร์มเดิม ได้ 201 สถานะ DRAFT")
    void newVersion() throws Exception {
        when(service.newVersion(10L, 1L)).thenReturn(form(FormStatus.DRAFT));

        mvc.perform(post("/api/forms/10/new-version").header("X-User-Id", STAFF))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }
}
