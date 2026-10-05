-- Dropdown habits can allow several options per day. Multiple picks are stored
-- newline-separated in entries.value (option labels never contain newlines).
ALTER TABLE habits ADD COLUMN multi_select BOOLEAN NOT NULL DEFAULT FALSE;

-- Days logged before a habit's start date were stored but never scored (e.g. back-filling last week
-- for a habit created today). Logging now moves start_date back automatically; this repairs existing data.
UPDATE habits h
SET start_date = m.first_day
FROM (SELECT habit_id, MIN(day) AS first_day FROM entries GROUP BY habit_id) m
WHERE m.habit_id = h.id AND m.first_day < h.start_date;
