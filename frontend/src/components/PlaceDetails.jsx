import React from 'react';
import './PlaceDetails.css';
import { duration, typeName } from '../format.js';

export default function PlaceDetails({ dialog, selected, activeDay, scheduled, add }) {
  return (
    <dialog ref={dialog} aria-labelledby="detail-title" className="details">
      {selected && <>
        <div className="detail-top">
          <span className="category-label clay">{typeName(selected)}</span>
          <form method="dialog"><button className="close" aria-label="Close details">×</button></form>
        </div>
        <h2 id="detail-title">{selected.name}</h2>
        <p className="location">{[selected.city, selected.neighborhood].filter(Boolean).join(' · ')}</p>
        <p>{selected.description}</p>
        <dl>
          <dt>Opening hours</dt><dd>{selected.hours || 'Verify opening hours'}</dd>
          <dt>Typical visit</dt><dd>{selected.duration_minutes ? duration(selected.duration_minutes) : 'Unlisted'}</dd>
          <dt>Price range</dt><dd>{selected.price_range || 'Unlisted'}</dd>
        </dl>
        {selected.seasonal_notes && <p className="detail-note">{selected.seasonal_notes}</p>}
        {selected.booking_required && <p className="detail-note">Booking required</p>}
        <button className="add-button" disabled={scheduled} onClick={() => add(selected.id)}>
          {scheduled ? '✓ Added to Day ' + (activeDay + 1) : '+ Add to Day ' + (activeDay + 1)}
        </button>
      </>}
    </dialog>
  );
}
