import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <header className="navbar">
      <div className="navbar__brand">
        <span className="navbar__logo" aria-hidden="true">✦</span>
        <span>Helios</span>
        <small className="navbar__sub">HelloDoctor</small>
      </div>
      {user && (
        <div className="navbar__user">
          <span className="badge badge--role">{user.role}</span>
          <span className="navbar__name">{user.name}</span>
          <button className="btn btn--ghost" onClick={handleLogout}>
            Sign out
          </button>
        </div>
      )}
    </header>
  );
}
