import { useEffect, useState } from 'react';
import api from '../services/api';
import { getRoles } from '../utils/jwt';
import { apiError } from '../utils/apiError';
import NavBar from '../components/NavBar';

const AVAILABLE_ROLES = ['ADMIN', 'GERENTE', 'CAJERO', 'ALMACENISTA'];

function EmployeesPage() {
  const [editingId, setEditingId] = useState(null);
  const [saving, setSaving] = useState(false);
  const canManage = getRoles().some(role => ["ADMIN"].includes(role));
  const [employees, setEmployees] = useState([]);
  const [form, setForm] = useState({ username: '', password: '', fullName: '', email: '', roles: [] });

  const loadEmployees = () => {
    api.get('/employees').then((res) => setEmployees(res.data)).catch(() => alert('No se pudieron cargar los empleados'));
  };

  useEffect(() => {
    loadEmployees();
  }, []);

  const toggleRole = (role) => {
    setForm((prev) => ({
      ...prev,
      roles: prev.roles.includes(role) ? prev.roles.filter((r) => r !== role) : [...prev.roles, role]
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (saving) return;
    if (form.roles.length === 0) {
      alert('Selecciona al menos un rol');
      return;
    }
    try {
      setSaving(true);
      if (editingId === null) await api.post('/employees', form);
      else await api.put(`/employees/${editingId}`, { fullName: form.fullName, email: form.email, password: form.password, roles: form.roles });
      setEditingId(null);
      setForm({ username: '', password: '', fullName: '', email: '', roles: [] });
      loadEmployees();
    } catch (error) {
      alert(apiError(error, 'No se pudieron guardar los cambios'));
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('¿Eliminar este registro?')) return;
    try {
      await api.delete(`/employees/${id}`);
      if (editingId === id) { setEditingId(null); setForm({ username: '', password: '', fullName: '', email: '', roles: [] }); }
      loadEmployees();
    } catch (error) {
      alert('No se pudo eliminar el empleado');
    }
  };

  return (
    <div>
      <NavBar />
      <div className="page-container">
      <h2>Empleados</h2>

      {canManage && <form onSubmit={handleSubmit} className="card form-row">
        <input disabled={editingId !== null} title="El usuario no se modifica" placeholder="Usuario" value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} required />
        <input type="password" placeholder={editingId === null ? "Contraseña" : "Nueva contraseña (opcional)"} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required={editingId === null} />
        <input placeholder="Nombre completo" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required />
        <input placeholder="Email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
        {AVAILABLE_ROLES.map((role) => (
          <label key={role}>
            <input type="checkbox" checked={form.roles.includes(role)} onChange={() => toggleRole(role)} /> {role}
          </label>
        ))}
        <button type="submit" disabled={saving}>{saving ? 'Guardando…' : editingId === null ? 'Agregar' : 'Guardar cambios'}</button>
        {editingId !== null && <button type="button" disabled={saving} onClick={() => { setEditingId(null); setForm({ username: '', password: '', fullName: '', email: '', roles: [] }); }}>Cancelar edición</button>}
      </form>}

      <table className="table">
        <thead>
          <tr>
            <th>Usuario</th>
            <th>Nombre</th>
            <th>Email</th>
            <th>Roles</th>
            {canManage && <th>Acciones</th>}
          </tr>
        </thead>
        <tbody>
          {employees.map((employee) => (
            <tr key={employee.id}>
              <td>{employee.username}</td>
              <td>{employee.fullName}</td>
              <td>{employee.email}</td>
              <td>{employee.roles.join(', ')}</td>
              {canManage && <td>
                <button onClick={() => { setEditingId(employee.id); setForm({ ...employee, password: '' }); }}>Editar</button>{' '}
                <button className="btn-danger" onClick={() => handleDelete(employee.id)}>Eliminar</button>
              </td>}
            </tr>
          ))}
        </tbody>
      </table>
      </div>
    </div>
  );
}

export default EmployeesPage;
