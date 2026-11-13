package ats.domain.recurrence;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Repeats on the given days of every week, e.g. every Monday and Wednesday. */
public class WeeklyPattern implements RecurrencePattern {
    private final Set<DayOfWeek> days;

    public WeeklyPattern(Set<DayOfWeek> days) {
        if (days == null || days.isEmpty()) {
            throw new IllegalArgumentException("A weekly pattern needs at least one day of the week");
        }
        this.days = EnumSet.copyOf(days);
    }

    @Override
    public boolean occursOn(LocalDate date) {
        return days.contains(date.getDayOfWeek());
    }

    @Override
    public String describe() {
        return "every " + days.stream().map(Enum::name).collect(Collectors.joining(", "));
    }
}
