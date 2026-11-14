import React from 'react';
import { duration } from '../format.js';
import WalkingTime from './WalkingTime.jsx';

export default function Itinerary({ days, activeDay, setActiveDay, dayPlaces, showDetails, reorder, remove }) {
  const cityCount = new Set(dayPlaces.map(place => place.city)).size;
  const minutes = dayPlaces.reduce((total, place) => total + (place.duration_minutes ?? 0), 0);

  return (
    <aside id="itinerary" className="itinerary" aria-labelledby="trip-title">
      <div className="itinerary-heading">
        <h2 id="trip-title">My itinerary</h2>
        <span className="trip-duration">3 days</span>
      </div>
      <div className="days" role="group" aria-label="Choose itinerary day">
        {days.map((day, index) => (
          <button key={index} aria-pressed={activeDay === index} onClick={() => setActiveDay(index)}>
            Day {index + 1}{day.length > 0 && <small>{day.length}</small>}
          </button>
        ))}
      </div>
      <div className="day-summary">
        <span>{dayPlaces.length} {dayPlaces.length === 1 ? 'stop' : 'stops'}</span>
        {minutes > 0 && <span>{duration(minutes)} visiting</span>}
      </div>
      {dayPlaces.length === 0 ? (
        <div className="empty-day">
          <span aria-hidden="true">✦</span>
          <strong>Day {activeDay + 1} is open</strong>
          <p>Use the add button on a place to put it on this day.</p>
        </div>
      ) : (
        <ol className="stops">
          {dayPlaces.map((place, index) => (
            <li key={place.id}>
              <span className="stop-number" aria-hidden="true">{index + 1}</span>
              <div className="stop-body">
                <button className="stop-title" onClick={() => showDetails(place)}>{place.name}</button>
                <p>{[place.city, place.duration_minutes ? duration(place.duration_minutes) : ''].filter(Boolean).join(' · ')}</p>
                {index > 0 && <WalkingTime key={dayPlaces[index - 1].id} from={dayPlaces[index - 1]} to={place} />}
                <div className="stop-actions">
                  <button aria-label={'Move ' + place.name + ' earlier'} disabled={index === 0} onClick={() => reorder(place.id, -1)}>↑</button>
                  <button aria-label={'Move ' + place.name + ' later'} disabled={index === dayPlaces.length - 1} onClick={() => reorder(place.id, 1)}>↓</button>
                  <button aria-label={'Remove ' + place.name} onClick={() => remove(place.id)}>Remove</button>
                </div>
              </div>
            </li>
          ))}
        </ol>
      )}
      {cityCount > 1 && <p className="trip-warning">This day includes multiple cities. Check travel time between stops.</p>}
      {minutes > 480 && <p className="trip-warning">Visits add up to more than 8 hours before travel.</p>}
      <p className="save-note">Your plan stays in this browser.</p>
    </aside>
  );
}
