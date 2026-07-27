import { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import StatusBadge from '../components/StatusBadge';
import api from '../api/client';
import { formatDateTime } from '../utils/format';

export default function AdminDashboard() {
  const [users, setUsers] = useState([]);
  const [appointments, setAppointments] = useState([]);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setError('');
    try {
      const [u, a] = await Promise.all([
        api.get('/api/users'),
        api.get('/api/appointments'),
      ]);
      setUsers(u.data);
      setAppointments(a.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const toggleStatus = async (user) => {
    setError('');
    setMessage('');
    const next = user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
    try {
      await api.put(`/api/users/${user.id}/status?status=${next}`);
      setMessage(`${user.name} is now ${next}.`);
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="app">
      <Navbar />
      <main className="container">
        <h1 className="page-title">Admin console</h1>

        {error && <div className="alert alert--error">{error}</div>}
        {message && <div className="alert alert--success">{message}</div>}

        <div className="stats">
          <div className="stat">
            <span className="stat__value">{users.length}</span>
            <span className="stat__label">Users</span>
          </div>
          <div className="stat">
            <span className="stat__value">
              {users.filter((u) => u.role === 'DOCTOR').length}
            </span>
            <span className="stat__label">Doctors</span>
          </div>
          <div className="stat">
            <span className="stat__value">{appointments.length}</span>
            <span className="stat__label">Appointments</span>
          </div>
        </div>

        {loading ? (
          <p className="muted">Loading…</p>
        ) : (
          <section className="grid grid--2">
            <div className="card">
              <h2>Users</h2>
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Name</th>
                      <th>Email</th>
                      <th>Role</th>
                      <th>Status</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.map((u) => (
                      <tr key={u.id}>
                        <td>{u.name}</td>
                        <td className="muted">{u.email}</td>
                        <td>
                          <span className="badge badge--role">{u.role}</span>
                        </td>
                        <td>
                          <span
                            className={`badge ${
                              u.status === 'ACTIVE' ? 'badge--approved' : 'badge--cancelled'
                            }`}
                          >
                            {u.status}
                          </span>
                        </td>
                        <td>
                          {u.role !== 'ADMIN' && (
                            <button
                              className="btn btn--ghost btn--sm"
                              onClick={() => toggleStatus(u)}
                            >
                              {u.status === 'ACTIVE' ? 'Disable' : 'Enable'}
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="card">
              <h2>All appointments</h2>
              {appointments.length === 0 ? (
                <p className="muted">No appointments in the system.</p>
              ) : (
                <ul className="list">
                  {appointments.map((a) => (
                    <li key={a.id} className="list__item">
                      <div>
                        <strong>{a.patientName}</strong> → Dr. {a.doctorName}
                        <div className="muted">{formatDateTime(a.scheduledAt)}</div>
                      </div>
                      <StatusBadge status={a.status} />
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </section>
        )}
      </main>
    </div>
  );
}
