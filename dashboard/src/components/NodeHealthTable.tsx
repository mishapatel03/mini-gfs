import { useEffect, useState } from 'react';
import { fetchNodes, type Node } from '../api/client';

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${(bytes / Math.pow(1024, i)).toFixed(1)} ${units[i]}`;
}

function timeAgo(isoString: string): string {
  const seconds = Math.floor((Date.now() - new Date(isoString).getTime()) / 1000);
  if (seconds < 60) return `${seconds}s ago`;
  return `${Math.floor(seconds / 60)}m ago`;
}

export function NodeHealthTable() {
  const [nodes, setNodes] = useState<Node[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function poll() {
      try {
        const data = await fetchNodes();
        setNodes(data);
        setError(null);
      } catch (err) {
        setError('Failed to reach Master Server');
      }
    }

    poll();
    const interval = setInterval(poll, 3000);
    return () => clearInterval(interval);
  }, []);

  if (error) {
    return <div style={{ color: '#ff6b6b' }}>{error}</div>;
  }

  return (
    <div>
      <h2>Node Health</h2>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ textAlign: 'left', borderBottom: '1px solid #444' }}>
            <th style={{ padding: '8px' }}>Node ID</th>
            <th style={{ padding: '8px' }}>Status</th>
            <th style={{ padding: '8px' }}>Host:Port</th>
            <th style={{ padding: '8px' }}>Free Storage</th>
            <th style={{ padding: '8px' }}>Last Heartbeat</th>
          </tr>
        </thead>
        <tbody>
          {nodes.map((node) => (
            <tr key={node.id} style={{ borderBottom: '1px solid #333' }}>
              <td style={{ padding: '8px' }}>{node.id}</td>
              <td style={{ padding: '8px' }}>
                <span
                  style={{
                    display: 'inline-block',
                    width: '10px',
                    height: '10px',
                    borderRadius: '50%',
                    backgroundColor: node.status === 'HEALTHY' ? '#4caf50' : '#f44336',
                    marginRight: '6px',
                  }}
                />
                {node.status}
              </td>
              <td style={{ padding: '8px' }}>{node.host}:{node.port}</td>
              <td style={{ padding: '8px' }}>{formatBytes(node.availableStorage)}</td>
              <td style={{ padding: '8px' }}>{timeAgo(node.lastHeartbeat)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {nodes.length === 0 && <p>No nodes registered yet.</p>}
    </div>
  );
}
