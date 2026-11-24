package com.tally.domain;

/** How a day is logged for a habit. For CHECK/SELECT/TEXT any saved entry counts as the day's point. */
public enum InputType {
    /** a tick box; an optional note can be attached */
    CHECK,
    /** one option from a list the user builds up over time; an optional note can be attached */
    SELECT,
    /** free text; a non-empty text is the entry */
    TEXT,
    /** an amount with an allowed min and/or max; a day outside the range is -1, otherwise 0 */
    LIMIT
}
