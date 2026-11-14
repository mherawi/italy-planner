import React, { useRef, useState } from 'react';
import Account from './components/Account.jsx';
import PlaceCatalog from './components/PlaceCatalog.jsx';
import PlaceDetails from './components/PlaceDetails.jsx';
import Itinerary from './components/Itinerary.jsx';
import { usePlaces } from './hooks/usePlaces.js';
import { useItinerary } from './hooks/useItinerary.js';

export default function App() {
  const [profile, setProfile] = useState(null);
  const { places, status, retry } = usePlaces();
  const byId = new Map(places.map(place => [place.id, place]));
  const { days, activeDay, setActiveDay, dayPlaces, add, remove, reorder, announcement } = useItinerary(byId);
  const [selected, setSelected] = useState(null);
  const dialog = useRef(null);

  function showDetails(place) {
    setSelected(place);
    dialog.current.showModal();
  }

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
            <a className="hero-link" href="#explore">Explore places <span aria-hidden="true">↓</span></a>
          </div>
        </section>

        <div className="workspace">
          <PlaceCatalog places={places} status={status} retry={retry} scheduledIds={days[activeDay]}
            activeDay={activeDay} add={add} showDetails={showDetails} />
          <Itinerary days={days} activeDay={activeDay} setActiveDay={setActiveDay} dayPlaces={dayPlaces}
            showDetails={showDetails} reorder={reorder} remove={remove} />
        </div>
      </main>

      <PlaceDetails dialog={dialog} selected={selected} activeDay={activeDay}
        scheduled={days[activeDay].includes(selected?.id)} add={add} />
      <div className="sr-only" role="status">{announcement}</div>
    </>
  );
}
