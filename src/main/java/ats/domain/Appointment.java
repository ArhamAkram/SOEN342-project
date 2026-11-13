package ats.domain;

import java.util.Objects;

/**
 * A booked meeting between one patient and one professional (FR-09 to FR-16).
 * It is created REQUESTED (holding no slot) and books its slot only when confirmed.
 */
public class Appointment {
    private final String appointmentId;
    private final Patient patient;
    private final TimeSlot requestedSlot;
    private final BookingChannel channel;
    private final Referral referral; // null when no referral was needed
    private AppointmentStatus status;

    public Appointment(String appointmentId, Patient patient, TimeSlot requestedSlot, BookingChannel channel, Referral referral) {
        this.appointmentId = Objects.requireNonNull(appointmentId);
        this.patient = Objects.requireNonNull(patient);
        this.requestedSlot = Objects.requireNonNull(requestedSlot);
        this.channel = Objects.requireNonNull(channel);
        this.referral = referral;
        this.status = AppointmentStatus.REQUESTED;
    }

    public String getAppointmentId() { return appointmentId; }
    public Patient getPatient() { return patient; }
    public TimeSlot getRequestedSlot() { return requestedSlot; }
    public HealthcareProfessional getProfessional() { return requestedSlot.getProfessional(); }
    public BookingChannel getChannel() { return channel; }
    public Referral getReferral() { return referral; }
    public AppointmentStatus getStatus() { return status; }

    /** REQUESTED -> CONFIRMED, booking the requested slot (contract CO-02-4, success). */
    public void confirm() {
        if (status != AppointmentStatus.REQUESTED) {
            throw new IllegalStateException("Only a REQUESTED appointment can be confirmed (was " + status + ")");
        }
        requestedSlot.book(this);
        status = AppointmentStatus.CONFIRMED;
    }

    /**
     * Cancels the appointment. A confirmed appointment releases its slot;
     * a requested one never held a slot, so there is nothing to release.
     *
     * @return true if a slot was released (so waitlisted patients could be offered it)
     */
    public boolean cancel() {
        if (status == AppointmentStatus.CANCELLED || status == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("Appointment " + appointmentId + " cannot be cancelled (it is " + status + ")");
        }
        boolean released = false;
        if (status.occupiesSlot() && requestedSlot.getBookedBy() == this) {
            requestedSlot.release();
            released = true;
        }
        status = AppointmentStatus.CANCELLED;
        return released;
    }

    @Override
    public String toString() {
        return appointmentId + " " + status + " " + patient.getFullName() + " with " + getProfessional().getFullName()
                + " on " + requestedSlot.getDate() + " " + requestedSlot.getStart() + "-" + requestedSlot.getEnd()
                + " (" + channel + ")";
    }
}
