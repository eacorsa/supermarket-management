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
    <nav className="navbar">
      <span className="navbar-brand">🥦 Supermercado</span>
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
      <button className="btn-logout" onClick={handleLogout}>Cerrar sesión</button>
    </nav>
  );
}

export default NavBar;
