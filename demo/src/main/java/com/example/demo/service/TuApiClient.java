package com.example.demo.service;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.example.demo.dto.TuVerifyResponse;

/**
 * U1: เรียก TU REST API ของมหาวิทยาลัยเพื่อตรวจ Username / Password
 * Application-Key อ่านจาก environment variable TU_API_KEY (ดู application.properties) ห้าม commit ค่าจริง
 */
@Component
public class TuApiClient {

    static final String VERIFY_PATH = "/api/v1/auth/Ad/verify";
    static final String UNAVAILABLE_MESSAGE = "ไม่สามารถเชื่อมต่อระบบของมหาวิทยาลัยได้ กรุณาลองใหม่อีกครั้ง";

    private final RestClient restClient;
    private final String applicationKey;

    @Autowired
    public TuApiClient(@Value("${tu.api.base-url:https://restapi.tu.ac.th}") String baseUrl,
            @Value("${tu.api.key:}") String applicationKey) {
        this(RestClient.builder().requestFactory(timeouts()), baseUrl, applicationKey);
    }

    /** ใช้ใน test: ส่ง builder ที่ผูกกับ MockRestServiceServer เข้ามา */
    TuApiClient(RestClient.Builder builder, String baseUrl, String applicationKey) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.applicationKey = applicationKey == null ? "" : applicationKey.trim();
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return factory;
    }

    /**
     * ตรวจบัญชี มธ.
     * คืน status = true เมื่อถูกต้อง, status = false เมื่อ username/password ผิด
     * โยน ApiException 502 TU_API_UNAVAILABLE เมื่อเชื่อมต่อไม่ได้หรือยังไม่ได้ตั้งค่า Application-Key
     */
    public TuVerifyResponse verify(String username, String password) {
        if (applicationKey.isEmpty()) {
            throw unavailable();
        }
        try {
            TuVerifyResponse body = restClient.post()
                    .uri(VERIFY_PATH)
                    .header("Application-Key", applicationKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("UserName", username, "PassWord", password))
                    .retrieve()
                    .body(TuVerifyResponse.class);
            return body != null ? body : TuVerifyResponse.invalid(null);
        } catch (HttpClientErrorException e) {
            // TU REST API ตอบ 400 พร้อม status:false เมื่อ username หรือ password ไม่ถูกต้อง
            if (e.getStatusCode().value() == 400) {
                return TuVerifyResponse.invalid(null);
            }
            throw unavailable(); // 401/403 = Application-Key ไม่ถูกต้อง เป็นปัญหาฝั่งระบบ ไม่ใช่ของผู้ใช้
        } catch (RestClientException e) {
            throw unavailable(); // 5xx, timeout, เชื่อมต่อไม่ได้, JSON ผิดรูปแบบ
        }
    }

    static ApiException unavailable() {
        return new ApiException(502, "TU_API_UNAVAILABLE", UNAVAILABLE_MESSAGE);
    }
}