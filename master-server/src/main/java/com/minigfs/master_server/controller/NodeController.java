package com.minigfs.master_server.controller;

import com.minigfs.master_server.entity.NodeEntity;
import com.minigfs.master_server.repository.NodeRepository;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/nodes")
public class NodeController {

    private final NodeRepository nodeRepository;

    public NodeController(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    public record RegisterRequest(String nodeId, String host, int port) {}

    @PostMapping("/register")
    public NodeEntity register(@RequestBody RegisterRequest request) {
        NodeEntity node = nodeRepository.findById(request.nodeId())
                .orElse(new NodeEntity());

        node.setId(request.nodeId());
        node.setHost(request.host());
        node.setPort(request.port());
        node.setStatus(NodeEntity.NodeStatus.HEALTHY);
        node.setLastHeartbeat(Instant.now());

        return nodeRepository.save(node);
    }

    @GetMapping
public List<NodeEntity> listNodes() {
        return nodeRepository.findAll();
    }
}
