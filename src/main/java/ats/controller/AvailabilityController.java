package ats.controller;

import ats.domain.HealthcareProfessional;
import ats.domain.TimeSlot;
import ats.domain.recurrence.RecurrencePattern;
import ats.registry.ActivityLog;
import ats.registry.AvailabilityCatalog;
import ats.registry.ProfessionalRegistry;
import ats.registry.SlotQuery;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Use-case controller for UC-15 Make Availability and UC-16 Search Availability.
 */
public class AvailabilityController {
    private final ProfessionalRegistry professionals;
    private final AvailabilityCatalog availability;
    private final ActivityLog log;

    public AvailabilityController(ProfessionalRegistry professionals, AvailabilityCatalog availability, ActivityLog log) {
        this.professionals = professionals;
        this.availability = availability;
        this.log = log;
    }

    /** Adds a professional so availability can be created for them (the system starts empty). */
    public HealthcareProfessional registerProfessional(String firstName, String lastName, String title, String specialty) {
        HealthcareProfessional p = professionals.findOrCreate(firstName, lastName, title, specialty);
        log.success("registerProfessional", p.getProfessionalId());
        return p;
    }

    /**
     * makeAvailability(professionalId, from, to, pattern, start, end, slotMinutes).
     * For each date in [from, to] on which the pattern occurs, the block start-end is cut
     * into slots of slotMinutes (or kept as one slot when slotMinutes is 0).
     * Slots that would overlap existing ones are skipped and reported, not created.
     */
    public AvailabilityResult makeAvailability(String professionalId, LocalDate from, LocalDate to,
                                               RecurrencePattern pattern, LocalTime start, LocalTime end, int slotMinutes) {
        HealthcareProfessional professional = professionals.findById(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("No professional " + professionalId));
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("The end date must not be before the start date");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("The end time must be after the start time");
        }
        long blockMinutes = Duration.between(start, end).toMinutes();
        if (slotMinutes < 0 || (slotMinutes > 0 && blockMinutes % slotMinutes != 0)) {
            throw new IllegalArgumentException("Slot length must divide the block evenly (" + blockMinutes + " minutes)");
        }
        int step = slotMinutes == 0 ? (int) blockMinutes : slotMinutes;

        List<TimeSlot> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (!pattern.occursOn(date)) {
                continue;
            }
            for (LocalTime t = start; t.isBefore(end); t = t.plusMinutes(step)) {
                try {
                    created.add(availability.addSlot(professional, date, t, t.plusMinutes(step)));
                } catch (IllegalArgumentException e) {
                    skipped.add(date + " " + t + ": " + e.getMessage());
                }
            }
        }
        log.success("makeAvailability", professional.getProfessionalId() + " " + pattern.describe()
                + " " + from + ".." + to + ": " + created.size() + " created, " + skipped.size() + " skipped");
        return new AvailabilityResult(created, skipped);
    }

    /** searchAvailability(...) - used directly by staff, or indirectly while booking. */
    public List<TimeSlot> searchAvailability(String professionalId, String specialty, LocalDate from, LocalDate to, boolean onlyFree) {
        HealthcareProfessional professional = (professionalId == null || professionalId.isBlank()) ? null
                : professionals.findById(professionalId)
                        .orElseThrow(() -> new IllegalArgumentException("No professional " + professionalId));
        return availability.search(new SlotQuery(professional, specialty, from, to, onlyFree));
    }
}
