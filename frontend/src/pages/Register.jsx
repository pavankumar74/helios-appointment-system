import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    role: 'PATIENT',
    specialty: '',
    phone: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const update = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const payload = { ...form };
      if (payload.role !== 'DOCTOR') delete payload.specialty;
      if (!payload.phone) delete payload.phone;
      await register(payload);
      navigate('/', { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth">
      <div className="auth__panel auth__panel--brand">
        <div className="auth__brandmark">✦ Helios</div>
        <h1>Join the future of healthcare scheduling.</h1>
        <p>Create your account to book or manage appointments in seconds.</p>
      </div>
      <div className="auth__panel">
        <form className="card" onSubmit={handleSubmit}>
          <h2>Create account</h2>
          <p className="muted">It only takes a moment.</p>

          {error && <div className="alert alert--error" role="alert">{error}</div>}

          <label htmlFor="name">Full name</label>
          <input id="name" value={form.name} onChange={update('name')} required />

          <label htmlFor="email">Email</label>
          <input id="email" type="email" value={form.email} onChange={update('email')} required />

          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            minLength={8}
            value={form.password}
            onChange={update('password')}
            required
          />
          <small className="muted">Minimum 8 characters.</small>

          <label htmlFor="phone">Phone (optional, for SMS)</label>
          <input
            id="phone"
            type="tel"
            value={form.phone}
            onChange={update('phone')}
            placeholder="+15551234567"
          />

          <label htmlFor="role">I am a</label>
          <select id="role" value={form.role} onChange={update('role')}>
            <option value="PATIENT">Patient</option>
            <option value="DOCTOR">Doctor</option>
          </select>

          {form.role === 'DOCTOR' && (
            <>
              <label htmlFor="specialty">Specialty</label>
              <input
                id="specialty"
                value={form.specialty}
                onChange={update('specialty')}
                placeholder="e.g. Cardiology"
              />
            </>
          )}

          <button className="btn btn--primary" type="submit" disabled={loading}>
            {loading ? 'Creating…' : 'Create account'}
          </button>

          <p className="muted center">
            Already have an account? <Link to="/login">Sign in</Link>
          </p>
        </form>
      </div>
    </div>
  );
}
