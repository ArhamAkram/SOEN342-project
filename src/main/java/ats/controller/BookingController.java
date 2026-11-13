package ats.controller;

import ats.domain.Appointment;
import ats.domain.AppointmentStatus;
import ats.domain.BookingChannel;
import ats.domain.HealthcareProfessional;
import ats.domain.Patient;
import ats.domain.Referral;
import ats.domain.TimeSlot;
import ats.registry.ActivityLog;
import ats.registry.AppointmentBook;
import ats.registry.AvailabilityCatalog;
import ats.registry.PatientRegistry;
import ats.registry.ReferralRegistry;
import ats.registry.SlotQuery;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Use-case controller for UC-02 Book Appointment (and UC-04 Cancel Appointment).
 * Each public method is one system operation from the Iteration I SSDs.
 */
public class BookingController {
    private final PatientRegistry patients;
    private final AvailabilityCatalog availability;
    private final AppointmentBook appointments;
    private final ReferralRegistry referrals;
    private final ActivityLog log;
    private final Clock clock;

    public BookingController(PatientRegistry patients, AvailabilityCatalog availability, AppointmentBook appointments,
                             ReferralRegistry referrals, ActivityLog log, Clock clock) {
        this.patients = patients;
        this.availability = availability;
        this.appointments = appointments;
        this.referrals = referrals;
        this.log = log;
        this.clock = clock;
    }

    /** searchPatient(searchCriteria) - contract CO-02-1. Read-only. */
    public List<Patient> searchPatient(String criteria) {
        List<Patient> found = patients.search(criteria);
        log.success("searchPatient", found.size() + " match(es)");
        return found;
    }

    /** registerPatient(...) - UC-01, used when the caller is a new patient. */
    public Patient registerPatient(String firstName, String lastName, String telephone, String ramqNumber) {
        Patient patient = patients.register(firstName, lastName, telephone, ramqNumber);
        log.success("registerPatient", patient.getPatientId());
        return patient;
    }

    /**
     * getProfessionalAvailability(...) - contract CO-02-2. Read-only.
     * Returns free slots in the range that start in the future.
     */
    public List<TimeSlot> getProfessionalAvailability(HealthcareProfessional professional, String specialty,
                                                      LocalDate from, LocalDate to) {
        LocalDateTime now = LocalDateTime.now(clock);
        return availability.search(SlotQuery.freeSlots(professional, specialty, from, to)).stream()
                .filter(s -> s.getStartDateTime().isAfter(now))
                .toList();
    }

    /**
     * requestAppointment(patientId, slotId, channel) - contract CO-02-3.
     * Success: a REQUESTED appointment is created; the slot is NOT booked.
     * Failure: nothing is created and the failure is logged.
     */
    public Appointment requestAppointment(String patientId, String slotId, BookingChannel channel) {
        try {
            Patient patient = patients.findById(patientId)
                    .orElseThrow(() -> new BookingException(FailureReason.PATIENT_NOT_FOUND, "No patient " + patientId));
            TimeSlot slot = availability.findById(slotId)
                    .orElseThrow(() -> new BookingException(FailureReason.SLOT_NOT_FOUND, "No time slot " + slotId));
            Referral referral = verifyBookable(patient, slot);
            Appointment appointment = appointments.create(patient, slot, channel, referral);
            log.success("requestAppointment", appointment.getAppointmentId() + " for " + patient.getPatientId()
                    + " in " + slot.getSlotId());
            return appointment;
        } catch (BookingException e) {
            log.failure("requestAppointment", e.getReason().contractName() + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * confirmAppointment(appointmentId) - contract CO-02-4.
     * Re-checks everything; success books the slot and sets CONFIRMED.
     * Failure (extension 9a): the appointment becomes CANCELLED and no slot changes.
     */
    public Appointment confirmAppointment(String appointmentId) {
        Appointment appointment = appointments.findById(appointmentId).orElseThrow(() -> {
            log.failure("confirmAppointment", "AppointmentNotFound: " + appointmentId);
            return new BookingException(FailureReason.APPOINTMENT_NOT_FOUND, "No appointment " + appointmentId);
        });
        if (appointment.getStatus() != AppointmentStatus.REQUESTED) {
            log.failure("confirmAppointment", "InvalidStatus: " + appointmentId + " is " + appointment.getStatus());
            throw new BookingException(FailureReason.INVALID_STATUS,
                    "Only a requested appointment can be confirmed; " + appointmentId + " is " + appointment.getStatus());
        }
        try {
            verifyBookable(appointment.getPatient(), appointment.getRequestedSlot());
        } catch (BookingException e) {
            appointment.cancel(); // REQUESTED -> CANCELLED; it held no slot, so none is released
            log.failure("confirmAppointment", e.getReason().contractName() + ": " + e.getMessage());
            throw e;
        }
        appointment.confirm();
        log.success("confirmAppointment", appointmentId + " booked " + appointment.getRequestedSlot().getSlotId());
        return appointment;
    }

    /**
     * cancelAppointment(appointmentId) - UC-04.
     * @return true if a booked slot was released (Iteration II: waitlisted patients would be offered it)
     */
    public boolean cancelAppointment(String appointmentId) {
        Appointment appointment = appointments.findById(appointmentId)
                .orElseThrow(() -> new BookingException(FailureReason.APPOINTMENT_NOT_FOUND, "No appointment " + appointmentId));
        try {
            boolean released = appointment.cancel();
            log.success("cancelAppointment", appointmentId + (released ? " released " + appointment.getRequestedSlot().getSlotId() : ""));
            return released;
        } catch (IllegalStateException e) {
            log.failure("cancelAppointment", "InvalidStatus: " + e.getMessage());
            throw new BookingException(FailureReason.INVALID_STATUS, e.getMessage());
        }
    }

    /** Records a submitted referral so a specialist booking can be made (simplified UC-08). */
    public Referral recordReferral(String patientId, String specialty, String referringProfessionalName, String clinicalReason) {
        Patient patient = patients.findById(patientId)
                .orElseThrow(() -> new BookingException(FailureReason.PATIENT_NOT_FOUND, "No patient " + patientId));
        Referral referral = referrals.record(patient, specialty, referringProfessionalName, clinicalReason, LocalDate.now(clock));
        log.success("recordReferral", referral.getReferralId() + " for " + patientId + " to " + specialty);
        return referral;
    }

    /**
     * The checks shared by request and confirm: availability (FR-10), scheduling
     * constraints (FR-11, A2) and referral requirement (FR-12, A3/A4).
     *
     * @return the referral that makes the booking valid, or null if none is needed
     */
    private Referral verifyBookable(Patient patient, TimeSlot slot) {
        if (slot.isBooked()) {
            throw new BookingException(FailureReason.PROFESSIONAL_UNAVAILABLE,
                    "Slot " + slot.getSlotId() + " is already booked");
        }
        if (!slot.getStartDateTime().isAfter(LocalDateTime.now(clock))) {
            throw new BookingException(FailureReason.SCHEDULING_CONSTRAINT_VIOLATION,
                    "Slot " + slot.getSlotId() + " is in the past");
        }
        if (appointments.patientHasOverlap(patient, slot)) {
            throw new BookingException(FailureReason.SCHEDULING_CONSTRAINT_VIOLATION,
                    patient.getFullName() + " already has an appointment at that time");
        }
        HealthcareProfessional professional = slot.getProfessional();
        if (!professional.isSpecialist()) {
            return null;
        }
        return referrals.findValid(patient, professional.getSpecialty(), slot.getDate())
                .orElseThrow(() -> new BookingException(FailureReason.REFERRAL_REQUIRED,
                        professional.getSpecialty() + " requires a valid referral for " + patient.getFullName()));
    }
}
