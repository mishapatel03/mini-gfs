package com.minigfs.master_server.service;

import com.minigfs.master_server.entity.*;
import com.minigfs.master_server.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
public class FileService {

    @Value("${minigfs.chunk-size}")
    private long chunkSize;

    @Value("${minigfs.replication-factor}")
    private int replicationFactor;

    private final FileRepository fileRepository;
    private final ChunkRepository chunkRepository;
    private final NodeRepository nodeRepository;
    private final ChunkLocationRepository chunkLocationRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public FileService(FileRepository fileRepository,
                        ChunkRepository chunkRepository,
                        NodeRepository nodeRepository,
                        ChunkLocationRepository chunkLocationRepository) {
        this.fileRepository = fileRepository;
        this.chunkRepository = chunkRepository;
        this.nodeRepository = nodeRepository;
        this.chunkLocationRepository = chunkLocationRepository;
    }

    public FileEntity uploadFile(MultipartFile multipartFile) throws IOException {
        FileEntity file = new FileEntity();
        file.setName(multipartFile.getOriginalFilename());
        file.setSize(multipartFile.getSize());
        file.setChunkSize(chunkSize);
        file = fileRepository.save(file);

        List<NodeEntity> healthyNodes = nodeRepository.findAll().stream()
                .filter(n -> n.getStatus() == NodeEntity.NodeStatus.HEALTHY)
                .toList();

        if (healthyNodes.isEmpty()) {
            throw new IllegalStateException("No healthy nodes available to store chunks");
        }

        int chunkNumber = 0;
        try (InputStream inputStream = multipartFile.getInputStream()) {
            byte[] buffer = new byte[(int) chunkSize];
            int bytesRead;

            while ((bytesRead = inputStream.readNBytes(buffer, 0, buffer.length)) > 0) {
                byte[] chunkData = Arrays.copyOf(buffer, bytesRead);

                List<NodeEntity> targetNodes = pickNodesForChunk(healthyNodes, chunkNumber);

                ChunkEntity chunk = new ChunkEntity();
                chunk.setFile(file);
                chunk.setChunkNumber(chunkNumber);
                chunk.setSize(chunkData.length);

                String checksum = null;
                for (NodeEntity node : targetNodes) {
                    checksum = pushChunkToNode(node, file.getId() + "-chunk-" + chunkNumber, chunkData);
                }
                chunk.setChecksum(checksum);
                chunk = chunkRepository.save(chunk);

                for (NodeEntity node : targetNodes) {
                    saveChunkLocation(chunk, node);
                }

                chunkNumber++;
            }
        }

        return file;
    }

    public byte[] downloadFile(UUID fileId) throws NoSuchAlgorithmException {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new NoSuchElementException("File not found: " + fileId));

        List<ChunkEntity> chunks = chunkRepository.findAll().stream()
                .filter(c -> c.getFile().getId().equals(fileId))
                .sorted(Comparator.comparingInt(ChunkEntity::getChunkNumber))
                .toList();

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        for (ChunkEntity chunk : chunks) {
            List<ChunkLocationEntity> locations = chunkLocationRepository.findAll().stream()
                    .filter(loc -> loc.getChunk().getId().equals(chunk.getId()))
                    .toList();

            byte[] chunkData = fetchChunkFromAnyReplica(file.getId(), chunk, locations);
            output.writeBytes(chunkData);
        }

        return output.toByteArray();
    }

    private byte[] fetchChunkFromAnyReplica(UUID fileId, ChunkEntity chunk, List<ChunkLocationEntity> locations) throws NoSuchAlgorithmException {
        String chunkId = fileId + "-chunk-" + chunk.getChunkNumber();

        for (ChunkLocationEntity location : locations) {
            NodeEntity node = location.getNode();
            if (node.getStatus() != NodeEntity.NodeStatus.HEALTHY) {
                continue; // skip dead replicas, try the next one
            }

            try {
                String url = "http://" + node.getHost() + ":" + node.getPort() + "/chunks/" + chunkId;
                byte[] data = restTemplate.getForObject(url, byte[].class);

                if (data != null && verifyChecksum(data, chunk.getChecksum())) {
                    return data;
                } else {
                    System.err.println("[FileService] Checksum mismatch for chunk " + chunkId
                            + " on " + node.getId() + ", trying next replica");
                }
            } catch (Exception e) {
                System.err.println("[FileService] Failed to fetch chunk " + chunkId
                        + " from " + node.getId() + ": " + e.getMessage() + ", trying next replica");
            }
        }

        throw new RuntimeException("Could not retrieve chunk " + chunkId + " from any healthy replica");
    }

    private boolean verifyChecksum(byte[] data, String expectedChecksum) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString().equals(expectedChecksum);
    }

    private List<NodeEntity> pickNodesForChunk(List<NodeEntity> healthyNodes, int chunkNumber) {
        int count = Math.min(replicationFactor, healthyNodes.size());
        List<NodeEntity> selected = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int index = (chunkNumber + i) % healthyNodes.size();
            selected.add(healthyNodes.get(index));
        }
        return selected;
    }

    private String pushChunkToNode(NodeEntity node, String chunkId, byte[] data) {
        String url = "http://" + node.getHost() + ":" + node.getPort() + "/chunks/" + chunkId;

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
        org.springframework.http.HttpEntity<byte[]> request =
                new org.springframework.http.HttpEntity<>(data, headers);

        Map response = restTemplate.exchange(url, org.springframework.http.HttpMethod.PUT,
                request, Map.class).getBody();

        return response != null ? (String) response.get("checksum") : null;
    }

    private void saveChunkLocation(ChunkEntity chunk, NodeEntity node) {
        ChunkLocationEntity location = new ChunkLocationEntity();
        location.setId(new ChunkLocationEntity.ChunkLocationId(chunk.getId(), node.getId()));
        location.setChunk(chunk);
        location.setNode(node);
        chunkLocationRepository.save(location);
    }
}
