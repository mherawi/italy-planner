import { useEffect, useState } from 'react';

export function usePlaces() {
  const [places, setPlaces] = useState([]);
  const [status, setStatus] = useState('loading');
  const [request, setRequest] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    fetch('/api/places', { signal: controller.signal })
      .then(response => {
        if (!response.ok) throw new Error('Catalog request failed');
        return response.json();
      })
      .then(data => {
        setPlaces(data);
        setStatus('ready');
      })
      .catch(error => {
        if (error.name !== 'AbortError') setStatus('error');
      });
    return () => controller.abort();
  }, [request]);

  function retry() {
    setStatus('loading');
    setRequest(current => current + 1);
  }

  return { places, status, retry };
}
