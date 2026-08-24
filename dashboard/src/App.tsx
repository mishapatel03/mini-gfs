import { NodeHealthTable } from './components/NodeHealthTable';
import { FilesPanel } from './components/FilesPanel';
import './App.css';

function App() {
  return (
    <div style={{ padding: '24px', fontFamily: 'sans-serif', color: '#eee', backgroundColor: '#1a1a1a', minHeight: '100vh' }}>
      <h1>Mini-GFS Dashboard</h1>
      <NodeHealthTable />
      <FilesPanel />
    </div>
  );
}

export default App;
