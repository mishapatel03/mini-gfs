package com.minigfs.master_server.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "chunk_locations")
@Data
public class ChunkLocationEntity {

    @EmbeddedId
    private ChunkLocationId id;

    @ManyToOne
    @MapsId("chunkId")
    @JoinColumn(name = "chunk_id")
    private ChunkEntity chunk;

    @ManyToOne
    @MapsId("nodeId")
    @JoinColumn(name = "node_id")
    private NodeEntity node;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    @Embeddable
    @Data
    public static class ChunkLocationId implements Serializable {
        private UUID chunkId;
        private String nodeId;

        public ChunkLocationId() {}

        public ChunkLocationId(UUID chunkId, String nodeId) {
            this.chunkId = chunkId;
            this.nodeId = nodeId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ChunkLocationId)) return false;
            ChunkLocationId that = (ChunkLocationId) o;
            return Objects.equals(chunkId, that.chunkId) && Objects.equals(nodeId, that.nodeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(chunkId, nodeId);
        }
    }
}
