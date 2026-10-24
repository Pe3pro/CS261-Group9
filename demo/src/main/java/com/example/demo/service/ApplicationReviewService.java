package com.example.demo.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dao.ApplicationRepository;
import com.example.demo.dao.ApprovalRepository;
import com.example.demo.dao.DocumentRepository;
import com.example.demo.dao.FormFieldRepository;
import com.example.demo.dao.FormResponseRepository;
import com.example.demo.dao.FormTemplateRepository;
import com.example.demo.dao.StudentRepository;
import com.example.demo.dao.UserRepository;
import com.example.demo.dto.AnswerResponse;
import com.example.demo.dto.ApplicationDetailResponse;
import com.example.demo.dto.ApplicationReviewResponse;
import com.example.demo.dto.ApprovalResponse;
import com.example.demo.dto.DocumentResponse;
import com.example.demo.entity.Application;
import com.example.demo.entity.ApplicationStatus;
import com.example.demo.entity.Approval;
import com.example.demo.entity.FormField;
import com.example.demo.entity.FormTemplate;
import com.example.demo.entity.Role;
import com.example.demo.entity.Student;
import com.example.demo.entity.User;

/** U5: เจ้าหน้าที่ / อาจารย์ ดูคำร้องที่นักศึกษายื่นเข้ามา (อาจารย์เห็นเฉพาะนักศึกษาที่ตนเป็นอาจารย์ที่ปรึกษา) */
@Service
@Transactional(readOnly = true)
public class ApplicationReviewService {

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final FormTemplateRepository templateRepository;
    private final FormFieldRepository fieldRepository;
    private final FormResponseRepository responseRepository;
    private final ApprovalRepository approvalRepository;
    private final AccessGuard guard;

    public ApplicationReviewService(ApplicationRepository applicationRepository, StudentRepository studentRepository,
            UserRepository userRepository, DocumentRepository documentRepository,
            FormTemplateRepository templateRepository, FormFieldRepository fieldRepository,
            FormResponseRepository responseRepository, ApprovalRepository approvalRepository, AccessGuard guard) {
        this.applicationRepository = applicationRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.templateRepository = templateRepository;
        this.fieldRepository = fieldRepository;
        this.responseRepository = responseRepository;
        this.approvalRepository = approvalRepository;
        this.guard = guard;
    }

    public List<ApplicationReviewResponse> getByStatus(ApplicationStatus status, Long userId) {
        User viewer = guard.require(userId, Role.STAFF, Role.ADVISOR, Role.ADMIN);
        if (status == null) {
            throw ApiException.badRequest("กรุณาระบุสถานะคำร้อง");
        }
        return applicationRepository.findByStatusOrderBySubmittedAtAsc(status).stream().map(app -> {
            Student student = studentRepository.findById(app.getStudentId()).orElse(null);
            if (!canSee(viewer, student)) {
                return null;
            }
            List<Approval> history = approvalRepository.findByApplicationIdOrderByIdAsc(app.getId());
            Approval last = history.isEmpty() ? null : history.get(history.size() - 1);
            String reviewedBy = last == null ? null
                    : userRepository.findById(last.getApproverId()).map(User::getFullName).orElse(null);
            return new ApplicationReviewResponse(app.getId(), studentCode(student), studentName(student),
                    app.getStatus(), app.getSubmittedAt(), documents(app.getId()), reviewedBy,
                    last == null ? null : last.getDecidedAt());
        }).filter(Objects::nonNull).toList();
    }

    public ApplicationDetailResponse getDetail(Long applicationId, Long userId) {
        User viewer = guard.require(userId, Role.STAFF, Role.ADVISOR, Role.ADMIN);
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> ApiException.notFound("ไม่พบคำร้อง"));
        Student student = studentRepository.findById(app.getStudentId()).orElse(null);
        if (!canSee(viewer, student)) {
            throw new ApiException(403, "FORBIDDEN", "คุณไม่ได้เป็นอาจารย์ที่ปรึกษาของนักศึกษาเจ้าของคำร้องนี้");
        }
        User studentUser = student == null ? null : userRepository.findById(student.getUserId()).orElse(null);
        String formName = templateRepository.findById(app.getFormTemplateId())
                .map(FormTemplate::getName).orElse(null);

        Map<Long, FormField> fields = fieldRepository
                .findByFormTemplateIdOrderByDisplayOrderAsc(app.getFormTemplateId()).stream()
                .collect(Collectors.toMap(FormField::getId, Function.identity()));
        List<AnswerResponse> answers = responseRepository.findByApplicationId(app.getId()).stream()
                .filter(r -> fields.containsKey(r.getFormFieldId()))
                .sorted(Comparator.comparing(r -> fields.get(r.getFormFieldId()).getDisplayOrder()))
                .map(r -> new AnswerResponse(fields.get(r.getFormFieldId()).getLabel(), r.getValue()))
                .toList();

        List<ApprovalResponse> approvals = approvalRepository.findByApplicationIdOrderByIdAsc(app.getId()).stream()
                .map(a -> new ApprovalResponse(a.getId(), a.getApplicationId(), a.getApprovalLevel(),
                        a.getDecision(), userRepository.findById(a.getApproverId()).map(User::getFullName).orElse(null),
                        a.getComment(), a.getDecidedAt(), app.getStatus()))
                .toList();

        return new ApplicationDetailResponse(app.getId(), studentCode(student),
                studentUser == null ? null : studentUser.getFullName(),
                studentUser == null ? null : studentUser.getEmail(),
                student == null ? null : student.getFaculty(),
                student == null ? null : student.getMajor(),
                student == null ? null : student.getGpa(),
                formName, app.getStatus(), app.getSubmittedAt(), answers, documents(app.getId()), approvals);
    }

    /** เจ้าหน้าที่ / ผู้ดูแลระบบเห็นทุกคำร้อง · อาจารย์เห็นเฉพาะนักศึกษาที่ตนเป็นอาจารย์ที่ปรึกษา */
    static boolean canSee(User viewer, Student student) {
        if (viewer.getRole() != Role.ADVISOR) {
            return true;
        }
        return student != null && viewer.getId().equals(student.getAdvisorId());
    }

    private List<DocumentResponse> documents(Long applicationId) {
        return documentRepository.findByApplicationId(applicationId).stream().map(DocumentResponse::from).toList();
    }

    private String studentCode(Student student) {
        return student == null ? null : student.getStudentId();
    }

    private String studentName(Student student) {
        if (student == null) return null;
        return userRepository.findById(student.getUserId()).map(User::getFullName).orElse(null);
    }
}
