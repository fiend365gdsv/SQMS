import React, { useEffect, useState, useRef } from 'react';
import { Routes, Route, Link, useParams } from 'react-router-dom';
import { FiUsers, FiList, FiBell } from 'react-icons/fi';
import API, { API_BASE } from './api';

// Utility
const fmtETA = (secs) => {
  if (secs == null) return '-';
  const m = Math.floor(secs / 60);
  const s = secs % 60;
  return m > 0 ? `${m}m ${s}s` : `${s}s`;
};

// Toast Component
function Toast({ message, type, onClose }) {
  if (!message) return null;
  return (
    <div className={`toast ${type}`}>
      {message}
      <button onClick={onClose} className="close-btn">×</button>
    </div>
  );
}

// NavBar
function NavBar() {
  return (
    <header className="nav">
      <div className="nav-inner">
        <Link to="/" className="brand">SQMS</Link>
        <nav className="nav-links">
          <Link to="/doctors">Doctors</Link>
          <Link to="/reception">Reception</Link>
          <Link to="/doctor/1">Doctor Dashboard</Link>
        </nav>
      </div>
    </header>
  );
}

// Home
function Home() {
  return (
    <main className="hero">
      <div className="hero-grid">
        <div className="hero-left">
          <h1>Smart Queue Management — Fast, clear, professional</h1>
          <p className="lead">
            Reduce waiting time, automate patient notifications, and run your clinic like a pro.
          </p>
          <div className="cta-row">
            <Link to="/reception" className="btn primary">Open Reception</Link>
            <Link to="/doctors" className="btn ghost">Manage Doctors</Link>
          </div>
          <div className="features">
            <div className="feature"><FiBell/> Real-time alerts</div>
            <div className="feature"><FiUsers/> Queue visibility</div>
            <div className="feature"><FiList/> Simple workflows</div>
          </div>
        </div>
        <div className="hero-right">
          <div className="card large">
            <h3>Now serving</h3>
            <div className="now-card">
              <div>
                <div className="now-token">--</div>
                <div className="now-meta">Select a doctor dashboard to view live</div>
              </div>
              <div className="now-actions">
                <Link to="/doctor/1" className="btn small">Doctor Dashboard</Link>
              </div>
            </div>
          </div>
          <div className="card stats">
            <div>
              <div className="stat">Avg wait <strong>~3m</strong></div>
              <div className="muted">Calculated from recent services</div>
            </div>
            <div>
              <div className="stat">Completed <strong>120</strong></div>
              <div className="muted">Today</div>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}

// Doctors Page
function DoctorsPage({ showToast }) {
  const [doctors, setDoctors] = useState([]);
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(false);

  const fetchDoctors = async () => {
    try {
      setLoading(true);
      const res = await API.get('/api/doctors');
      setDoctors(res.data || []);
    } catch (e) {
      console.error(e);
      showToast('Unable to load doctors ❌', 'error');
    } finally { setLoading(false); }
  };

  useEffect(() => { fetchDoctors(); }, []);

  const create = async (e) => {
    e.preventDefault();
    if (!name.trim()) return;
    try {
      const res = await API.post('/api/doctors', { name, available: true });
      setDoctors(d => [res.data, ...d]);
      setName('');
      showToast("Doctor created successfully ✅", "success");
    } catch (err) {
      console.error(err);
      showToast('Failed to create doctor ❌', 'error');
    }
  };

  const toggleAvail = async (d) => {
    try {
      await API.post(`/api/doctors/${d.id}/availability`, null, { params: { available: !d.available } });
      setDoctors(prev => prev.map(x => x.id === d.id ? { ...x, available: !x.available } : x));
      showToast("Doctor availability updated ✅", "success");
    } catch (err) {
      console.error(err);
      showToast('Failed to update availability ❌', 'error');
    }
  };

  return (
    <section className="page container">
      <h2>Doctors</h2>
      <form className="form-row" onSubmit={create}>
        <input placeholder="Doctor name" value={name} onChange={e=>setName(e.target.value)} />
        <button className="btn primary" type="submit">Add</button>
      </form>

      <div className="grid">
        {loading && <div className="muted">Loading...</div>}
        {doctors.map(d => (
          <div key={d.id} className="card doctor-card">
            <div>
              <div className="doc-name">{d.name}</div>
              <div className="muted">ID: {d.id}</div>
            </div>
            <div className="doc-actions">
              <div className={`badge ${d.available ? 'green' : 'red'}`}>{d.available ? 'Available' : 'Offline'}</div>
              <button className="btn ghost small" onClick={() => toggleAvail(d)}>
                {d.available ? 'Set Offline' : 'Set Available'}
              </button>
              <Link to={`/doctor/${d.id}`} className="btn small">Open</Link>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}

// Reception Page
function ReceptionPage({ showToast }) {
  const [doctors, setDoctors] = useState([]);
  const [selectedDoctor, setSelectedDoctor] = useState('');
  const [patientName, setPatientName] = useState('');
  const [age, setAge] = useState('');
  const [contact, setContact] = useState('');
  const [waiting, setWaiting] = useState([]);

  const fetchDoctors = async () => {
    try {
      const res = await API.get('/api/doctors');
      setDoctors(res.data || []);
    } catch (e) { console.error(e); }
  };

  const fetchWaiting = async () => {
    if (!selectedDoctor) return;
    try {
      const r = await API.get(`/api/queue/${selectedDoctor}/waiting`);
      setWaiting(r.data || []);
    } catch(e){ console.error(e); }
  };

  const createAndEnqueue = async (e) => {
    e.preventDefault();
    if (!selectedDoctor) return showToast('Select doctor first ❌', 'error');
    try {
      const patientRes = await API.post(`/api/queue/patients`, {
        name: patientName,
        age,
        contact
      });
      const patientId = patientRes.data.id;

      await API.post(`/api/queue/${selectedDoctor}/enqueue`, null, { params: { patientId } });

      setPatientName(''); setAge(''); setContact('');
      fetchWaiting();
      showToast("Patient created & enqueued successfully ✅", "success");
    } catch(err){ 
      console.error(err); 
      showToast("Failed to create & enqueue patient ❌", "error");
    }
  };

  useEffect(()=>{ fetchDoctors(); }, []);
  useEffect(()=>{ fetchWaiting(); }, [selectedDoctor]);

  return (
    <section className="page container">
      <h2>Reception — Enqueue patient</h2>
      <div className="form-row">
        <select value={selectedDoctor || ''} onChange={e=>setSelectedDoctor(e.target.value)}>
          <option value="">Select Doctor</option>
          {doctors.map(d => <option key={d.id} value={d.id}>{d.name} {d.available? '(Available)':''}</option>)}
        </select>
      </div>

      <form className="card form" onSubmit={createAndEnqueue}>
        <div className="form-grid">
          <input value={patientName} onChange={e=>setPatientName(e.target.value)} placeholder="Patient name" required />
          <input value={age} onChange={e=>setAge(e.target.value)} placeholder="Age" />
          <input value={contact} onChange={e=>setContact(e.target.value)} placeholder="Contact phone" required />
        </div>
        <div className="form-actions">
          <button className="btn primary" type="submit">Create & Enqueue</button>
          <button type="button" className="btn ghost" onClick={fetchWaiting}>Refresh waiting</button>
        </div>
      </form>

      <h3>Waiting list</h3>
      <div className="grid small">
        {waiting.length === 0 && <div className="muted">No waiting patients</div>}
        {waiting.map(w => (
          <div key={w.tokenId} className="card">
            <div className="row-sb">
              <div>
                <div className="token">#{w.tokenNumber}</div>
                <div className="muted">{w.patientName}</div>
              </div>
              <div className="meta">
                <div className="muted">Pos {w.position}</div>
                <div className="muted">ETA {fmtETA(w.etaSeconds)}</div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}

// Doctor Dashboard
function DoctorDashboard({ showToast }) {
  const { id } = useParams();
  const doctorId = id;
  const [waiting, setWaiting] = useState([]);
  const [pending, setPending] = useState([]);
  const [completed, setCompleted] = useState([]);
  const esRef = useRef(null);

  const refreshAll = async () => {
    try {
      const w = await API.get(`/api/queue/${doctorId}/waiting`);
      const p = await API.get(`/api/queue/${doctorId}/pending`);
      const c = await API.get(`/api/queue/${doctorId}/completed`);
      setWaiting(w.data || []);
      setPending(p.data || []);
      setCompleted(c.data || []);
    } catch(e){ console.error(e); }
  };

  useEffect(() => {
    refreshAll();
    const url = `${API_BASE}/api/queue/stream/${doctorId}`;
    try {
      const es = new EventSource(url);
      es.addEventListener('queue-update', () => refreshAll());
      es.onopen = () => console.log('SSE open');
      es.onerror = (e) => console.log('SSE error', e);
      esRef.current = es;
      return () => { es.close(); };
    } catch (err) {
      console.warn('SSE not available', err);
    }
  }, [doctorId]);

  const callNext = async () => {
    try { 
      await API.post(`/api/queue/${doctorId}/call-next`); 
      refreshAll(); 
      showToast("Called next ✅", "success");
    } catch(e){ 
      console.error(e); 
      showToast('No waiting or error ❌', "error"); 
    }
  };

  const markServed = async (tokenId) => {
    try { 
      await API.post(`/api/queue/tokens/${tokenId}/served`); 
      refreshAll(); 
      showToast("Marked served ✅", "success");
    } catch(e){ console.error(e); showToast("Error marking served ❌", "error"); }
  };

  const markAbsent = async (tokenId) => {
    try { 
      await API.post(`/api/queue/tokens/${tokenId}/absent`); 
      refreshAll(); 
      showToast("Marked absent ✅", "success");
    } catch(e){ console.error(e); showToast("Error marking absent ❌", "error"); }
  };

  return (
    <section className="page container">
      <div className="row-sb">
        <h2>Doctor Dashboard — ID {doctorId}</h2>
        <div>
          <button className="btn primary" onClick={callNext}>Call Next</button>
        </div>
      </div>

      <div className="grid two-cols">
        <div>
          <h3>Waiting</h3>
          {waiting.length === 0 && <div className="muted">No waiting patients</div>}
          {waiting.map(w => (
            <div className="card" key={w.tokenId}>
              <div className="row-sb">
                <div>
                  <div className="token">#{w.tokenNumber}</div>
                  <div className="muted">{w.patientName} — Pos {w.position}</div>
                  <div className="muted">ETA {fmtETA(w.etaSeconds)}</div>
                </div>
                <div className="stack">
                  <button className="btn small" onClick={()=>markServed(w.tokenId)}>Serve</button>
                  <button className="btn ghost small" onClick={()=>markAbsent(w.tokenId)}>Absent</button>
                </div>
              </div>
            </div>
          ))}
        </div>
        <div>
          <h3>Pending / Called</h3>
          {pending.length === 0 && <div className="muted">No pending tokens</div>}
          {pending.map(p => (
            <div className="card small" key={p.id}>
              <div className="row-sb">
                <div>
                  <div className="muted">#{p.tokenNumber} — {p.patient?.name || '—'}</div>
                </div>
                <div>
                  <button className="btn small" onClick={()=>markServed(p.id)}>Mark served</button>
                </div>
              </div>
            </div>
          ))}

          <h3 style={{marginTop: 18}}>Completed (Today)</h3>
          {completed.length === 0 && <div className="muted">No completed services</div>}
          {completed.map(c => (
            <div key={c.id} className="card muted small">
              #{c.tokenNumber} — {c.patient?.name || '—'} — {c.serviceSeconds || '-'}s
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

// Footer
function Footer(){
  return (
    <footer className="footer">
      <div className="container">SQMS — Smart Queue Management System • Built with React</div>
    </footer>
  );
}

// App
export default function App(){
  const [toast, setToast] = useState({ message: '', type: 'success' });

  const showToast = (msg, type='success') => {
    setToast({ message: msg, type });
    setTimeout(() => setToast({ message: '', type }), 3000);
  };

  return (
    <div className="app-root">
      <NavBar />
      <Toast message={toast.message} type={toast.type} onClose={()=>setToast({ message: '', type: 'success' })}/>
      <Routes>
        <Route path="/" element={<Home/>} />
        <Route path="/doctors" element={<DoctorsPage showToast={showToast}/>} />
        <Route path="/reception" element={<ReceptionPage showToast={showToast}/>} />
        <Route path="/doctor/:id" element={<DoctorDashboard showToast={showToast}/>} />
        <Route path="*" element={<main className="container page"><h2>Not found</h2><p>Try the main menu.</p></main>} />
      </Routes>
      <Footer />
    </div>
  );
}
