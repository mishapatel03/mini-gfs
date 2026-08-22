package com.minigfs.master_server.repository;

import com.minigfs.master_server.entity.ChunkLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChunkLocationRepository extends JpaRepository<ChunkLocationEntity, ChunkLocationEntity.ChunkLocationId> {
}
