import { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import StatusBadge from '../components/StatusBadge';
import api from '../api/client';
import { defaultSlot, formatDateTime } from '../utils/format';

export default function PatientDashboard() {
  const [appointments, setAppointments] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [form, setForm] = useState({ doctorId: '', scheduledAt: defaultSlot(), notes: '' });
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [booking, setBooking] = useState(false);

  const load = async () => {
    setError('');
    try {
      const [appts, docs] = await Promise.all([
        api.get('/api/appointments'),
        api.get('/api/doctors'),
      ]);
      setAppointments(appts.data);
      setDoctors(docs.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const book = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setBooking(true);
    try {
      await api.post('/api/appointments', {
        doctorId: Number(form.doctorId),
        scheduledAt: form.scheduledAt,
        notes: form.notes || null,
      });
      setMessage('Appointment requested. You will be notified once the doctor responds.');
      setForm({ doctorId: '', scheduledAt: defaultSlot(), notes: '' });
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBooking(false);
    }
  };

  const cancel = async (id) => {
    setError('');
    setMessage('');
    try {
      await api.delete(`/api/appointments/${id}`);
      setMessage('Appointment cancelled.');
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  const canCancel = (s) => s === 'PENDING' || s === 'APPROVED';

  return (
    <div className="app">
      <Navbar />
      <main className="container">
        <h1 className="page-title">Patient dashboard</h1>

        {error && <div className="alert alert--error">{error}</div>}
        {message && <div className="alert alert--success">{message}</div>}

        <section className="grid grid--2">
          <div className="card">
            <h2>Book an appointment</h2>
            <form onSubmit={book}>
              <label htmlFor="doctor">Doctor</label>
              <select
                id="doctor"
                value={form.doctorId}
                onChange={(e) => setForm({ ...form, doctorId: e.target.value })}
                required
              >
                <option value="" disabled>
                  Select a doctor…
                </option>
                {doctors.map((d) => (
                  <option key={d.id} value={d.id}>
                    Dr. {d.name}
                    {d.specialty ? ` — ${d.specialty}` : ''}
                  </option>
                ))}
              </select>

              <label htmlFor="when">Date &amp; time</label>
              <input
                id="when"
                type="datetime-local"
                value={form.scheduledAt}
                onChange={(e) => setForm({ ...form, scheduledAt: e.target.value })}
                required
              />

              <label htmlFor="notes">Reason / notes</label>
              <textarea
                id="notes"
                rows={3}
                value={form.notes}
                onChange={(e) => setForm({ ...form, notes: e.target.value })}
                placeholder="Describe your symptoms or reason for the visit"
              />

              <button className="btn btn--primary" type="submit" disabled={booking}>
                {booking ? 'Requesting…' : 'Request appointment'}
              </button>
            </form>
          </div>

          <div className="card">
            <h2>Your appointments</h2>
            {loading ? (
              <p className="muted">Loading…</p>
            ) : appointments.length === 0 ? (
              <p className="muted">No appointments yet. Book your first one!</p>
            ) : (
              <ul className="list">
                {appointments.map((a) => (
                  <li key={a.id} className="list__item">
                    <div>
                      <strong>Dr. {a.doctorName}</strong>
                      <div className="muted">{formatDateTime(a.scheduledAt)}</div>
                      {a.notes && <div className="notes">“{a.notes}”</div>}
                    </div>
                    <div className="list__actions">
                      <StatusBadge status={a.status} />
                      {canCancel(a.status) && (
                        <button className="btn btn--ghost btn--sm" onClick={() => cancel(a.id)}>
                          Cancel
                        </button>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </section>
      </main>
    </div>
  );
}
