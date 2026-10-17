package com.example.demo.service;

import com.example.demo.dto.EligibilityRequest;
import com.example.demo.dto.EligibilityResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class EligibilityService {

    private static final Map<String, Double> GRADE_POINTS = Map.of(
        "A", 4.0,
        "B+", 3.5,
        "B", 3.0,
        "C+", 2.5,
        "C", 2.0,
        "D+", 1.5,
        "D", 1.0,
        "F", 0.0
    );

    private static final Set<String> ALLOWED_STATUSES = Set.of(
        "ยังไม่เคยลง",
        "กำลังศึกษา",
        "เคยศึกษาแล้ว"
    );

    public EligibilityResponse check(EligibilityRequest request) {
        List<String> errors = validate(request);

        // ข้อมูลไม่ครบหรือผิดรูปแบบ
        if (!errors.isEmpty()) {
            return new EligibilityResponse(
                false,
                null,
                "ข้อมูลไม่ถูกต้อง",
                errors,
                List.of(),
                List.of()
            );
        }

        List<String> reasons = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        double gpax = Double.parseDouble(request.gpax());

        // ตรวจ GPA
        if (gpax < 2.75) {
            reasons.add("GPA ต้องไม่ต่ำกว่า 2.75");
        }

        // ตรวจเกรดรายวิชา
        request.grades().forEach((course, grade) -> {
            if (GRADE_POINTS.get(grade) < 2.5) {
                reasons.add(course + " ต้องได้เกรด C+ ขึ้นไป");
            }
        });

        // ตรวจสถานะรายวิชา
        request.courseStatuses().forEach((course, status) -> {
            if ("ยังไม่เคยลง".equals(status)) {
                reasons.add(
                    course + " ต้องกำลังศึกษา หรือเคยศึกษาแล้ว"
                );
            }
        });

        // ตรวจ checkbox ยืนยัน
        boolean acknowledgedAll =
            request.acknowledgements()
                   .stream()
                   .allMatch(Boolean.TRUE::equals);

        if (!acknowledgedAll) {
            reasons.add("ต้องยืนยันเงื่อนไขให้ครบทั้ง 3 ข้อ");
        }

        // เพิ่มคำเตือนตามหลักสูตร
        if ("61".equals(request.curriculum())) {
            warnings.add(
                "ต้องสอบได้วิชา คพ.302 ก่อนออกปฏิบัติงาน"
            );
        } else {
            warnings.add(
                "ต้องสอบได้วิชา คพ.301 ก่อนออกปฏิบัติงาน"
            );
        }

        boolean eligible = reasons.isEmpty();

        return new EligibilityResponse(
            true,
            eligible,
            eligible
                ? "ผ่านคุณสมบัติ"
                : "ไม่ผ่านคุณสมบัติ",
            List.of(),
            reasons,
            warnings
        );
    }

    /*
     * ตรวจว่าข้อมูลที่ API รับมาครบและมีรูปแบบถูกต้องหรือไม่
     */
    private List<String> validate(EligibilityRequest request) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add("กรุณาส่งข้อมูลสำหรับตรวจสอบ");
            return errors;
        }

        validateStudentId(request, errors);
        validateCurriculum(request, errors);
        validateGpax(request, errors);
        validateGrades(request, errors);
        validateCourseStatuses(request, errors);
        validateAcknowledgements(request, errors);

        return errors;
    }

    private void validateStudentId(
        EligibilityRequest request,
        List<String> errors
    ) {
        if (isBlank(request.studentId())) {
            errors.add("กรุณากรอกรหัสนักศึกษา");
        } else if (!request.studentId().matches("\\d{10}")) {
            errors.add(
                "รหัสนักศึกษาต้องเป็นตัวเลข 10 หลัก"
            );
        }
    }

    private void validateCurriculum(
        EligibilityRequest request,
        List<String> errors
    ) {
        boolean validCurriculum =
            "61".equals(request.curriculum()) ||
            "66".equals(request.curriculum());

        if (!validCurriculum) {
            errors.add("หลักสูตรต้องเป็น 61 หรือ 66");
        }
    }

    private void validateGpax(
        EligibilityRequest request,
        List<String> errors
    ) {
        if (isBlank(request.gpax())) {
            errors.add("กรุณากรอก GPA");
            return;
        }

        /*
         * รับค่า 0–3 พร้อมทศนิยมไม่เกิน 2 ตำแหน่ง
         * หรือ 4, 4.0, 4.00
         */
        String gpaxPattern =
            "(?:[0-3](?:\\.\\d{1,2})?|4(?:\\.0{1,2})?)";

        if (!request.gpax().matches(gpaxPattern)) {
            errors.add(
                "GPA ต้องเป็นตัวเลข 0.00–4.00 " +
                "และมีทศนิยมไม่เกิน 2 ตำแหน่ง"
            );
        }
    }

    private void validateGrades(
        EligibilityRequest request,
        List<String> errors
    ) {
        if (
            request.grades() == null ||
            request.grades().isEmpty()
        ) {
            errors.add(
                "กรุณาส่งผลการเรียนรายวิชาบังคับ"
            );
            return;
        }

        List<String> fixedCourses;

        if ("61".equals(request.curriculum())) {
            fixedCourses = List.of(
                "คพ.101",
                "คพ.102",
                "คพ.111",
                "คพ.251",
                "คพ.264"
            );
        } else {
            fixedCourses = List.of(
                "คพ.100",
                "คพ.101",
                "คพ.102",
                "คพ.111",
                "คพ.251",
                "คพ.261"
            );
        }

        // ตรวจรายวิชาบังคับ
        for (String course : fixedCourses) {
            if (!request.grades().containsKey(course)) {
                errors.add(
                    "ไม่พบผลการเรียนวิชา " + course
                );
            }
        }

        // ต้องมี คพ.213 หรือ คพ.216 อย่างน้อยหนึ่งวิชา
        boolean hasAlternativeCourse =
            request.grades().containsKey("คพ.213") ||
            request.grades().containsKey("คพ.216");

        if (!hasAlternativeCourse) {
            errors.add(
                "ต้องส่งผลการเรียนวิชา คพ.213 " +
                "หรือ คพ.216 อย่างน้อย 1 วิชา"
            );
        }

        // ตรวจรูปแบบเกรด
        request.grades().forEach((course, grade) -> {
            if (!GRADE_POINTS.containsKey(grade)) {
                errors.add(
                    "เกรดของวิชา " +
                    course +
                    " ไม่ถูกต้อง"
                );
            }
        });
    }

    private void validateCourseStatuses(
        EligibilityRequest request,
        List<String> errors
    ) {
        if (
            request.courseStatuses() == null ||
            request.courseStatuses().isEmpty()
        ) {
            errors.add(
                "กรุณาส่งสถานะรายวิชาที่กำลังศึกษา " +
                "หรือเคยศึกษา"
            );
            return;
        }

        String fixedCourse;

        if ("61".equals(request.curriculum())) {
            fixedCourse = "คพ.384";
        } else {
            fixedCourse = "คพ.180";
        }

        if (
            !request.courseStatuses()
                    .containsKey(fixedCourse)
        ) {
            errors.add(
                "ไม่พบสถานะรายวิชา " + fixedCourse
            );
        }

        Set<String> alternativeCourses;

        if ("61".equals(request.curriculum())) {
            alternativeCourses = Set.of(
                "คพ.266",
                "คพ.322",
                "คพ.348"
            );
        } else {
            alternativeCourses = Set.of(
                "คพ.262",
                "คพ.331",
                "คพ.240"
            );
        }

        boolean hasAlternativeCourse =
            alternativeCourses.stream().anyMatch(
                request.courseStatuses()::containsKey
            );

        if (!hasAlternativeCourse) {
            errors.add(
                "ต้องส่งสถานะรายวิชาในกลุ่มวิชาเลือก" +
                "อย่างน้อย 1 วิชา"
            );
        }

        request.courseStatuses().forEach(
            (course, status) -> {
                if (!ALLOWED_STATUSES.contains(status)) {
                    errors.add(
                        "สถานะของวิชา " +
                        course +
                        " ไม่ถูกต้อง"
                    );
                }
            }
        );
    }

    private void validateAcknowledgements(
        EligibilityRequest request,
        List<String> errors
    ) {
        if (
            request.acknowledgements() == null ||
            request.acknowledgements().size() != 3
        ) {
            errors.add(
                "ต้องส่งข้อมูลการยืนยันให้ครบ 3 ข้อ"
            );
            return;
        }

        boolean containsNull =
            request.acknowledgements()
                   .stream()
                   .anyMatch(value -> value == null);

        if (containsNull) {
            errors.add(
                "ข้อมูลการยืนยันต้องเป็น true หรือ false"
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}