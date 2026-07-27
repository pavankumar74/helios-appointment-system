import { Navigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

/**
 * Guards a route: requires authentication and (optionally) one of the given roles.
 */
export default function ProtectedRoute({ children, roles }) {
  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  if (roles && roles.length > 0 && !roles.includes(user?.role)) {
    return <Navigate to="/" replace />;
  }
  return children;
}
