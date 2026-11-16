import React from 'react';
import './Itinerary.css';
import { duration, itineraryDate } from '../format.js';
import WalkingTime from './WalkingTime.jsx';

export default function Itinerary({ days, activeDay, setActiveDay, dayPlaces, showDetails, reorder, remove,
  addDay, removeLastDay, startDate, setStartDate, save, saving, saved, saveError }) {
  const cityCount = new Set(dayPlaces.map(place => place.city)).size;
  const minutes = dayPlaces.reduce((total, place) => total + (place.duration_minutes ?? 0), 0);

  return (
    <aside id="itinerary" className="itinerary" aria-labelledby="trip-title">
      <div className="itinerary-heading">
        <h2 id="trip-title">My itinerary</h2>
        <span className="trip-duration">{days.length} {days.length === 1 ? 'day' : 'days'}</span>
      </div>
      <div className="trip-date">
        <label htmlFor="trip-start-date">Trip start date</label>
        <input id="trip-start-date" type="date" value={startDate} onChange={event => setStartDate(event.target.value)} />
        {startDate && <button type="button" className="add-button" onClick={() => setStartDate('')}>Clear date</button>}
      </div>
      <div className="day-controls">
        <button type="button" className="add-button" onClick={addDay}>Add day</button>
        <button type="button" className="add-button" disabled={days.length === 1} onClick={removeLastDay}>Remove last day</button>
      </div>
      <div className="days" role="group" aria-label="Choose itinerary day">
        {days.map((_, index) => {
          const date = itineraryDate(startDate, index);
          return (
            <button key={index} aria-pressed={activeDay === index} onClick={() => setActiveDay(index)}>
              <span>Day {index + 1}</span>
              {date && <time dateTime={date.value}>{date.label}</time>}
            </button>
          );
        })}
      </div>
      <div className="day-summary">
        <span>{dayPlaces.length} {dayPlaces.length === 1 ? 'stop' : 'stops'}</span>
        {minutes > 0 && <span>{duration(minutes)} visiting</span>}
      </div>
      {dayPlaces.length > 6 && <p className="activity-note">You might have too many activities for one day.</p>}
      {dayPlaces.length === 0 ? (
        <div className="empty-day">
          <span aria-hidden="true">✦</span>
          <strong>Day {activeDay + 1} is open</strong>
          <p>Use the add button on a place to put it on this day.</p>
        </div>
      ) : (
        <ol className="stops" tabIndex={0} aria-label={'Activities for day ' + (activeDay + 1)}>
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
      {save ? (
        <div className="save-note">
          <button type="button" className="add-button" disabled={saving || saved} onClick={save}>
            {saving ? 'Saving…' : 'Save itinerary'}
          </button>
          <p role="status">{saveError || (saved ? 'Saved to your account.' : 'You have unsaved changes.')}</p>
        </div>
      ) : <p className="save-note">Your plan stays in this browser.</p>}
    </aside>
  );
}
