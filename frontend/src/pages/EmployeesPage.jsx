import { useEffect, useState } from 'react';
import api from '../services/api';
import NavBar from '../components/NavBar';

const AVAILABLE_ROLES = ['ADMIN', 'GERENTE', 'CAJERO', 'ALMACENISTA'];

function EmployeesPage() {
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
    if (form.roles.length === 0) {
      alert('Selecciona al menos un rol');
      return;
    }
    try {
      await api.post('/employees', form);
      setForm({ username: '', password: '', fullName: '', email: '', roles: [] });
      loadEmployees();
    } catch (error) {
      alert(error.response?.data?.message ?? 'No se pudo crear el empleado');
    }
  };

  const handleDelete = async (id) => {
    try {
      await api.delete(`/employees/${id}`);
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

      <form onSubmit={handleSubmit} className="card form-row">
        <input placeholder="Usuario" value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} required />
        <input type="password" placeholder="Contraseña" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
        <input placeholder="Nombre completo" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required />
        <input placeholder="Email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
        {AVAILABLE_ROLES.map((role) => (
          <label key={role}>
            <input type="checkbox" checked={form.roles.includes(role)} onChange={() => toggleRole(role)} /> {role}
          </label>
        ))}
        <button type="submit">Crear empleado</button>
      </form>

      <table className="table">
        <thead>
          <tr>
            <th>Usuario</th>
            <th>Nombre</th>
            <th>Email</th>
            <th>Roles</th>
            <th>Acción</th>
          </tr>
        </thead>
        <tbody>
          {employees.map((employee) => (
            <tr key={employee.id}>
              <td>{employee.username}</td>
              <td>{employee.fullName}</td>
              <td>{employee.email}</td>
              <td>{employee.roles.join(', ')}</td>
              <td>
                <button className="btn-danger" onClick={() => handleDelete(employee.id)}>Eliminar</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      </div>
    </div>
  );
}

export default EmployeesPage;
