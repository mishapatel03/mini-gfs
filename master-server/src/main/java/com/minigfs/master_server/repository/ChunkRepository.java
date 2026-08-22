package com.minigfs.master_server.repository;

import com.minigfs.master_server.entity.ChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ChunkRepository extends JpaRepository<ChunkEntity, UUID> {
}
