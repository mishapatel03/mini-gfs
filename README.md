# Mini GFS (Google File System)

A distributed file system implementation inspired by Google File System (GFS), featuring a master-server architecture with multiple chunk servers for distributed storage.

## Architecture

The system consists of the following components:

- **Master Server**: Central coordinator that manages file system metadata, chunk allocation, and load balancing
- **Chunk Servers**: Distributed storage nodes that store file chunks and handle read/write operations
- **Dashboard**: Web-based UI for monitoring and managing the file system
- **PostgreSQL**: Database for storing metadata and file system state

## Project Structure

```
mini-gfs/
├── master-server/     # Spring Boot master server application
├── chunk-server/      # Spring Boot chunk server application
├── dashboard/         # React + TypeScript dashboard UI
├── docker/            # Docker Compose configuration
└── docs/              # Documentation
```

## Technologies

- **Master Server**: Spring Boot 4.1.1, Java 21, Spring Data JPA, PostgreSQL
- **Chunk Servers**: Spring Boot 4.1.1, Java 21
- **Dashboard**: React, TypeScript, Vite
- **Orchestration**: Docker, Docker Compose
- **Database**: PostgreSQL 16

## Quick Start

### Prerequisites

- Docker and Docker Compose installed
- Java 21 (for local development)
- Node.js (for dashboard development)

### Running with Docker Compose

1. Clone the repository
2. Navigate to the project root
3. Start all services:

```bash
cd docker
docker-compose up -d
```

This will start:
- PostgreSQL on port 5432
- Master Server on port 8080
- Chunk Server A on port 8081
- Chunk Server B on port 8082
- Chunk Server C on port 8083
- Dashboard on port 3000

### Accessing the Services

- **Dashboard**: http://localhost:3000
- **Master Server API**: http://localhost:8080
- **Chunk Server A**: http://localhost:8081
- **Chunk Server B**: http://localhost:8082
- **Chunk Server C**: http://localhost:8083

## Development

### Master Server

```bash
cd master-server
./mvnw spring-boot:run
```

### Chunk Server

```bash
cd chunk-server
./mvnw spring-boot:run
```

### Dashboard

```bash
cd dashboard
npm install
npm run dev
```

## Configuration

### Environment Variables

**Master Server:**
- `SPRING_DATASOURCE_URL`: PostgreSQL connection URL
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password

**Chunk Servers:**
- `NODE_ID`: Unique identifier for the chunk server (e.g., node-A, node-B, node-C)
- `SERVER_PORT`: Port for the chunk server (default: 8081)
- `MASTER_URL`: URL of the master server

## Features

- Distributed file storage across multiple chunk servers
- Master server for metadata management and coordination
- Web dashboard for monitoring and management
- Fault tolerance through replication
- Scalable architecture supporting additional chunk servers

## License

This project is provided as-is for educational and development purposes.
