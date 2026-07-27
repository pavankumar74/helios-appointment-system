import { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import StatusBadge from '../components/StatusBadge';
import api from '../api/client';
import { formatDateTime } from '../utils/format';

export default function DoctorDashboard() {
  const [appointments, setAppointments] = useState([]);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setError('');
    try {
      const { data } = await api.get('/api/appointments');
      setAppointments(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const setStatus = async (appointment, status) => {
    setError('');
    setMessage('');
    try {
      await api.put(`/api/appointments/${appointment.id}`, {
        status,
        scheduledAt: null,
        notes: appointment.notes || null,
      });
      setMessage(`Appointment ${status.toLowerCase()}.`);
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  const pending = appointments.filter((a) => a.status === 'PENDING');
  const upcoming = appointments.filter((a) => a.status === 'APPROVED');
  const history = appointments.filter(
    (a) => !['PENDING', 'APPROVED'].includes(a.status)
  );

  const Row = ({ a, actions }) => (
    <li className="list__item">
      <div>
        <strong>{a.patientName}</strong>
        <div className="muted">{formatDateTime(a.scheduledAt)}</div>
        {a.notes && <div className="notes">“{a.notes}”</div>}
      </div>
      <div className="list__actions">
        <StatusBadge status={a.status} />
        {actions}
      </div>
    </li>
  );

  return (
    <div className="app">
      <Navbar />
      <main className="container">
        <h1 className="page-title">Doctor dashboard</h1>

        {error && <div className="alert alert--error">{error}</div>}
        {message && <div className="alert alert--success">{message}</div>}

        {loading ? (
          <p className="muted">Loading…</p>
        ) : (
          <section className="grid grid--3">
            <div className="card">
              <h2>Requests <span className="pill">{pending.length}</span></h2>
              {pending.length === 0 ? (
                <p className="muted">No pending requests.</p>
              ) : (
                <ul className="list">
                  {pending.map((a) => (
                    <Row
                      key={a.id}
                      a={a}
                      actions={
                        <>
                          <button
                            className="btn btn--primary btn--sm"
                            onClick={() => setStatus(a, 'APPROVED')}
                          >
                            Approve
                          </button>
                          <button
                            className="btn btn--ghost btn--sm"
                            onClick={() => setStatus(a, 'REJECTED')}
                          >
                            Reject
                          </button>
                        </>
                      }
                    />
                  ))}
                </ul>
              )}
            </div>

            <div className="card">
              <h2>Upcoming <span className="pill">{upcoming.length}</span></h2>
              {upcoming.length === 0 ? (
                <p className="muted">Nothing scheduled.</p>
              ) : (
                <ul className="list">
                  {upcoming.map((a) => (
                    <Row
                      key={a.id}
                      a={a}
                      actions={
                        <button
                          className="btn btn--ghost btn--sm"
                          onClick={() => setStatus(a, 'COMPLETED')}
                        >
                          Mark complete
                        </button>
                      }
                    />
                  ))}
                </ul>
              )}
            </div>

            <div className="card">
              <h2>History <span className="pill">{history.length}</span></h2>
              {history.length === 0 ? (
                <p className="muted">No past appointments.</p>
              ) : (
                <ul className="list">
                  {history.map((a) => (
                    <Row key={a.id} a={a} actions={null} />
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
