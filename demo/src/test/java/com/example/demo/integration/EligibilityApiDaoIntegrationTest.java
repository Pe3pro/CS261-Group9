package com.example.demo.integration;

import com.example.demo.dao.EligibilityRecordDao;
import com.example.demo.entity.EligibilityRecord;
import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import static org.hamcrest.Matchers.hasItem;

import static org.springframework.test.web.servlet.request
    .MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result
    .MockMvcResultMatchers.jsonPath;

import static org.springframework.test.web.servlet.result
    .MockMvcResultMatchers.status;

@SpringBootTest(
    properties = {
        "spring.datasource.url=" +
            "jdbc:h2:mem:t18db;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
    }
)
@AutoConfigureMockMvc
class EligibilityApiDaoIntegrationTest {

    private static final String ENDPOINT =
        "/api/eligibility/check";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EligibilityRecordDao dao;

    /*
     * ล้างฐานข้อมูลก่อนแต่ละ Test Case
     * เพื่อให้แต่ละกรณีไม่กระทบกัน
     */
    @BeforeEach
    void clearDatabase() {
        dao.deleteAll();
        dao.flush();
    }

    /*
     * API-DAO-01:
     * ข้อมูลถูกต้องและผ่านคุณสมบัติ
     */
    @Test
    void shouldSaveEligibleRecordWhenRequestPasses()
            throws Exception {

        long beforeCount = dao.count();

        mockMvc.perform(
                post(ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRequest("3.10"))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.eligible").value(true))
            .andExpect(
                jsonPath("$.message")
                    .value("ผ่านคุณสมบัติ")
            );

        long afterCount = dao.count();

        assertThat(afterCount).isEqualTo(beforeCount + 1);

        List<EligibilityRecord> records =
            dao.findByStudentIdOrderByCheckedAtDesc(
                "6410000001"
            );

        assertThat(records).hasSize(1);

        EligibilityRecord saved = records.get(0);

        assertThat(saved.getStudentId())
            .isEqualTo("6410000001");

        assertThat(saved.isEligible()).isTrue();

        assertThat(saved.getMessage())
            .isEqualTo("ผ่านคุณสมบัติ");

        assertThat(saved.getCheckedAt()).isNotNull();
    }

    /*
     * API-DAO-02:
     * ข้อมูลถูกต้องแต่ไม่ผ่านคุณสมบัติ
     */
    @Test
    void shouldSaveNotEligibleRecordWhenGpaxIsLow()
            throws Exception {

        long beforeCount = dao.count();

        mockMvc.perform(
                post(ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRequest("2.74"))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.eligible").value(false))
            .andExpect(
                jsonPath("$.message")
                    .value("ไม่ผ่านคุณสมบัติ")
            )
            .andExpect(
                jsonPath("$.reasons")
                    .value(
                        hasItem(
                            "GPA ต้องไม่ต่ำกว่า 2.75"
                        )
                    )
            );

        long afterCount = dao.count();

        assertThat(afterCount).isEqualTo(beforeCount + 1);

        List<EligibilityRecord> records =
            dao.findByStudentIdOrderByCheckedAtDesc(
                "6410000001"
            );

        assertThat(records).hasSize(1);

        EligibilityRecord saved = records.get(0);

        assertThat(saved.isEligible()).isFalse();

        assertThat(saved.getMessage())
            .isEqualTo("ไม่ผ่านคุณสมบัติ");
    }

    /*
     * API-DAO-03:
     * กรอกข้อมูลไม่ครบ
     * ต้องตอบ HTTP 400 และฐานข้อมูลต้องไม่เพิ่ม
     */
    @Test
    void shouldNotSaveWhenRequestIsIncomplete()
            throws Exception {

        long beforeCount = dao.count();

        mockMvc.perform(
                post(ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.eligible").isEmpty())
            .andExpect(
                jsonPath("$.message")
                    .value("ข้อมูลไม่ถูกต้อง")
            )
            .andExpect(jsonPath("$.errors").isNotEmpty());

        long afterCount = dao.count();

        assertThat(afterCount).isEqualTo(beforeCount);
        assertThat(dao.findAll()).isEmpty();
    }

    /*
     * API-DAO-04:
     * กรอกข้อมูลผิดรูปแบบ
     * ต้องตอบ HTTP 400 และฐานข้อมูลต้องไม่เพิ่ม
     */
    @Test
    void shouldNotSaveWhenRequestFormatIsInvalid()
            throws Exception {

        long beforeCount = dao.count();

        mockMvc.perform(
                post(ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest())
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.eligible").isEmpty())
            .andExpect(
                jsonPath("$.message")
                    .value("ข้อมูลไม่ถูกต้อง")
            )
            .andExpect(
                jsonPath("$.errors")
                    .value(
                        hasItem(
                            "รหัสนักศึกษาต้องเป็นตัวเลข 10 หลัก"
                        )
                    )
            )
            .andExpect(
                jsonPath("$.errors")
                    .value(
                        hasItem(
                            "GPA ต้องเป็นตัวเลข 0.00–4.00 " +
                            "และมีทศนิยมไม่เกิน 2 ตำแหน่ง"
                        )
                    )
            )
            .andExpect(
                jsonPath("$.errors")
                    .value(
                        hasItem(
                            "เกรดของวิชา คพ.101 ไม่ถูกต้อง"
                        )
                    )
            )
            .andExpect(
                jsonPath("$.errors")
                    .value(
                        hasItem(
                            "สถานะของวิชา คพ.384 ไม่ถูกต้อง"
                        )
                    )
            );

        long afterCount = dao.count();

        assertThat(afterCount).isEqualTo(beforeCount);
        assertThat(dao.findAll()).isEmpty();
    }

    /*
     * API-DAO-05:
     * Request ที่ถูกต้องหนึ่งครั้ง
     * ต้องบันทึกเพียงหนึ่งรายการ
     */
    @Test
    void shouldSaveExactlyOneRecordPerRequest()
            throws Exception {

        long beforeCount = dao.count();

        mockMvc.perform(
                post(ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRequest("3.10"))
            )
            .andExpect(status().isOk());

        long afterCount = dao.count();

        assertThat(afterCount - beforeCount).isEqualTo(1);
    }

    /*
     * API-DAO-06:
     * ค่า eligible และ message ในฐานข้อมูล
     * ต้องตรงกับ API Response
     */
    @Test
    void shouldSaveRecordMatchingApiResponse()
            throws Exception {

        MvcResult result =
            mockMvc.perform(
                    post(ENDPOINT)
                        .contentType(
                            MediaType.APPLICATION_JSON
                        )
                        .content(validRequest("3.10"))
                )
                .andExpect(status().isOk())
                .andReturn();

        String responseJson =
            result.getResponse()
                .getContentAsString();

        Boolean apiEligible =
            JsonPath.read(
                responseJson,
                "$.eligible"
            );

        String apiMessage =
            JsonPath.read(
                responseJson,
                "$.message"
            );

        List<EligibilityRecord> records =
            dao.findByStudentIdOrderByCheckedAtDesc(
                "6410000001"
            );

        assertThat(records).hasSize(1);

        EligibilityRecord saved = records.get(0);

        assertThat(saved.getStudentId())
            .isEqualTo("6410000001");

        assertThat(saved.isEligible())
            .isEqualTo(apiEligible);

        assertThat(saved.getMessage())
            .isEqualTo(apiMessage);

        assertThat(saved.getCheckedAt()).isNotNull();
    }

    /*
     * JSON มาตรฐานสำหรับข้อมูลที่ถูกต้อง
     */
    private String validRequest(String gpax) {
        return """
            {
              "studentId": "6410000001",
              "curriculum": "61",
              "gpax": "%s",
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
            """.formatted(gpax);
    }

    /*
     * JSON สำหรับกรณีข้อมูลผิดรูปแบบ
     */
    private String invalidRequest() {
        return """
            {
              "studentId": "ABC123",
              "curriculum": "61",
              "gpax": "3.10abc",
              "grades": {
                "คพ.101": "Z",
                "คพ.102": "B",
                "คพ.111": "B",
                "คพ.213": "B",
                "คพ.251": "B",
                "คพ.264": "B"
              },
              "courseStatuses": {
                "คพ.384": "เรียนอยู่",
                "คพ.266": "กำลังศึกษา"
              },
              "acknowledgements": [
                true,
                true,
                true
              ]
            }
            """;
    }
}