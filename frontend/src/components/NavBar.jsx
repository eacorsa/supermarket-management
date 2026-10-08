import { Link, useNavigate, useLocation } from 'react-router-dom';
import { getRoles } from '../utils/jwt';
import { NAV_LINKS } from '../config/navLinks';

function NavBar() {
  const navigate = useNavigate();
  const location = useLocation();
  const roles = getRoles();
  const visibleLinks = NAV_LINKS.filter((link) => link.roles.some((role) => roles.includes(role)));

  const handleLogout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    navigate('/login');
  };

  return (
    <header className="store-header">
      <div className="store-topbar"><span>Frescura y calidad, todos los días</span><span>Tu supermercado · Gestión de inventario</span></div>
      <div className="store-brand-row">
        <Link to="/dashboard" className="store-brand"><span className="store-brand-icon" aria-hidden="true">🥦</span><span>Supermercado<small>Todo lo que necesitas, en un solo lugar</small></span></Link>
        <div className="store-session"><span>{roles.join(' · ') || 'Sin sesión'}</span><button className="btn-secondary" onClick={handleLogout}>Cerrar sesión</button></div>
      </div>
    <nav className="store-navigation" aria-label="Navegación principal">
      <div className="navbar-links">
        {visibleLinks.map((link) => (
          <Link
            key={link.to}
            to={link.to}
            className={`nav-link${location.pathname === link.to ? ' active' : ''}`}
          >
            {link.label}
          </Link>
        ))}
      </div>
    </nav>
    </header>
  );
}

export default NavBar;
