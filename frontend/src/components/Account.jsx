import React, { useEffect, useRef, useState } from 'react';
import './Account.css';

async function accountRequest(action, credentials) {
  return fetch('/api/account/' + action, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Planner-Request': 'account' },
    body: credentials ? JSON.stringify(credentials) : undefined,
  });
}

export default function Account({ profile, setProfile }) {
  const [mode, setMode] = useState('login');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [ready, setReady] = useState(false);
  const dialog = useRef(null);

  useEffect(() => {
    const controller = new AbortController();
    fetch('/api/account', { signal: controller.signal, cache: 'no-store' })
      .then(response => response.ok ? response.json() : null)
      .then(setProfile)
      .catch(() => {})
      .finally(() => {
        if (!controller.signal.aborted) setReady(true);
      });
    return () => controller.abort();
  }, [setProfile]);

  function openAccount() {
    setError('');
    setMode('login');
    dialog.current.showModal();
  }

  async function submit(event) {
    event.preventDefault();
    if (busy) return;
    const form = event.currentTarget;
    const credentials = Object.fromEntries(new FormData(form));
    setError('');
    setBusy(true);
    try {
      const response = await accountRequest(mode, credentials);
      if (response.status === 401) throw new Error('Email or password is incorrect.');
      if (response.status === 409) throw new Error('That email already has an account.');
      if (response.status === 400) throw new Error('Enter a valid email and password. Passwords can use up to 72 UTF-8 bytes.');
      if (!response.ok) throw new Error('Could not complete the request. Try again.');
      setProfile(await response.json());
      form.reset();
      dialog.current.close();
    } catch (failure) {
      setError(failure instanceof TypeError ? 'Could not reach the server. Try again.' : failure.message);
    } finally {
      setBusy(false);
    }
  }

  async function signOut() {
    setError('');
    setBusy(true);
    try {
      if (!(await accountRequest('logout')).ok) throw new Error('Sign-out failed');
      setProfile(null);
    } catch {
      setError('Could not sign out. Try again.');
    } finally {
      setBusy(false);
    }
  }

  const registering = mode === 'register';
  return (
    <div className="account">
      <button type="button" className="nav-trip account-button" onClick={openAccount} disabled={!ready}>Account</button>
      <dialog ref={dialog} className="details" aria-labelledby="account-title" onClose={() => setError('')}>
        <div className="detail-top">
          <h2 id="account-title">{profile ? 'Your account' : registering ? 'Create account' : 'Sign in'}</h2>
          <form method="dialog"><button className="close" aria-label="Close account">×</button></form>
        </div>
        {profile ? (
          <div className="account-form">
            <p>Signed in as <strong>{profile.email}</strong></p>
            <button type="button" className="add-button" disabled={busy} onClick={signOut}>Sign out</button>
          </div>
        ) : (
          <form className="account-form" onSubmit={submit}>
            <label>Email <input name="email" type="email" autoComplete="username" required maxLength={254} /></label>
            <label>Password <input key={mode} name="password" type="password"
              autoComplete={registering ? 'new-password' : 'current-password'} required
              minLength={registering ? 8 : undefined} maxLength={72} /></label>
            {registering && <p>Use at least 8 characters.</p>}
            <button type="submit" className="add-button" disabled={busy}>{registering ? 'Create account' : 'Sign in'}</button>
            <button type="button" className="account-switch" disabled={busy}
              onClick={() => { setError(''); setMode(registering ? 'login' : 'register'); }}>
              {registering ? 'Already registered? Sign in' : 'New here? Create an account'}
            </button>
          </form>
        )}
        {error && <p className="trip-warning" role="alert">{error}</p>}
      </dialog>
    </div>
  );
}
