package ats.controller;

import ats.Clinic;
import ats.TestClinics;
import ats.domain.Appointment;
import ats.domain.AppointmentStatus;
import ats.domain.BookingChannel;
import ats.domain.HealthcareProfessional;
import ats.domain.Patient;
import ats.domain.TimeSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingControllerTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 23);

    private Clinic clinic;
    private BookingController booking;
    private HealthcareProfessional familyDoctor;
    private HealthcareProfessional cardiologist;
    private TimeSlot familySlot;
    private TimeSlot cardioSlot;
    private Patient emilie;
    private Patient olivier;

    @BeforeEach
    void setUp() {
        clinic = TestClinics.beforeSampleData();
        booking = clinic.booking();
        familyDoctor = clinic.availability().registerProfessional("Amélie", "Tremblay", "Dr.", "Family Medicine");
        cardiologist = clinic.availability().registerProfessional("Marc", "Bouchard", "Dr.", "Cardiology");
        familySlot = clinic.getAvailability().addSlot(familyDoctor, DAY, LocalTime.of(9, 0), LocalTime.of(9, 30));
        cardioSlot = clinic.getAvailability().addSlot(cardiologist, DAY, LocalTime.of(10, 0), LocalTime.of(11, 0));
        emilie = booking.registerPatient("Émilie", "Lavoie", "514-555-0101", "LAVE99010101");
        olivier = booking.registerPatient("Olivier", "Côté", "514-555-0102", "COTO85061502");
    }

    @Test
    void requestCreatesRequestedAppointmentWithoutBookingTheSlot() {
        Appointment a = booking.requestAppointment(emilie.getPatientId(), familySlot.getSlotId(), BookingChannel.IN_PERSON);

        assertEquals(AppointmentStatus.REQUESTED, a.getStatus());
        assertFalse(familySlot.isBooked(), "a requested appointment must not hold the slot");
    }

    @Test
    void confirmBooksTheSlot() {
        Appointment a = booking.requestAppointment(emilie.getPatientId(), familySlot.getSlotId(), BookingChannel.PHONE);
        booking.confirmAppointment(a.getAppointmentId());

        assertEquals(AppointmentStatus.CONFIRMED, a.getStatus());
        assertSame(a, familySlot.getBookedBy());
    }

    @Test
    void twoRequestsForTheSameSlotFirstConfirmationWins() {
        Appointment first = booking.requestAppointment(emilie.getPatientId(), familySlot.getSlotId(), BookingChannel.PHONE);
        Appointment second = booking.requestAppointment(olivier.getPatientId(), familySlot.getSlotId(), BookingChannel.PHONE);
        booking.confirmAppointment(first.getAppointmentId());

        BookingException e = assertThrows(BookingException.class, () -> booking.confirmAppointment(second.getAppointmentId()));
        assertEquals(FailureReason.PROFESSIONAL_UNAVAILABLE, e.getReason());
        assertEquals(AppointmentStatus.CANCELLED, second.getStatus(), "failed confirmation cancels the request (ext. 9a)");
        assertSame(first, familySlot.getBookedBy(), "the slot stays with the first appointment");
    }

    @Test
    void requestForBookedSlotIsRejectedAndNothingIsCreated() {
        Appointment a = booking.requestAppointment(emilie.getPatientId(), familySlot.getSlotId(), BookingChannel.PHONE);
        booking.confirmAppointment(a.getAppointmentId());
        int before = clinic.getAppointments().all().size();

        BookingException e = assertThrows(BookingException.class,
                () -> booking.requestAppointment(olivier.getPatientId(), familySlot.getSlotId(), BookingChannel.PHONE));
        assertEquals(FailureReason.PROFESSIONAL_UNAVAILABLE, e.getReason());
        assertEquals(before, clinic.getAppointments().all().size());
    }

    @Test
    void specialistWithoutReferralIsRejected() {
        BookingException e = assertThrows(BookingException.class,
                () -> booking.requestAppointment(olivier.getPatientId(), cardioSlot.getSlotId(), BookingChannel.PHONE));
        assertEquals(FailureReason.REFERRAL_REQUIRED, e.getReason());
        assertTrue(clinic.getAppointments().all().isEmpty());
    }

    @Test
    void specialistWithValidReferralIsAccepted() {
        booking.recordReferral(olivier.getPatientId(), "Cardiology", "Dr. Amélie Tremblay", "Palpitations");
        Appointment a = booking.requestAppointment(olivier.getPatientId(), cardioSlot.getSlotId(), BookingChannel.PHONE);

        assertNotNull(a.getReferral());
        booking.confirmAppointment(a.getAppointmentId());
        assertTrue(cardioSlot.isBooked());
    }

    @Test
    void referralForAnotherSpecialtyDoesNotCount() {
        booking.recordReferral(olivier.getPatientId(), "Dermatology", "Dr. Amélie Tremblay", "Rash");
        BookingException e = assertThrows(BookingException.class,
                () -> booking.requestAppointment(olivier.getPatientId(), cardioSlot.getSlotId(), BookingChannel.PHONE));
        assertEquals(FailureReason.REFERRAL_REQUIRED, e.getReason());
    }

    @Test
    void patientCannotBeDoubleBooked() {
        TimeSlot overlapping = clinic.getAvailability().addSlot(
                clinic.availability().registerProfessional("Sophie", "Gagnon", "Nurse Practitioner", "Primary Care"),
                DAY, LocalTime.of(9, 15), LocalTime.of(9, 45));
        Appointment a = booking.requestAppointment(emilie.getPatientId(), familySlot.getSlotId(), BookingChannel.PHONE);
        booking.confirmAppointment(a.getAppointmentId());

        BookingException e = assertThrows(BookingException.class,
                () -> booking.requestAppointment(emilie.getPatientId(), overlapping.getSlotId(), BookingChannel.PHONE));
        assertEquals(FailureReason.SCHEDULING_CONSTRAINT_VIOLATION, e.getReason());
    }

    @Test
    void pastSlotIsRejected() {
        TimeSlot past = clinic.getAvailability().addSlot(familyDoctor, LocalDate.of(2026, 10, 30), LocalTime.of(9, 0), LocalTime.of(9, 30));
        BookingException e = assertThrows(BookingException.class,
                () -> booking.requestAppointment(emilie.getPatientId(), past.getSlotId(), BookingChannel.PHONE));
        assertEquals(FailureReason.SCHEDULING_CONSTRAINT_VIOLATION, e.getReason());
    }

    @Test
    void cancellingConfirmedReleasesSlotButCancellingRequestedHasNothingToRelease() {
        Appointment confirmed = booking.requestAppointment(emilie.getPatientId(), familySlot.getSlotId(), BookingChannel.PHONE);
        booking.confirmAppointment(confirmed.getAppointmentId());
        assertTrue(booking.cancelAppointment(confirmed.getAppointmentId()));
        assertFalse(familySlot.isBooked());

        Appointment requested = booking.requestAppointment(olivier.getPatientId(), familySlot.getSlotId(), BookingChannel.PHONE);
        assertFalse(booking.cancelAppointment(requested.getAppointmentId()));
        assertEquals(AppointmentStatus.CANCELLED, requested.getStatus());
    }

    @Test
    void availabilityHidesBookedAndPastSlots() {
        Appointment a = booking.requestAppointment(emilie.getPatientId(), familySlot.getSlotId(), BookingChannel.PHONE);
        booking.confirmAppointment(a.getAppointmentId());

        var free = booking.getProfessionalAvailability(familyDoctor, null, DAY, DAY);
        assertTrue(free.isEmpty());
    }

    @Test
    void failuresAreLogged() {
        assertThrows(BookingException.class,
                () -> booking.requestAppointment(olivier.getPatientId(), cardioSlot.getSlotId(), BookingChannel.PHONE));
        var last = clinic.getActivityLog().entries().get(clinic.getActivityLog().entries().size() - 1);
        assertEquals("requestAppointment", last.operation());
        assertEquals("Failure", last.outcome());
        assertTrue(last.detail().startsWith("ReferralRequired"));
    }
}
