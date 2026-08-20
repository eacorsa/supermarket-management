import { useEffect, useState } from 'react';
import api from '../services/api';
import NavBar from '../components/NavBar';

function CustomersPage() {
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
    try {
      await api.post('/customers', form);
      setForm({ name: '', document: '', email: '', phone: '' });
      loadCustomers();
    } catch (error) {
      alert('No se pudo crear el cliente');
    }
  };

  const handleDelete = async (id) => {
    try {
      await api.delete(`/customers/${id}`);
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
        <button type="submit">Agregar</button>
      </form>

      <table className="table">
        <thead>
          <tr>
            <th>Nombre</th>
            <th>Documento</th>
            <th>Email</th>
            <th>Teléfono</th>
            <th>Acción</th>
          </tr>
        </thead>
        <tbody>
          {customers.map((customer) => (
            <tr key={customer.id}>
              <td>{customer.name}</td>
              <td>{customer.document}</td>
              <td>{customer.email}</td>
              <td>{customer.phone}</td>
              <td>
                <button className="btn-danger" onClick={() => handleDelete(customer.id)}>Eliminar</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      </div>
    </div>
  );
}

export default CustomersPage;
