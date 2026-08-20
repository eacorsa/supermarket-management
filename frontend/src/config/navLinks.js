export const NAV_LINKS = [
  { to: '/dashboard', label: 'Inicio', roles: ['ADMIN', 'GERENTE', 'CAJERO', 'ALMACENISTA'] },
  { to: '/products', label: 'Productos', roles: ['ADMIN', 'GERENTE', 'ALMACENISTA'] },
  { to: '/purchases', label: 'Compras', roles: ['ADMIN', 'GERENTE', 'ALMACENISTA'] },
  { to: '/sales', label: 'Ventas', roles: ['ADMIN', 'GERENTE', 'CAJERO'] },
  { to: '/customers', label: 'Clientes', roles: ['ADMIN', 'GERENTE', 'CAJERO'] },
  { to: '/employees', label: 'Empleados', roles: ['ADMIN'] },
  { to: '/reports', label: 'Reportes', roles: ['ADMIN', 'GERENTE'] },
  { to: '/audit', label: 'Auditoría', roles: ['ADMIN'] }
];
