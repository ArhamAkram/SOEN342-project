package ats.domain.recurrence;

import java.time.LocalDate;

/** Repeats on the same day number of every month, e.g. the 15th. */
public class MonthlyByDatePattern implements RecurrencePattern {
    private final int dayOfMonth;

    public MonthlyByDatePattern(int dayOfMonth) {
        if (dayOfMonth < 1 || dayOfMonth > 31) {
            throw new IllegalArgumentException("Day of month must be between 1 and 31");
        }
        this.dayOfMonth = dayOfMonth;
    }

    @Override
    public boolean occursOn(LocalDate date) {
        return date.getDayOfMonth() == dayOfMonth;
    }

    @Override
    public String describe() {
        return "monthly on day " + dayOfMonth;
    }
}
