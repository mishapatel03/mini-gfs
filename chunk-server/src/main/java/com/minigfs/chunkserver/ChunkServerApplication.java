package com.minigfs.chunkserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChunkServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChunkServerApplication.class, args);
    }
}
