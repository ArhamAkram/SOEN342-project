package ats.domain.recurrence;

import java.time.LocalDate;

/** Decides on which dates a recurring availability applies (PoC "Making Availability"). */
public interface RecurrencePattern {
    boolean occursOn(LocalDate date);

    /** Human-readable form, e.g. "every MONDAY". */
    String describe();
}
