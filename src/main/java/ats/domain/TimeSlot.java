package ats.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * One bookable interval of a professional's availability (FR-07, FR-10).
 * It is booked only when an appointment is confirmed (Iteration I feedback fix).
 */
public class TimeSlot {
    private final String slotId;
    private final HealthcareProfessional professional;
    private final LocalDate date;
    private final LocalTime start;
    private final LocalTime end;
    private Appointment bookedBy; // the appointment that occupies this slot, or null

    public TimeSlot(String slotId, HealthcareProfessional professional, LocalDate date, LocalTime start, LocalTime end) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Slot end time must be after its start time");
        }
        this.slotId = Objects.requireNonNull(slotId);
        this.professional = Objects.requireNonNull(professional);
        this.date = Objects.requireNonNull(date);
        this.start = start;
        this.end = end;
    }

    public String getSlotId() { return slotId; }
    public HealthcareProfessional getProfessional() { return professional; }
    public LocalDate getDate() { return date; }
    public LocalTime getStart() { return start; }
    public LocalTime getEnd() { return end; }
    public LocalDateTime getStartDateTime() { return date.atTime(start); }
    public LocalDateTime getEndDateTime() { return date.atTime(end); }

    public boolean isBooked() { return bookedBy != null; }
    public Appointment getBookedBy() { return bookedBy; }

    /** Called only by Appointment.confirm(): the single place a slot becomes booked. */
    void book(Appointment appointment) {
        if (isBooked()) {
            throw new IllegalStateException("Slot " + slotId + " is already booked");
        }
        this.bookedBy = appointment;
    }

    /** Called only by Appointment.cancel() on a confirmed appointment. */
    void release() {
        this.bookedBy = null;
    }

    /** True if this slot and the given interval on the same date overlap. */
    public boolean overlaps(LocalDate otherDate, LocalTime otherStart, LocalTime otherEnd) {
        return date.equals(otherDate) && start.isBefore(otherEnd) && otherStart.isBefore(end);
    }

    public boolean overlaps(TimeSlot other) {
        return overlaps(other.date, other.start, other.end);
    }

    @Override
    public String toString() {
        return slotId + " " + date + " " + start + "-" + end + " " + professional + (isBooked() ? " [booked]" : "");
    }
}
