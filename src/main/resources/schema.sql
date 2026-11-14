CREATE TABLE IF NOT EXISTS places (
    id text PRIMARY KEY,
    name text NOT NULL,
    type text NOT NULL,
    city text NOT NULL,
    region text,
    data jsonb NOT NULL
);

CREATE TABLE IF NOT EXISTS place_import_lock (
    id integer PRIMARY KEY
);

INSERT INTO place_import_lock (id) VALUES (1) ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS planner_users (
    email text PRIMARY KEY,
    password_hash text NOT NULL
);
