package com.minigfs.master_server.service;

import com.minigfs.master_server.entity.*;
import com.minigfs.master_server.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
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
        // 1. Create the file metadata record
        FileEntity file = new FileEntity();
        file.setName(multipartFile.getOriginalFilename());
        file.setSize(multipartFile.getSize());
        file.setChunkSize(chunkSize);
        file = fileRepository.save(file);

        // 2. Get healthy nodes to distribute chunks across
        List<NodeEntity> healthyNodes = nodeRepository.findAll().stream()
                .filter(n -> n.getStatus() == NodeEntity.NodeStatus.HEALTHY)
                .toList();

        if (healthyNodes.isEmpty()) {
            throw new IllegalStateException("No healthy nodes available to store chunks");
        }

        // 3. Read the file in chunkSize-sized pieces
        int chunkNumber = 0;
        try (InputStream inputStream = multipartFile.getInputStream()) {
            byte[] buffer = new byte[(int) chunkSize];
            int bytesRead;

            while ((bytesRead = inputStream.readNBytes(buffer, 0, buffer.length)) > 0) {
                byte[] chunkData = Arrays.copyOf(buffer, bytesRead);

                // naive placement: round-robin through healthy nodes, wrapping around
                // TODO: improve with capacity-aware placement later
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
