-- Tally schema. Free-text fields (habit names, notes) are stored AES-GCM encrypted
-- by the application, so they are unreadable to anyone with raw database access.

CREATE TABLE users (
    id               UUID PRIMARY KEY,
    email            VARCHAR(254) NOT NULL UNIQUE,
    password_hash    VARCHAR(100) NOT NULL,
    week_start       SMALLINT     NOT NULL DEFAULT 1,      -- ISO day: 1 = Monday, 7 = Sunday
    lock_pin_hash    VARCHAR(100),
    lock_minutes     SMALLINT     NOT NULL DEFAULT 0,      -- 0 = never auto-lock
    discreet_default BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE habits (
    id            UUID PRIMARY KEY,
    user_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name          TEXT        NOT NULL,                    -- encrypted
    alias         VARCHAR(40) NOT NULL,                    -- shown instead of name in discreet mode
    kind          VARCHAR(10) NOT NULL DEFAULT 'BUILD',    -- BUILD (do it) | AVOID (don't do it)
    weekly_target SMALLINT    NOT NULL CHECK (weekly_target BETWEEN 1 AND 7),
    is_private    BOOLEAN     NOT NULL DEFAULT FALSE,
    color         VARCHAR(9)  NOT NULL DEFAULT '#3987e5',
    position      INT         NOT NULL DEFAULT 0,
    start_date    DATE        NOT NULL,
    archived      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_habits_user ON habits(user_id, position);

CREATE TABLE entries (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    habit_id   UUID        NOT NULL REFERENCES habits(id) ON DELETE CASCADE,
    day        DATE        NOT NULL,
    note       TEXT,                                       -- encrypted
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_entry_habit_day UNIQUE (habit_id, day)
);
CREATE INDEX idx_entries_user_day ON entries(user_id, day);

CREATE TABLE day_meta (
    id       UUID PRIMARY KEY,
    user_id  UUID    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    day      DATE    NOT NULL,
    rest_day BOOLEAN NOT NULL DEFAULT FALSE,
    note     TEXT,                                         -- encrypted
    CONSTRAINT uq_daymeta_user_day UNIQUE (user_id, day)
);
