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

export async function fetchNodes(): Promise<Node[]> {
  const response = await apiClient.get<Node[]>('/api/nodes');
  return response.data;
}
