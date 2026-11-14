export function duration(minutes) {
  const hours = Math.floor(minutes / 60);
  const remainder = minutes % 60;
  return [hours ? hours + 'h' : '', remainder ? remainder + 'm' : ''].filter(Boolean).join(' ') || '0m';
}

export function typeName(place) {
  return place.type.replaceAll('_', ' ');
}
