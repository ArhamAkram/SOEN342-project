package ats.domain.recurrence;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** Repeats on the n-th given weekday of every month, e.g. the first Monday. */
public class MonthlyByWeekdayPattern implements RecurrencePattern {
    private final int occurrence; // 1 to 5
    private final DayOfWeek dayOfWeek;

    public MonthlyByWeekdayPattern(int occurrence, DayOfWeek dayOfWeek) {
        if (occurrence < 1 || occurrence > 5) {
            throw new IllegalArgumentException("Occurrence must be between 1 and 5");
        }
        this.occurrence = occurrence;
        this.dayOfWeek = dayOfWeek;
    }

    @Override
    public boolean occursOn(LocalDate date) {
        return date.getDayOfWeek() == dayOfWeek && (date.getDayOfMonth() - 1) / 7 + 1 == occurrence;
    }

    @Override
    public String describe() {
        return "monthly on occurrence " + occurrence + " of " + dayOfWeek;
    }
}
