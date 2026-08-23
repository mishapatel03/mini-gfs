package com.minigfs.master_server.controller;

import com.minigfs.master_server.entity.FileEntity;
import com.minigfs.master_server.repository.FileRepository;
import com.minigfs.master_server.service.FileService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    private final FileRepository fileRepository;

    public FileController(FileService fileService, FileRepository fileRepository) {
        this.fileService = fileService;
        this.fileRepository = fileRepository;
    }

    @PostMapping
    public ResponseEntity<FileEntity> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        FileEntity uploaded = fileService.uploadFile(file);
        return ResponseEntity.ok(uploaded);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileEntity> getFile(@PathVariable UUID id) {
        return fileRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadFile(@PathVariable UUID id) throws NoSuchAlgorithmException {
        FileEntity file = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found: " + id));

        byte[] data = fileService.downloadFile(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(file.getName()).build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}
