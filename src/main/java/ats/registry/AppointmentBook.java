package ats.registry;

import ats.domain.Appointment;
import ats.domain.BookingChannel;
import ats.domain.Patient;
import ats.domain.Referral;
import ats.domain.TimeSlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** In-memory store of appointments; creator of Appointment and expert on patient double-booking. */
public class AppointmentBook {
    private final List<Appointment> appointments = new ArrayList<>();
    private int nextId = 1;

    /** Creates a REQUESTED appointment. It holds no slot until it is confirmed. */
    public Appointment create(Patient patient, TimeSlot slot, BookingChannel channel, Referral referral) {
        Appointment appointment = new Appointment(String.format("A%05d", nextId++), patient, slot, channel, referral);
        appointments.add(appointment);
        return appointment;
    }

    public Optional<Appointment> findById(String appointmentId) {
        return appointments.stream().filter(a -> a.getAppointmentId().equalsIgnoreCase(appointmentId.trim())).findFirst();
    }

    /** A2 rule 3: does the patient already have a slot-occupying appointment overlapping this slot? */
    public boolean patientHasOverlap(Patient patient, TimeSlot slot) {
        return appointments.stream().anyMatch(a -> a.getPatient() == patient
                && a.getStatus().occupiesSlot()
                && a.getRequestedSlot() != slot
                && a.getRequestedSlot().overlaps(slot));
    }

    public List<Appointment> forPatient(Patient patient) {
        return appointments.stream().filter(a -> a.getPatient() == patient).toList();
    }

    public List<Appointment> all() {
        return Collections.unmodifiableList(appointments);
    }
}
