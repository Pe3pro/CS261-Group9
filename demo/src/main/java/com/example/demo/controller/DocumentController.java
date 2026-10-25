package com.example.demo.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.DocumentService;
import com.example.demo.service.DocumentService.FileContent;

/**
 * U3/U5: เปิดไฟล์ PDF ที่แนบกับคำร้อง
 * หน้าเว็บเปิดไฟล์ด้วย window.open ซึ่งใส่ header ไม่ได้ จึงรับ userId จาก query string ได้ด้วย
 */
@RestController
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/api/documents/{id}")
    public ResponseEntity<byte[]> open(@PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "userId", required = false) Long queryUserId) {
        FileContent file = documentService.download(id, headerUserId != null ? headerUserId : queryUserId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(file.content());
    }
}
