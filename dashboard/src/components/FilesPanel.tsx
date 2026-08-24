import { useEffect, useRef, useState } from 'react';
import { fetchFiles, uploadFile, downloadFileUrl, type FileMeta } from '../api/client';

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${(bytes / Math.pow(1024, i)).toFixed(1)} ${units[i]}`;
}

export function FilesPanel() {
  const [files, setFiles] = useState<FileMeta[]>([]);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  async function loadFiles() {
    try {
      const data = await fetchFiles();
      setFiles(data);
    } catch (err) {
      setError('Failed to load files');
    }
  }

  useEffect(() => {
    loadFiles();
    const interval = setInterval(loadFiles, 5000);
    return () => clearInterval(interval);
  }, []);

  async function handleFileSelected(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploading(true);
    setError(null);
    try {
      await uploadFile(file);
      await loadFiles();
    } catch (err) {
      setError('Upload failed');
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  }

  return (
    <div style={{ marginTop: '32px' }}>
      <h2>Files</h2>

      <input
        ref={fileInputRef}
        type="file"
        onChange={handleFileSelected}
        disabled={uploading}
        style={{ marginBottom: '16px' }}
      />
      {uploading && <span style={{ marginLeft: '12px' }}>Uploading...</span>}
      {error && <div style={{ color: '#ff6b6b' }}>{error}</div>}

      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ textAlign: 'left', borderBottom: '1px solid #444' }}>
            <th style={{ padding: '8px' }}>Name</th>
            <th style={{ padding: '8px' }}>Size</th>
            <th style={{ padding: '8px' }}>Chunks</th>
            <th style={{ padding: '8px' }}>Uploaded</th>
            <th style={{ padding: '8px' }}></th>
          </tr>
        </thead>
        <tbody>
          {files.map((file) => (
            <tr key={file.id} style={{ borderBottom: '1px solid #333' }}>
              <td style={{ padding: '8px' }}>{file.name}</td>
              <td style={{ padding: '8px' }}>{formatBytes(file.size)}</td>
              <td style={{ padding: '8px' }}>{Math.ceil(file.size / file.chunkSize)}</td>
              <td style={{ padding: '8px' }}>{new Date(file.createdAt).toLocaleTimeString()}</td>
              <td style={{ padding: '8px' }}>
                <a href={downloadFileUrl(file.id)} style={{ color: '#4caf50' }}>
                  Download
                </a>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {files.length === 0 && <p>No files uploaded yet.</p>}
    </div>
  );
}
