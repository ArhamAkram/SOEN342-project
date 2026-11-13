package ats.domain;

/**
 * Appointment statuses (Iteration I, assumption A7.1).
 * A REQUESTED appointment holds no slot; only CONFIRMED (and later) statuses occupy one.
 */
public enum AppointmentStatus {
    REQUESTED,
    CONFIRMED,
    CHECKED_IN,
    IN_CONSULTATION,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    /** True for statuses that occupy a time slot (A2 rule 2). */
    public boolean occupiesSlot() {
        return this == CONFIRMED || this == CHECKED_IN || this == IN_CONSULTATION;
    }
}
