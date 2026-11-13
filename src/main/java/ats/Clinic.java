package ats;

import ats.controller.AvailabilityController;
import ats.controller.BookingController;
import ats.controller.CsvController;
import ats.registry.ActivityLog;
import ats.registry.AppointmentBook;
import ats.registry.AvailabilityCatalog;
import ats.registry.PatientRegistry;
import ats.registry.ProfessionalRegistry;
import ats.registry.ReferralRegistry;

import java.time.Clock;

/**
 * The root object of the system. It owns the in-memory registries and creates the
 * use-case controllers that the user interface talks to (GRASP Creator / Controller).
 * Nothing is persisted: every run starts empty (Iteration II PoC scope).
 */
public class Clinic {
    private final Clock clock;
    private final PatientRegistry patients = new PatientRegistry();
    private final ProfessionalRegistry professionals = new ProfessionalRegistry();
    private final AvailabilityCatalog availability = new AvailabilityCatalog();
    private final AppointmentBook appointments = new AppointmentBook();
    private final ReferralRegistry referrals = new ReferralRegistry();
    private final ActivityLog activityLog;

    private final BookingController bookingController;
    private final AvailabilityController availabilityController;
    private final CsvController csvController;

    public Clinic(Clock clock) {
        this.clock = clock;
        this.activityLog = new ActivityLog(clock);
        this.bookingController = new BookingController(patients, availability, appointments, referrals, activityLog, clock);
        this.availabilityController = new AvailabilityController(professionals, availability, activityLog);
        this.csvController = new CsvController(patients, professionals, availability, appointments, activityLog);
    }

    public Clinic() {
        this(Clock.systemDefaultZone());
    }

    public Clock getClock() { return clock; }
    public PatientRegistry getPatients() { return patients; }
    public ProfessionalRegistry getProfessionals() { return professionals; }
    public AvailabilityCatalog getAvailability() { return availability; }
    public AppointmentBook getAppointments() { return appointments; }
    public ReferralRegistry getReferrals() { return referrals; }
    public ActivityLog getActivityLog() { return activityLog; }

    public BookingController booking() { return bookingController; }
    public AvailabilityController availability() { return availabilityController; }
    public CsvController csv() { return csvController; }
}
