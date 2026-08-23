package com.minigfs.chunkserver.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Service
public class RegistrationService {

    @Value("${node.id}")
    private String nodeId;

    @Value("${server.port}")
    private int serverPort;

    @Value("${master.url}")
    private String masterUrl;

    @Value("${chunk.storage.dir}")
    private String storageDir;

    private final RestTemplate restTemplate = new RestTemplate();

    @EventListener(ApplicationReadyEvent.class)
    public void registerWithMaster() {
        Map<String, Object> body = new HashMap<>();
        body.put("nodeId", nodeId);
        body.put("host", "localhost");
        body.put("port", serverPort);

        try {
            restTemplate.postForObject(masterUrl + "/api/nodes/register", body, Object.class);
            System.out.println("[RegistrationService] Registered with master as " + nodeId);
        } catch (Exception e) {
            System.err.println("[RegistrationService] Failed to register with master: " + e.getMessage());
        }
    }

    @Scheduled(fixedRate = 3000)
    public void sendHeartbeat() {
        long freeStorage = new File(storageDir).getUsableSpace();
        int chunkCount = countChunks();

        Map<String, Object> body = new HashMap<>();
        body.put("freeStorage", freeStorage);
        body.put("chunkCount", chunkCount);

        try {
            restTemplate.postForObject(masterUrl + "/api/nodes/" + nodeId + "/heartbeat", body, Object.class);
            System.out.println("[RegistrationService] Heartbeat sent for " + nodeId);
        } catch (Exception e) {
            System.err.println("[RegistrationService] Heartbeat failed: " + e.getMessage());
        }
    }

    private int countChunks() {
        File dir = new File(storageDir);
        File[] files = dir.listFiles();
        return files == null ? 0 : files.length;
    }
}
