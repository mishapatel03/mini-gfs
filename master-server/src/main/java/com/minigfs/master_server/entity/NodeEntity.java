package com.minigfs.master_server.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "nodes")
@Data
public class NodeEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private int port;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NodeStatus status;

    @Column(name = "last_heartbeat")
    private Instant lastHeartbeat;

    @Column(name = "available_storage")
    private long availableStorage;

    public enum NodeStatus {
        HEALTHY, DEAD
    }
}
