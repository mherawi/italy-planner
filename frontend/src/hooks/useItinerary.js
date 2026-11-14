import { useEffect, useState } from 'react';

const storageKey = 'italy-planner-days-v1';

function loadDays() {
  try {
    const saved = JSON.parse(localStorage.getItem(storageKey));
    if (Array.isArray(saved) && saved.length > 0 &&
        saved.every(day => Array.isArray(day) && day.every(id => typeof id === 'string'))) {
      return saved;
    }
  } catch {
    // The planner still works when browser storage is unavailable.
  }
  return [[], [], []];
}

export function useItinerary(byId) {
  const [days, setDays] = useState(loadDays);
  const [activeDay, setActiveDay] = useState(0);
  const [announcement, setAnnouncement] = useState('');

  useEffect(() => {
    try {
      localStorage.setItem(storageKey, JSON.stringify(days));
    } catch {
      // Keep the current plan in memory.
    }
  }, [days]);

  const dayPlaces = days[activeDay].map(id => byId.get(id)).filter(Boolean);

  function addDay() {
    setDays(current => [...current, []]);
    setActiveDay(days.length);
    setAnnouncement('Day ' + (days.length + 1) + ' added');
  }

  function removeLastDay() {
    if (days.length === 1) return;
    const lastDay = days.at(-1);
    if (lastDay.length > 0 &&
        !window.confirm('Remove Day ' + days.length + ' and all stops on that day?')) return;

    setDays(current => current.slice(0, -1));
    setActiveDay(current => Math.min(current, days.length - 2));
    setAnnouncement('Day ' + days.length + ' removed');
  }

  function add(id) {
    setDays(current => current.map((day, index) =>
      index === activeDay && !day.includes(id) ? [...day, id] : day));
    setAnnouncement(byId.get(id).name + ' added to Day ' + (activeDay + 1));
  }

  function remove(id) {
    setDays(current => current.map((day, index) => index === activeDay ? day.filter(stop => stop !== id) : day));
    setAnnouncement(byId.get(id).name + ' removed from Day ' + (activeDay + 1));
  }

  function reorder(id, offset) {
    setDays(current => current.map((day, index) => {
      if (index !== activeDay) return day;
      const from = day.indexOf(id);
      const to = from + offset;
      if (from < 0 || to < 0 || to >= day.length) return day;
      const order = [...day];
      [order[from], order[to]] = [order[to], order[from]];
      return order;
    }));
    setAnnouncement(byId.get(id).name + (offset < 0 ? ' moved earlier' : ' moved later'));
  }

  return { days, activeDay, setActiveDay, dayPlaces, add, remove, reorder, addDay, removeLastDay, announcement };
}
