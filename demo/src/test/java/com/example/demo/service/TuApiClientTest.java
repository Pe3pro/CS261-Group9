package com.example.demo.service;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.web.client.RestClient;

import com.example.demo.dto.TuVerifyResponse;

/** U1:T8 Unit test การเรียก TU REST API (จำลอง server ของมหาวิทยาลัยด้วย MockRestServiceServer) */
class TuApiClientTest {

    private static final String BASE = "https://tu.test";
    private static final String KEY = "TEST-KEY";

    private MockRestServiceServer server;
    private TuApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TuApiClient(builder, BASE, KEY);
    }

    @Test
    @DisplayName("TU-01 ส่ง Application-Key และ UserName/PassWord ถูกต้อง แล้วอ่านผล status true")
    void success() {
        server.expect(requestTo(BASE + "/api/v1/auth/Ad/verify"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Application-Key", KEY))
                .andExpect(jsonPath("$.UserName").value("6609650099"))
                .andExpect(jsonPath("$.PassWord").value("secret"))
                .andRespond(withSuccess("""
                        {"status":true,"message":"Success","type":"student","username":"6609650099",
                         "tu_status":"ปกติ","statusid":"10","displayname_th":"ทดสอบ ระบบ","displayname_en":"Test System",
                         "email":"6609650099@dome.tu.ac.th","department":"วิทยาการคอมพิวเตอร์","faculty":"คณะวิทยาศาสตร์และเทคโนโลยี"}
                        """, MediaType.APPLICATION_JSON));

        TuVerifyResponse r = client.verify("6609650099", "secret");

        assertThat(r.status()).isTrue();
        assertThat(r.type()).isEqualTo("student");
        assertThat(r.displayname_th()).isEqualTo("ทดสอบ ระบบ");
        assertThat(r.department()).isEqualTo("วิทยาการคอมพิวเตอร์");
        server.verify();
    }

    @Test
    @DisplayName("TU-02 รหัสผ่านผิด (TU ตอบ 400 status false) คืน status false ไม่โยน error")
    void wrongPassword() {
        server.expect(requestTo(BASE + "/api/v1/auth/Ad/verify"))
                .andRespond(withBadRequest().contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":false,\"message\":\"Password Incorrect\"}"));

        assertThat(client.verify("6609650099", "wrong").status()).isFalse();
    }

    @Test
    @DisplayName("TU-03 TU ตอบ 500: โยน 502 TU_API_UNAVAILABLE")
    void serverError() {
        server.expect(requestTo(BASE + "/api/v1/auth/Ad/verify")).andRespond(withServerError());

        assertThatThrownBy(() -> client.verify("6609650099", "secret")).isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    assertThat(((ApiException) e).getStatus()).isEqualTo(502);
                    assertThat(((ApiException) e).getCode()).isEqualTo("TU_API_UNAVAILABLE");
                });
    }

    @Test
    @DisplayName("TU-04 Application-Key ไม่ถูกต้อง (TU ตอบ 401): โยน 502 ไม่ใช่ความผิดของผู้ใช้")
    void badKey() {
        server.expect(requestTo(BASE + "/api/v1/auth/Ad/verify")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.verify("6609650099", "secret")).isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(502);
    }

    @Test
    @DisplayName("TU-05 เชื่อมต่อไม่ได้ (network error): โยน 502")
    void networkError() {
        server.expect(requestTo(BASE + "/api/v1/auth/Ad/verify")).andRespond(request -> {
            throw new IOException("connection refused");
        });

        assertThatThrownBy(() -> client.verify("6609650099", "secret")).isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(502);
    }

    @Test
    @DisplayName("TU-06 ยังไม่ได้ตั้งค่า TU_API_KEY: โยน 502 โดยไม่ส่ง request")
    void missingKey() {
        TuApiClient noKey = new TuApiClient(RestClient.builder(), BASE, "  ");

        assertThatThrownBy(() -> noKey.verify("6609650099", "secret")).isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(502);
    }
}