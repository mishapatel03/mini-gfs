package com.minigfs.master_server.service;

import com.minigfs.master_server.entity.NodeEntity;
import com.minigfs.master_server.repository.NodeRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class HealthCheckService {

    private static final long TIMEOUT_SECONDS = 10;

    private final NodeRepository nodeRepository;

    public HealthCheckService(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
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
            }
        }
    }
}
