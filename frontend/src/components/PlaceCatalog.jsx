import React, { useState } from 'react';
import PlaceCard from './PlaceCard.jsx';

const categories = [
  ['All places', []],
  ['Food & drink', ['restaurant', 'cafe', 'bar', 'market', 'vineyard', 'winery', 'food_shop', 'bakery', 'gelateria', 'food_experience']],
  ['Art & history', ['historic_site', 'museum', 'church', 'architecture', 'gallery', 'palace', 'monument']],
  ['Outdoors', ['park', 'garden', 'beach', 'viewpoint', 'outdoor', 'hiking', 'nature']],
];

export default function PlaceCatalog({ places, status, retry, scheduledIds, activeDay, add, showDetails }) {
  const [city, setCity] = useState('');
  const [tag, setTag] = useState('');
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState(0);
  const [limit, setLimit] = useState(8);

  const cities = [...new Set(places.map(place => place.city))].sort();
  const tags = [...new Set(places.flatMap(place => place.tags ?? []))].sort();
  const search = query.trim().toLowerCase();
  const results = places.filter(place => {
    const searchable = [place.name, place.city, place.neighborhood, place.description, ...(place.tags ?? [])];
    return (!city || place.city === city) &&
      (!tag || place.tags?.includes(tag)) &&
      (!category || categories[category][1].includes(place.type)) &&
      (!search || searchable.some(value => value?.toLowerCase().includes(search)));
  });

  function changeFilter(setter, value) {
    setter(value);
    setLimit(8);
  }

  function resetFilters() {
    setCity('');
    setTag('');
    setQuery('');
    setCategory(0);
    setLimit(8);
  }

  return (
    <section id="explore" aria-labelledby="explore-title">
      <div className="section-heading">
        <h2 id="explore-title">Explore places</h2>
        {status === 'ready' && <span aria-live="polite">{results.length} {results.length === 1 ? 'place' : 'places'}</span>}
      </div>
      <div className="filters">
        <label className="search-label">
          <span className="sr-only">Search places</span>
          <span aria-hidden="true" className="search-icon">⌕</span>
          <input placeholder="Search places" type="search" value={query} onChange={event => changeFilter(setQuery, event.target.value)} />
        </label>
        <label>
          <span className="sr-only">City</span>
          <select value={city} onChange={event => changeFilter(setCity, event.target.value)}>
            <option value="">All cities</option>
            {cities.map(value => <option key={value}>{value}</option>)}
          </select>
        </label>
        <label>
          <span className="sr-only">Interest</span>
          <select value={tag} onChange={event => changeFilter(setTag, event.target.value)}>
            <option value="">All interests</option>
            {tags.map(value => <option key={value} value={value}>{value.replaceAll('-', ' ')}</option>)}
          </select>
        </label>
      </div>
      <div className="chips" role="group" aria-label="Place category">
        {categories.map(([label], index) => (
          <button key={label} aria-pressed={category === index} onClick={() => changeFilter(setCategory, index)}>{label}</button>
        ))}
      </div>

      {status === 'loading' && <p className="catalog-status" role="status">Loading places…</p>}
      {status === 'error' && <div className="catalog-status" role="alert">
        <p>Couldn’t load the place catalog.</p>
        <button onClick={retry}>Try again</button>
      </div>}
      {status === 'ready' && results.length === 0 && <div className="catalog-status">
        <p>No places match these filters.</p>
        <button onClick={resetFilters}>Clear filters</button>
      </div>}
      <div className="places">
        {results.slice(0, limit).map((place, index) => (
          <PlaceCard key={place.id} place={place} index={index} scheduled={scheduledIds.includes(place.id)}
            activeDay={activeDay} add={add} showDetails={showDetails} />
        ))}
      </div>
      {results.length > limit && <button className="load-more" onClick={() => setLimit(limit + 8)}>Show more places</button>}
    </section>
  );
}
