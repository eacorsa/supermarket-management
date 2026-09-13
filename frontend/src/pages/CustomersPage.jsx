import { useEffect, useState } from 'react';
import api from '../services/api';
import { getRoles } from '../utils/jwt';
import { apiError } from '../utils/apiError';
import NavBar from '../components/NavBar';

function CustomersPage() {
  const [editingId, setEditingId] = useState(null);
  const [saving, setSaving] = useState(false);
  const canManage = getRoles().some(role => ["ADMIN","GERENTE"].includes(role));
  const [customers, setCustomers] = useState([]);
  const [form, setForm] = useState({ name: '', document: '', email: '', phone: '' });

  const loadCustomers = () => {
    api.get('/customers').then((res) => setCustomers(res.data)).catch(() => alert('No se pudieron cargar los clientes'));
  };

  useEffect(() => {
    loadCustomers();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (saving) return;
    try {
      setSaving(true);
      if (editingId === null) await api.post('/customers', form);
      else await api.put(`/customers/${editingId}`, form);
      setEditingId(null);
      setForm({ name: '', document: '', email: '', phone: '' });
      loadCustomers();
    } catch (error) {
      alert(apiError(error, 'No se pudieron guardar los cambios'));
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('¿Eliminar este registro?')) return;
    try {
      await api.delete(`/customers/${id}`);
      if (editingId === id) { setEditingId(null); setForm({ name: '', document: '', email: '', phone: '' }); }
      loadCustomers();
    } catch (error) {
      alert('No se pudo eliminar el cliente');
    }
  };

  return (
    <div>
      <NavBar />
      <div className="page-container">
      <h2>Clientes</h2>

      <form onSubmit={handleSubmit} className="card form-row">
        <input placeholder="Nombre" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
        <input placeholder="Documento" value={form.document} onChange={(e) => setForm({ ...form, document: e.target.value })} />
        <input placeholder="Email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        <input placeholder="Teléfono" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
        <button type="submit" disabled={saving}>{saving ? 'Guardando…' : editingId === null ? 'Agregar' : 'Guardar cambios'}</button>
        {editingId !== null && <button type="button" disabled={saving} onClick={() => { setEditingId(null); setForm({ name: '', document: '', email: '', phone: '' }); }}>Cancelar edición</button>}
      </form>

      <table className="table">
        <thead>
          <tr>
            <th>Nombre</th>
            <th>Documento</th>
            <th>Email</th>
            <th>Teléfono</th>
            {canManage && <th>Acciones</th>}
          </tr>
        </thead>
        <tbody>
          {customers.map((customer) => (
            <tr key={customer.id}>
              <td>{customer.name}</td>
              <td>{customer.document}</td>
              <td>{customer.email}</td>
              <td>{customer.phone}</td>
              {canManage && <td>
                <button onClick={() => { setEditingId(customer.id); setForm({ name: customer.name, document: customer.document ?? '', email: customer.email ?? '', phone: customer.phone ?? '' }); }}>Editar</button>{' '}
                <button className="btn-danger" onClick={() => handleDelete(customer.id)}>Eliminar</button>
              </td>}
            </tr>
          ))}
        </tbody>
      </table>
      </div>
    </div>
  );
}

export default CustomersPage;
