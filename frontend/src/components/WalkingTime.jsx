import React, { useEffect, useState } from 'react';
import { duration } from '../format.js';

export default function WalkingTime({ from, to }) {
  const [result, setResult] = useState(null);
  const sameCity = from.city.toLowerCase() === to.city.toLowerCase();

  useEffect(() => {
    if (!sameCity) return;
    const controller = new AbortController();
    const query = new URLSearchParams({ from: from.id, to: to.id });

    fetch('/api/routes/walking?' + query, { signal: controller.signal })
      .then(response => {
        if (!response.ok) throw new Error('Walking time unavailable');
        return response.json();
      })
      .then(setResult)
      .catch(error => {
        if (error.name !== 'AbortError') setResult({ source: 'unavailable' });
      });

    return () => controller.abort();
  }, [from.id, to.id, sameCity]);

  let label;
  if (!sameCity) {
    label = 'Check intercity travel';
  } else if (!result) {
    label = 'Estimating walking time…';
  } else if (result.source === 'openrouteservice') {
    label = <>~{duration(result.minutes)} walk · <a href="https://openrouteservice.org/" target="_blank" rel="noreferrer">openrouteservice</a> © OpenStreetMap contributors</>;
  } else if (result.source === 'straight_line') {
    label = 'Straight-line estimate · ~' + duration(result.minutes) + ' at walking pace';
  } else if (result.source === 'same_location') {
    label = 'Same mapped location';
  } else {
    label = 'Walking time unavailable';
  }

  return <p className="walking-time" aria-live="polite">{label}</p>;
}
