package com.minigfs.chunkserver.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/chunks")
public class ChunkController {

    @Value("${chunk.storage.dir}")
    private String storageDir;

    private Path resolveChunkPath(String chunkId) throws IOException {
        Path dir = Paths.get(storageDir);
        Files.createDirectories(dir);
        return dir.resolve(chunkId);
    }

    @PutMapping("/{chunkId}")
    public ResponseEntity<Map<String, Object>> putChunk(
            @PathVariable String chunkId,
            @RequestBody byte[] data) throws IOException, NoSuchAlgorithmException {

        Path path = resolveChunkPath(chunkId);
        Files.write(path, data);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("chunkId", chunkId);
        response.put("checksum", hex.toString());
        response.put("size", data.length);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{chunkId}")
    public ResponseEntity<Resource> getChunk(@PathVariable String chunkId) throws IOException {
        Path path = resolveChunkPath(chunkId);
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/{chunkId}")
    public ResponseEntity<Void> deleteChunk(@PathVariable String chunkId) throws IOException {
        Path path = resolveChunkPath(chunkId);
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        Files.delete(path);
        return ResponseEntity.noContent().build();
    }
}
