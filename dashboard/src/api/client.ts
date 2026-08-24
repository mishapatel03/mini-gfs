import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
});

export interface Node {
  id: string;
  host: string;
  port: number;
  status: 'HEALTHY' | 'DEAD';
  lastHeartbeat: string;
  availableStorage: number;
}

export interface FileMeta {
  id: string;
  name: string;
  size: number;
  chunkSize: number;
  createdAt: string;
}

export async function fetchNodes(): Promise<Node[]> {
  const response = await apiClient.get<Node[]>('/api/nodes');
  return response.data;
}

export async function fetchFiles(): Promise<FileMeta[]> {
  const response = await apiClient.get<FileMeta[]>('/api/files');
  return response.data;
}

export async function uploadFile(file: File): Promise<FileMeta> {
  const formData = new FormData();
  formData.append('file', file);
  const response = await apiClient.post<FileMeta>('/api/files', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return response.data;
}

export function downloadFileUrl(fileId: string): string {
  return `${API_BASE_URL}/api/files/${fileId}/download`;
}
