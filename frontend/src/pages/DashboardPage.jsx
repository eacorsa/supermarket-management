import { Link } from 'react-router-dom';
import NavBar from '../components/NavBar';
import { NAV_LINKS } from '../config/navLinks';
import { getRoles } from '../utils/jwt';

function DashboardPage() {
  const roles = getRoles();
  const quickLinks = NAV_LINKS.filter((link) => link.to !== '/dashboard' && link.roles.some((role) => roles.includes(role)));

  return (
    <div>
      <NavBar />
      <div className="page-container">
      <div className="card">
      <h1>Dashboard del supermercado</h1>
      </div>
      </div>
    </div>
  );
}

export default DashboardPage;
