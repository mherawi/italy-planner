import assert from 'node:assert/strict';
import { afterEach, beforeEach, test } from 'node:test';
import { act, createElement } from 'react';
import { createRoot } from 'react-dom/client';
import { JSDOM } from 'jsdom';
import { useItinerary } from '../src/hooks/useItinerary.js';
import { itineraryDate } from '../src/format.js';

const storageKey = 'italy-planner-days-v1';
const gallery = { id: 'gallery', name: 'Gallery' };
const market = { id: 'market', name: 'Market' };
const places = new Map([[gallery.id, gallery], [market.id, market]]);

let browser;
let root;
let planner;

beforeEach(() => {
  browser = new JSDOM('<div id="app"></div>', { url: 'http://localhost' });
  globalThis.window = browser.window;
  globalThis.document = window.document;
  globalThis.localStorage = window.localStorage;
  globalThis.IS_REACT_ACT_ENVIRONMENT = true;
  root = createRoot(document.getElementById('app'));
});

afterEach(() => {
  act(() => root.unmount());
  browser.window.close();
  delete globalThis.IS_REACT_ACT_ENVIRONMENT;
  delete globalThis.localStorage;
  delete globalThis.document;
  delete globalThis.window;
});

function mountPlanner() {
  function Planner() {
    planner = useItinerary(places);
    return null;
  }

  act(() => root.render(createElement(Planner)));
}

test('defaults to three days when a saved itinerary is invalid', () => {
  localStorage.setItem(storageKey, '[[12]]');
  mountPlanner();

  assert.deepEqual(planner.days, [[], [], []]);
  assert.equal(planner.activeDay, 0);
});

test('restores, updates, and clears the optional trip start date', () => {
  const dateKey = 'italy-planner-start-date';
  localStorage.setItem(dateKey, '2028-02-28');
  mountPlanner();
  assert.equal(planner.startDate, '2028-02-28');

  act(() => planner.setStartDate('2028-03-01'));
  assert.equal(localStorage.getItem(dateKey), '2028-03-01');
  act(() => planner.setStartDate(''));
  assert.equal(localStorage.getItem(dateKey), null);
});

test('assigns consecutive calendar dates across leap day and month boundaries', () => {
  assert.equal(itineraryDate('', 0), null);
  assert.equal(itineraryDate('2028-02-28', 1).value, '2028-02-29');
  assert.equal(itineraryDate('2028-02-28', 2).value, '2028-03-01');
});

test('restores a saved itinerary with a custom number of days', () => {
  localStorage.setItem(storageKey, JSON.stringify([[gallery.id]]));
  mountPlanner();

  assert.deepEqual(planner.days, [[gallery.id]]);
  assert.deepEqual(planner.dayPlaces, [gallery]);
  act(() => planner.removeLastDay());
  assert.equal(planner.days.length, 1);
});

test('adds, reorders, and removes stops on the active day and saves the result', () => {
  mountPlanner();
  act(() => planner.add(gallery.id));
  act(() => planner.setActiveDay(1));
  act(() => planner.add(gallery.id));
  act(() => planner.add(gallery.id));
  act(() => planner.add(market.id));

  assert.deepEqual(planner.dayPlaces, [gallery, market]);
  act(() => planner.reorder(gallery.id, -1));
  assert.deepEqual(planner.dayPlaces, [gallery, market]);
  act(() => planner.reorder(market.id, -1));
  assert.deepEqual(planner.dayPlaces, [market, gallery]);
  act(() => planner.remove(gallery.id));

  assert.deepEqual(planner.days, [[gallery.id], [market.id], []]);
  assert.deepEqual(JSON.parse(localStorage.getItem(storageKey)), planner.days);
});

test('selects a new day and removes it with its stops without prompting', context => {
  mountPlanner();
  act(() => planner.addDay());
  assert.equal(planner.days.length, 4);
  assert.equal(planner.activeDay, 3);
  act(() => planner.add(gallery.id));

  const confirm = context.mock.method(window, 'confirm', () => false);
  act(() => planner.removeLastDay());
  assert.equal(confirm.mock.callCount(), 0);
  assert.deepEqual(planner.days, [[], [], []]);
  assert.equal(planner.activeDay, 2);
});
