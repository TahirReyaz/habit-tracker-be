-- LIMIT habits: log an amount per day; a day outside [limit_min, limit_max] is a -1 mark, otherwise 0.
-- e.g. "Screen time" max 120 (min), "Sleep" min 7 (h), "Coffee" max 2 (cups).
ALTER TABLE habits  ADD COLUMN limit_min DOUBLE PRECISION;   -- null = no lower bound
ALTER TABLE habits  ADD COLUMN limit_max DOUBLE PRECISION;   -- null = no upper bound
ALTER TABLE habits  ADD COLUMN unit      VARCHAR(16);        -- display only: "min", "h", "cups", "₹"
-- Whether a logged amount broke the habit's limit. Kept in clear (the amount itself stays encrypted)
-- so scores and statistics never have to decrypt values; recomputed whenever the limits change.
ALTER TABLE entries ADD COLUMN penalty   BOOLEAN NOT NULL DEFAULT FALSE;
