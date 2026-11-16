import React from 'react';
import './PlaceCard.css';
import { duration, typeName } from '../format.js';

const colors = ['clay', 'olive', 'blue', 'sand'];

export default function PlaceCard({ place, scheduled, activeDay, add, showDetails, index }) {
  return (
    <article className="place-card">
      <div className="card-top">
        <span className={'category-label ' + colors[index % colors.length]}>{typeName(place)}</span>
        {place.rating != null && <span className="rating"><span aria-hidden="true">★</span> {place.rating.toFixed(1)}</span>}
      </div>
      <button className="place-title" onClick={() => showDetails(place)}>{place.name}</button>
      <p className="location">{[place.city, place.neighborhood].filter(Boolean).join(' · ')}</p>
      <p className="description">{place.description}</p>
      <div className="card-footer">
        <span>{[place.duration_minutes ? duration(place.duration_minutes) : '', place.price_range].filter(Boolean).join(' · ')}</span>
        <button className="add-button" disabled={scheduled} onClick={() => add(place.id)}>
          {scheduled ? '✓ Added' : '+ Day ' + (activeDay + 1)}
        </button>
      </div>
    </article>
  );
}
