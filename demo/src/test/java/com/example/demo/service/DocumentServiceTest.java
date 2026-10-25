package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.demo.dao.ApplicationRepository;
import com.example.demo.dao.DocumentRepository;
import com.example.demo.dao.StudentRepository;
import com.example.demo.dto.DocumentResponse;
import com.example.demo.entity.Application;
import com.example.demo.entity.ApplicationStatus;
import com.example.demo.entity.Document;
import com.example.demo.entity.Student;

/**
 * U3:T8 Unit test ส่วน Service (DocumentService) ตาม T8.md U3-SVC-07
 * จำลองที่เก็บไฟล์ (FileStorage) และ Repository ด้วย Mockito
 */
class DocumentServiceTest {

    private RequestService requestService;
    private DocumentRepository documents;
    private FileStorage storage;
    private DocumentService service;

    @BeforeEach
    void setUp() {
        requestService = mock(RequestService.class);
        documents = mock(DocumentRepository.class);
        storage = mock(FileStorage.class);
        service = new DocumentService(requestService, mock(ApplicationRepository.class), documents,
                mock(StudentRepository.class), storage, mock(AccessGuard.class));

        Student a = new Student(3L, "6609650001", "วิทยาศาสตร์และเทคโนโลยี", "วิทยาการคอมพิวเตอร์", 3.10);
        ReflectionTestUtils.setField(a, "id", 1L);
        Application app = new Application(1L, 10L, ApplicationStatus.DRAFT);
        ReflectionTestUtils.setField(app, "id", 500L);
        when(requestService.currentStudent(3L)).thenReturn(a);
        when(requestService.ownedApplication(500L, a)).thenReturn(app);
        when(documents.findByApplicationId(500L)).thenReturn(List.of());
    }

    @Test
    @DisplayName("U3-SVC-07 ไฟล์นามสกุล .pdf แต่เนื้อไฟล์ไม่ใช่ PDF: ปฏิเสธ และไม่บันทึกลงที่เก็บไฟล์")
    void rejectFakePdf() {
        MockMultipartFile fake = new MockMultipartFile("file", "transcript.pdf", "application/pdf",
                "MZ this is really an exe".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> service.upload(500L, fake, 3L))
                .isInstanceOf(ApiException.class)
                .hasMessage("ไฟล์นี้ไม่ใช่ PDF ที่ถูกต้อง")
                .extracting("status", "code").containsExactly(400, "INVALID_FILE");
        verify(storage, never()).store(anyString(), any());
        verify(documents, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("U3-SVC-07 (เทียบ) PDF จริง: บันทึกลงที่เก็บไฟล์และบันทึก metadata")
    void acceptRealPdf() {
        MockMultipartFile pdf = new MockMultipartFile("file", "transcript.pdf", "application/pdf",
                "%PDF-1.4\n%test\n".getBytes(StandardCharsets.US_ASCII));
        when(storage.store(anyString(), any())).thenReturn("transcripts/x.pdf");
        when(documents.saveAndFlush(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            ReflectionTestUtils.setField(d, "id", 7L);
            return d;
        });

        DocumentResponse res = service.upload(500L, pdf, 3L);

        assertThat(res.id()).isEqualTo(7L);
        assertThat(res.fileName()).isEqualTo("transcript.pdf");
        verify(storage).store(anyString(), any());
    }

    @Test
    @DisplayName("U3-SVC-07 (เทียบ) ไฟล์ไม่ใช่ .pdf หรือใหญ่เกิน 10 MB: ปฏิเสธก่อนอ่านเนื้อไฟล์")
    void rejectWrongTypeOrSize() {
        MockMultipartFile jpg = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[10]);
        assertThatThrownBy(() -> service.upload(500L, jpg, 3L)).extracting("code").isEqualTo("INVALID_FILE");
        MockMultipartFile big = new MockMultipartFile("file", "transcript.pdf", "application/pdf",
                new byte[(int) DocumentService.MAX_SIZE + 1]);
        assertThatThrownBy(() -> service.upload(500L, big, 3L)).extracting("code").isEqualTo("FILE_TOO_LARGE");
        verify(storage, never()).store(anyString(), any());
    }
}
