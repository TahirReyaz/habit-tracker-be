-- Per-week target overrides, e.g. a public holiday lowers this week's office target from 5 to 4.
-- No row = the habit's usual weekly_target. 0 = habit excused for that week (left out of the total).
CREATE TABLE week_targets (
    id         UUID PRIMARY KEY,
    user_id    UUID     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    habit_id   UUID     NOT NULL REFERENCES habits(id) ON DELETE CASCADE,
    week_start DATE     NOT NULL,   -- first day of the week, per the user's week-start setting when saved
    target     SMALLINT NOT NULL CHECK (target BETWEEN 0 AND 7),
    CONSTRAINT uq_week_target UNIQUE (habit_id, week_start)
);
CREATE INDEX idx_week_targets_user ON week_targets(user_id, week_start);
