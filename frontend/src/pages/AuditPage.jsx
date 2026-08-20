import { useEffect, useState } from 'react';
import api from '../services/api';
import NavBar from '../components/NavBar';

const ENTITY_TYPES = ['PRODUCTO', 'CATEGORIA', 'PROVEEDOR', 'CLIENTE', 'COMPRA', 'VENTA'];

function formatDate(value) {
  if (!value) return '-';
  return new Date(value).toLocaleString();
}

function AuditPage() {
  const [records, setRecords] = useState([]);
  const [entityType, setEntityType] = useState('');

  const loadRecords = () => {
    api.get('/audit', { params: entityType ? { entityType } : {} })
      .then((res) => setRecords(res.data))
      .catch(() => alert('No se pudo cargar el registro de auditoría'));
  };

  useEffect(() => {
    loadRecords();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entityType]);

  return (
    <div>
      <NavBar />
      <div className="page-container">
        <h2>Auditoría</h2>

        <div className="card form-row">
          <label>
            Módulo:{' '}
            <select value={entityType} onChange={(e) => setEntityType(e.target.value)}>
              <option value="">Todos</option>
              {ENTITY_TYPES.map((type) => (
                <option key={type} value={type}>{type}</option>
              ))}
            </select>
          </label>
        </div>

        <table className="table">
          <thead>
            <tr>
              <th>Módulo</th>
              <th>ID</th>
              <th>Descripción</th>
              <th>Creado por</th>
              <th>Creado el</th>
              <th>Modificado por</th>
              <th>Modificado el</th>
            </tr>
          </thead>
          <tbody>
            {records.map((r) => (
              <tr key={`${r.entityType}-${r.entityId}`}>
                <td>{r.entityType}</td>
                <td>{r.entityId}</td>
                <td>{r.description}</td>
                <td>{r.createdBy ?? '-'}</td>
                <td>{formatDate(r.createdAt)}</td>
                <td>{r.updatedBy ?? '-'}</td>
                <td>{formatDate(r.updatedAt)}</td>
              </tr>
            ))}
            {records.length === 0 && (
              <tr>
                <td colSpan="7" style={{ textAlign: 'center' }}>Sin registros</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default AuditPage;
