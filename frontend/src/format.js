export function duration(minutes) {
  const hours = Math.floor(minutes / 60);
  const remainder = minutes % 60;
  return [hours ? hours + 'h' : '', remainder ? remainder + 'm' : ''].filter(Boolean).join(' ') || '0m';
}

export function typeName(place) {
  return place.type.replaceAll('_', ' ');
}

const itineraryDateFormat = new Intl.DateTimeFormat(undefined, {
  month: 'short',
  day: 'numeric',
  timeZone: 'UTC',
});

export function itineraryDate(startDate, dayIndex) {
  if (!startDate) return null;
  const date = new Date(startDate + 'T00:00:00Z');
  if (Number.isNaN(date.getTime())) return null;
  date.setUTCDate(date.getUTCDate() + dayIndex);
  return { value: date.toISOString().split('T')[0], label: itineraryDateFormat.format(date) };
}
