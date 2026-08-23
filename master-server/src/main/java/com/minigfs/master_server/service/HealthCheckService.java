package com.minigfs.master_server.service;

import com.minigfs.master_server.entity.*;
import com.minigfs.master_server.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

@Service
public class HealthCheckService {

    private static final long TIMEOUT_SECONDS = 10;

    @Value("${minigfs.replication-factor}")
    private int replicationFactor;

    private final NodeRepository nodeRepository;
    private final ChunkRepository chunkRepository;
    private final ChunkLocationRepository chunkLocationRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public HealthCheckService(NodeRepository nodeRepository,
                               ChunkRepository chunkRepository,
                               ChunkLocationRepository chunkLocationRepository) {
        this.nodeRepository = nodeRepository;
        this.chunkRepository = chunkRepository;
        this.chunkLocationRepository = chunkLocationRepository;
    }

    @Scheduled(fixedRate = 5000)
    public void checkNodeHealth() {
        List<NodeEntity> nodes = nodeRepository.findAll();
        Instant cutoff = Instant.now().minusSeconds(TIMEOUT_SECONDS);

        for (NodeEntity node : nodes) {
            if (node.getStatus() == NodeEntity.NodeStatus.HEALTHY
                    && node.getLastHeartbeat() != null
                    && node.getLastHeartbeat().isBefore(cutoff)) {

                node.setStatus(NodeEntity.NodeStatus.DEAD);
                nodeRepository.save(node);
                System.out.println("[HealthCheckService] Node marked DEAD: " + node.getId());

                reReplicateChunksFor(node);
            }
        }
    }

    private void reReplicateChunksFor(NodeEntity deadNode) {
        List<ChunkLocationEntity> affectedLocations = chunkLocationRepository.findAll().stream()
                .filter(loc -> loc.getNode().getId().equals(deadNode.getId()))
                .toList();

        for (ChunkLocationEntity affected : affectedLocations) {
            ChunkEntity chunk = affected.getChunk();
            reReplicateChunk(chunk);
        }
    }

    private void reReplicateChunk(ChunkEntity chunk) {
        List<ChunkLocationEntity> allLocations = chunkLocationRepository.findAll().stream()
                .filter(loc -> loc.getChunk().getId().equals(chunk.getId()))
                .toList();

        List<NodeEntity> healthyReplicaNodes = allLocations.stream()
                .map(ChunkLocationEntity::getNode)
                .filter(n -> n.getStatus() == NodeEntity.NodeStatus.HEALTHY)
                .toList();

        if (healthyReplicaNodes.size() >= replicationFactor) {
            return; // already has enough healthy copies, nothing to do
        }

        if (healthyReplicaNodes.isEmpty()) {
            System.err.println("[HealthCheckService] No healthy replicas left for chunk "
                    + chunk.getId() + " — cannot re-replicate!");
            return;
        }

        Set<String> nodesWithChunk = allLocations.stream()
                .map(loc -> loc.getNode().getId())
                .collect(java.util.stream.Collectors.toSet());

        Optional<NodeEntity> newNode = nodeRepository.findAll().stream()
                .filter(n -> n.getStatus() == NodeEntity.NodeStatus.HEALTHY)
                .filter(n -> !nodesWithChunk.contains(n.getId()))
                .findFirst();

        if (newNode.isEmpty()) {
            System.err.println("[HealthCheckService] No available healthy node to re-replicate chunk "
                    + chunk.getId());
            return;
        }

        NodeEntity sourceNode = healthyReplicaNodes.get(0);
        NodeEntity targetNode = newNode.get();
        String chunkId = chunk.getFile().getId() + "-chunk-" + chunk.getChunkNumber();

        try {
            String sourceUrl = "http://" + sourceNode.getHost() + ":" + sourceNode.getPort() + "/chunks/" + chunkId;
            byte[] data = restTemplate.getForObject(sourceUrl, byte[].class);

            String targetUrl = "http://" + targetNode.getHost() + ":" + targetNode.getPort() + "/chunks/" + chunkId;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            restTemplate.exchange(targetUrl, HttpMethod.PUT, new HttpEntity<>(data, headers), Map.class);

            ChunkLocationEntity newLocation = new ChunkLocationEntity();
            newLocation.setId(new ChunkLocationEntity.ChunkLocationId(chunk.getId(), targetNode.getId()));
            newLocation.setChunk(chunk);
            newLocation.setNode(targetNode);
            chunkLocationRepository.save(newLocation);

            System.out.println("[RE-REPLICATION] chunk " + chunkId + " copied from "
                    + sourceNode.getId() + " to " + targetNode.getId());
        } catch (Exception e) {
            System.err.println("[HealthCheckService] Re-replication failed for chunk "
                    + chunkId + ": " + e.getMessage());
        }
    }
}
