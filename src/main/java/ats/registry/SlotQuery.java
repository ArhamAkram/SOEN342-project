package ats.registry;

import ats.domain.HealthcareProfessional;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Criteria for searching availability. Professional and specialty are optional;
 * the date range is inclusive.
 */
public record SlotQuery(HealthcareProfessional professional, String specialty, LocalDate from, LocalDate to, boolean onlyFree) {
    public SlotQuery {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("The end date must not be before the start date");
        }
    }

    public static SlotQuery freeSlots(HealthcareProfessional professional, String specialty, LocalDate from, LocalDate to) {
        return new SlotQuery(professional, specialty, from, to, true);
    }
}
