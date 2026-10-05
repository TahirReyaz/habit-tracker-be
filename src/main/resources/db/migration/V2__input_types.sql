-- How a habit is logged for a day:
--   CHECK  — tick box (optional note)
--   SELECT — pick one of the habit's own options; new options can be created while logging
--   TEXT   — free text; a non-empty text is the point
-- In every case the existence of an entry row is what earns the day's point.
ALTER TABLE habits  ADD COLUMN input_type VARCHAR(10) NOT NULL DEFAULT 'CHECK';
ALTER TABLE habits  ADD COLUMN options    TEXT;   -- encrypted, newline-separated option labels
ALTER TABLE entries ADD COLUMN value      TEXT;   -- encrypted: chosen option (SELECT) or the text (TEXT)
