package ats.registry;

import ats.domain.HealthcareProfessional;
import ats.domain.TimeSlot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * In-memory "availability database": every TimeSlot of every professional.
 * Information expert for availability (FR-07, FR-10) and creator of TimeSlots.
 */
public class AvailabilityCatalog {
    private static final Comparator<TimeSlot> CHRONOLOGICAL = Comparator
            .comparing(TimeSlot::getDate)
            .thenComparing(TimeSlot::getStart)
            .thenComparing(s -> s.getProfessional().getFullName());

    private final List<TimeSlot> slots = new ArrayList<>();
    private int nextId = 1;

    /**
     * Creates a slot. Refuses one that overlaps another slot of the same professional,
     * so a professional can never be offered twice for the same time.
     */
    public TimeSlot addSlot(HealthcareProfessional professional, LocalDate date, LocalTime start, LocalTime end) {
        Optional<TimeSlot> clash = slots.stream()
                .filter(s -> s.getProfessional() == professional && s.overlaps(date, start, end))
                .findFirst();
        if (clash.isPresent()) {
            throw new IllegalArgumentException("Overlaps existing slot " + clash.get().getSlotId()
                    + " (" + date + " " + clash.get().getStart() + "-" + clash.get().getEnd() + ")");
        }
        TimeSlot slot = new TimeSlot(String.format("S%05d", nextId++), professional, date, start, end);
        slots.add(slot);
        return slot;
    }

    public Optional<TimeSlot> findById(String slotId) {
        return slots.stream().filter(s -> s.getSlotId().equalsIgnoreCase(slotId.trim())).findFirst();
    }

    public Optional<TimeSlot> findExact(HealthcareProfessional professional, LocalDate date, LocalTime start, LocalTime end) {
        return slots.stream()
                .filter(s -> s.getProfessional() == professional && s.getDate().equals(date)
                        && s.getStart().equals(start) && s.getEnd().equals(end))
                .findFirst();
    }

    /** Slots matching the query, in date/time order. */
    public List<TimeSlot> search(SlotQuery query) {
        return slots.stream()
                .filter(s -> query.professional() == null || s.getProfessional() == query.professional())
                .filter(s -> query.specialty() == null || query.specialty().isBlank()
                        || s.getProfessional().getSpecialty().equalsIgnoreCase(query.specialty().trim()))
                .filter(s -> !s.getDate().isBefore(query.from()) && !s.getDate().isAfter(query.to()))
                .filter(s -> !query.onlyFree() || !s.isBooked())
                .sorted(CHRONOLOGICAL)
                .toList();
    }

    /** Every slot, in date/time order (used by the CSV export). */
    public List<TimeSlot> all() {
        return slots.stream().sorted(CHRONOLOGICAL).toList();
    }

    public int size() {
        return slots.size();
    }
}
