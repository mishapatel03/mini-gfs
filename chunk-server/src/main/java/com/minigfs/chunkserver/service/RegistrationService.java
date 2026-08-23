package com.minigfs.chunkserver.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
}
