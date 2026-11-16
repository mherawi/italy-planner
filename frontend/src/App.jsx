import React, { useEffect, useRef, useState } from 'react';
import './App.css';
import Account from './components/Account.jsx';
import PlaceCatalog from './components/PlaceCatalog.jsx';
import PlaceDetails from './components/PlaceDetails.jsx';
import Itinerary from './components/Itinerary.jsx';
import { usePlaces } from './hooks/usePlaces.js';
import { useItinerary } from './hooks/useItinerary.js';

export default function App() {
  const [profile, setProfile] = useState(null);
  const { places, status, retry } = usePlaces();

  return (
    <>
      <header className="site-header">
        <div className="shell topbar">
          <a className="brand" href="#top" aria-label="Three Days in Italy home">
            <span className="brand-mark" aria-hidden="true">✦</span> Three Days<span className="brand-tail"> in Italy</span>
          </a>
          <nav aria-label="Main navigation">
            <a href="#explore">Explore</a>
            <a className="nav-trip" href="#itinerary">My itinerary <span aria-hidden="true">↗</span></a>
            <Account profile={profile} setProfile={setProfile} />
          </nav>
        </div>
      </header>

      <main className="shell" id="top">
        <section className="hero" aria-labelledby="page-title">
          <div className="hero-copy">
            <h1 id="page-title">Three days<br />in <em>Italy.</em></h1>
            <p>Find the places you’d love to visit and turn them into a trip of your own.</p>
          </div>
        </section>

        <Planner key={profile?.email ?? ''} email={profile?.email} places={places} status={status} retry={retry} />
      </main>
    </>
  );
}

const itineraryUrl = '/api/account/itinerary';

function Planner({ email, ...catalog }) {
  const [plan, setPlan] = useState(undefined);
  const [error, setError] = useState('');
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    if (!email) return;
    const controller = new AbortController();
    fetch(itineraryUrl, {
      signal: controller.signal,
      cache: 'no-store',
      headers: { 'X-Planner-Account': encodeURIComponent(email) },
    }).then(response => {
      if (!response.ok) throw new Error('Could not load your saved itinerary.');
      return response.status === 204 ? null : response.json();
    }).then(saved => {
      if (!controller.signal.aborted) setPlan(saved);
    }).catch(() => {
      if (!controller.signal.aborted) setError('Could not load your saved itinerary.');
    });
    return () => controller.abort();
  }, [email, attempt]);

  if (email && plan === undefined) {
    return (
      <section id="itinerary" className="catalog-status">
        <p role="status">{error || 'Loading your saved itinerary…'}</p>
        {error && <button type="button" onClick={() => {
          setError('');
          setAttempt(current => current + 1);
        }}>Try again</button>}
      </section>
    );
  }
  return <PlannerWorkspace {...catalog} email={email} accountPlan={plan} />;
}

function PlannerWorkspace({ places, status, retry, email, accountPlan }) {
  const byId = new Map(places.map(place => [place.id, place]));
  const { days, activeDay, setActiveDay, dayPlaces, add, remove, reorder, addDay, removeLastDay,
    announcement, startDate, setStartDate } = useItinerary(byId, accountPlan);
  const [selected, setSelected] = useState(null);
  const [savedPlan, setSavedPlan] = useState(() => accountPlan &&
    JSON.stringify({ days: accountPlan.days, startDate: accountPlan.startDate }));
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');
  const dialog = useRef(null);
  const plan = JSON.stringify({ days, startDate });

  async function save() {
    setSaving(true);
    setSaveError('');
    try {
      const response = await fetch(itineraryUrl, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'X-Planner-Request': 'account',
          'X-Planner-Account': encodeURIComponent(email),
        },
        body: plan,
      });
      if (response.status === 401) {
        setSaveError('Sign in again to save your itinerary.');
      } else if (!response.ok) {
        setSaveError('Could not save your itinerary. Try again.');
      } else {
        setSavedPlan(plan);
      }
    } catch {
      setSaveError('Could not save your itinerary. Try again.');
    } finally {
      setSaving(false);
    }
  }

  function showDetails(place) {
    setSelected(place);
    dialog.current.showModal();
  }

  return (
    <>
      <div className="workspace">
          <PlaceCatalog places={places} status={status} retry={retry} scheduledIds={days[activeDay]}
            activeDay={activeDay} add={add} showDetails={showDetails} />
          <Itinerary days={days} activeDay={activeDay} setActiveDay={setActiveDay} dayPlaces={dayPlaces}
            addDay={addDay} removeLastDay={removeLastDay} startDate={startDate} setStartDate={setStartDate}
            showDetails={showDetails} reorder={reorder} remove={remove}
            save={email ? save : undefined} saving={saving} saved={savedPlan === plan} saveError={saveError} />
      </div>

      <PlaceDetails dialog={dialog} selected={selected} activeDay={activeDay}
        scheduled={days[activeDay].includes(selected?.id)} add={add} />
      <div className="sr-only" role="status">{announcement}</div>
    </>
  );
}
