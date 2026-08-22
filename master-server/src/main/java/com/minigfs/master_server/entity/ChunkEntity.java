package com.minigfs.master_server.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "chunks")
@Data
public class ChunkEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    @Column(name = "chunk_number", nullable = false)
    private int chunkNumber;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false)
    private String checksum;
}
