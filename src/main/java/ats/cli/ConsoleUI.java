package ats.cli;

import ats.Clinic;
import ats.controller.AvailabilityResult;
import ats.controller.BookingException;
import ats.controller.FailureReason;
import ats.csv.ImportResult;
import ats.domain.Appointment;
import ats.domain.BookingChannel;
import ats.domain.HealthcareProfessional;
import ats.domain.Patient;
import ats.domain.TimeSlot;
import ats.domain.recurrence.MonthlyByDatePattern;
import ats.domain.recurrence.MonthlyByWeekdayPattern;
import ats.domain.recurrence.RecurrencePattern;
import ats.domain.recurrence.WeeklyPattern;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;

/** Command-line interface (CON-01). It only reads input, calls controllers and prints results. */
public class ConsoleUI {
    private static final int MAX_LISTED = 25;

    private final Clinic clinic;
    private final Scanner in;
    private final PrintStream out;

    public ConsoleUI(Clinic clinic, InputStream input, PrintStream output) {
        this.clinic = clinic;
        this.in = new Scanner(input, java.nio.charset.StandardCharsets.UTF_8);
        this.out = output;
    }

    public void run() {
        out.println("Appointment Tracking System - Iteration II proof of concept");
        out.println("Today is " + today() + ". The system starts empty: import a CSV file to load data.");
        try {
            while (true) {
                printMenu();
                String choice = ask("Choose an option");
                try {
                    switch (choice) {
                        case "1" -> importAvailability();
                        case "2" -> importAppointments();
                        case "3" -> exportAvailability();
                        case "4" -> makeAvailability();
                        case "5" -> searchAvailability();
                        case "6" -> bookAppointment();
                        case "7" -> cancelAppointment();
                        case "8" -> listAppointments();
                        case "9" -> registerPatient();
                        case "10" -> recordReferral();
                        case "11" -> listProfessionals();
                        case "12" -> showActivityLog();
                        case "0", "q", "quit", "exit" -> {
                            out.println("Goodbye.");
                            return;
                        }
                        default -> out.println("Unknown option: " + choice);
                    }
                } catch (BookingException e) {
                    out.println("Not done: " + e.getMessage() + " [" + e.getReason().contractName() + "]");
                } catch (IllegalArgumentException | IllegalStateException e) {
                    out.println("Not done: " + e.getMessage());
                }
            }
        } catch (NoSuchElementException endOfInput) {
            out.println();
            out.println("End of input. Goodbye.");
        }
    }

    private void printMenu() {
        out.println();
        out.println("=== Main menu ===");
        out.println(" 1  Import availability from CSV");
        out.println(" 2  Import booked appointments from CSV");
        out.println(" 3  Export availability to CSV");
        out.println(" 4  Make availability (recurring)");
        out.println(" 5  Search availability");
        out.println(" 6  Book an appointment");
        out.println(" 7  Cancel an appointment");
        out.println(" 8  List appointments");
        out.println(" 9  Register a patient");
        out.println("10  Record a referral");
        out.println("11  List professionals");
        out.println("12  Show activity log");
        out.println(" 0  Exit");
    }

    // ---------------------------------------------------------------- CSV

    private void importAvailability() {
        Path file = Path.of(ask("Availability CSV file (e.g. data/availability.csv)"));
        try {
            printImport(clinic.csv().importAvailability(file));
        } catch (IOException e) {
            out.println("Could not read " + file + ": " + e.getMessage());
        }
    }

    private void importAppointments() {
        Path file = Path.of(ask("Booked appointments CSV file (e.g. data/appointments.csv)"));
        try {
            printImport(clinic.csv().importAppointments(file));
        } catch (IOException e) {
            out.println("Could not read " + file + ": " + e.getMessage());
        }
    }

    private void printImport(ImportResult r) {
        out.println("Imported " + r.getImportedCount() + " row(s).");
        if (!r.getSkipped().isEmpty()) {
            out.println("Skipped " + r.getSkipped().size() + " row(s):");
            r.getSkipped().forEach(s -> out.println("  " + s));
        }
    }

    private void exportAvailability() {
        Path file = Path.of(askOr("Export to file", "availability-export.csv"));
        try {
            int n = clinic.csv().exportAvailability(file);
            out.println("Exported " + n + " slot(s) to " + file.toAbsolutePath());
        } catch (IOException e) {
            out.println("Could not write " + file + ": " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- availability

    private void makeAvailability() {
        HealthcareProfessional professional = chooseProfessional(true);
        if (professional == null) {
            return;
        }
        LocalDate from = askDate("From date (YYYY-MM-DD)", today());
        LocalDate to = askDate("To date (YYYY-MM-DD)", from.plusMonths(1).minusDays(1));
        RecurrencePattern pattern = askPattern();
        LocalTime start = askTime("Start time (HH:MM)");
        LocalTime end = askTime("End time (HH:MM)");
        int minutes = askInt("Slot length in minutes (0 = one slot for the whole block)", 30);

        AvailabilityResult r = clinic.availability().makeAvailability(
                professional.getProfessionalId(), from, to, pattern, start, end, minutes);
        out.println("Created " + r.created().size() + " slot(s) for " + professional + ", " + pattern.describe()
                + ", " + start + "-" + end + ", " + from + " to " + to + ".");
        if (!r.skipped().isEmpty()) {
            out.println("Skipped " + r.skipped().size() + " slot(s) that overlap existing availability:");
            r.skipped().stream().limit(MAX_LISTED).forEach(s -> out.println("  " + s));
        }
    }

    private RecurrencePattern askPattern() {
        out.println("Repeat: 1) weekly on chosen days  2) monthly on a date  3) monthly on a weekday (e.g. first Monday)");
        String kind = askOr("Repeat pattern", "1");
        switch (kind) {
            case "2":
                return new MonthlyByDatePattern(askInt("Day of month (1-31)", 1));
            case "3":
                int n = askInt("Which occurrence (1-5)", 1);
                return new MonthlyByWeekdayPattern(n, parseDay(ask("Day of week (MON..SUN)")));
            default:
                String days = ask("Days of week, comma separated (e.g. MON,WED)");
                Set<DayOfWeek> set = EnumSet.noneOf(DayOfWeek.class);
                for (String d : days.split(",")) {
                    set.add(parseDay(d));
                }
                return new WeeklyPattern(set);
        }
    }

    private void searchAvailability() {
        String who = askOr("Professional name or specialty (blank = everyone)", "");
        LocalDate from = askDate("From date (YYYY-MM-DD)", today());
        LocalDate to = askDate("To date (YYYY-MM-DD)", from.plusDays(30));
        boolean onlyFree = !askOr("Show booked slots too? (y/n)", "n").toLowerCase(Locale.ROOT).startsWith("y");
        List<TimeSlot> found = slotsFor(who, from, to, onlyFree);
        printSlots(found);
    }

    private List<TimeSlot> slotsFor(String who, LocalDate from, LocalDate to, boolean onlyFree) {
        if (who.isBlank()) {
            return clinic.availability().searchAvailability(null, null, from, to, onlyFree);
        }
        List<HealthcareProfessional> matches = clinic.getProfessionals().search(who);
        return matches.stream()
                .flatMap(p -> clinic.availability().searchAvailability(p.getProfessionalId(), null, from, to, onlyFree).stream())
                .sorted(java.util.Comparator.comparing(TimeSlot::getStartDateTime))
                .toList();
    }

    private void printSlots(List<TimeSlot> slots) {
        if (slots.isEmpty()) {
            out.println("No matching availability.");
            return;
        }
        out.println(slots.size() + " slot(s):");
        for (int i = 0; i < Math.min(slots.size(), MAX_LISTED); i++) {
            out.printf("  %2d) %s%n", i + 1, slots.get(i));
        }
        if (slots.size() > MAX_LISTED) {
            out.println("  ... and " + (slots.size() - MAX_LISTED) + " more (narrow the search).");
        }
    }

    // ---------------------------------------------------------------- booking (UC-02)

    private void bookAppointment() {
        String where = askOr("Is the patient 1) here at the clinic or 2) on the phone?", "1");
        BookingChannel channel = where.equals("2") ? BookingChannel.PHONE : BookingChannel.IN_PERSON;

        Patient patient = choosePatient();
        if (patient == null) {
            return;
        }
        String who = askOr("Professional name or specialty (blank = anyone)", "");
        LocalDate from = askDate("From date (YYYY-MM-DD)", today());
        LocalDate to = askDate("To date (YYYY-MM-DD)", from.plusDays(14));

        List<TimeSlot> free = freeFutureSlots(who, from, to);
        if (free.isEmpty()) {
            out.println("No available slot for that search. (Iteration II: the patient could be added to a waiting list.)");
            return;
        }
        printSlots(free);
        int pick = askInt("Slot number (0 = cancel)", 0);
        if (pick < 1 || pick > Math.min(free.size(), MAX_LISTED)) {
            out.println("Booking abandoned.");
            return;
        }
        TimeSlot slot = free.get(pick - 1);

        Appointment appointment;
        try {
            appointment = clinic.booking().requestAppointment(patient.getPatientId(), slot.getSlotId(), channel);
        } catch (BookingException e) {
            if (e.getReason() != FailureReason.REFERRAL_REQUIRED) {
                throw e;
            }
            out.println(e.getMessage() + ".");
            if (!askOr("Record a referral now? (y/n)", "n").toLowerCase(Locale.ROOT).startsWith("y")) {
                out.println("Booking not made: a referral must be created first.");
                return;
            }
            recordReferralFor(patient, slot.getProfessional().getSpecialty());
            appointment = clinic.booking().requestAppointment(patient.getPatientId(), slot.getSlotId(), channel);
        }
        out.println("Requested: " + appointment);
        if (askOr("Confirm this booking? (y/n)", "y").toLowerCase(Locale.ROOT).startsWith("y")) {
            Appointment confirmed = clinic.booking().confirmAppointment(appointment.getAppointmentId());
            out.println("Confirmed: " + confirmed);
        } else {
            clinic.booking().cancelAppointment(appointment.getAppointmentId());
            out.println("Request cancelled. No slot was ever held, so nothing needed releasing.");
        }
    }

    private List<TimeSlot> freeFutureSlots(String who, LocalDate from, LocalDate to) {
        if (who.isBlank()) {
            return clinic.booking().getProfessionalAvailability(null, null, from, to);
        }
        return clinic.getProfessionals().search(who).stream()
                .flatMap(p -> clinic.booking().getProfessionalAvailability(p, null, from, to).stream())
                .sorted(java.util.Comparator.comparing(TimeSlot::getStartDateTime))
                .toList();
    }

    private void cancelAppointment() {
        String id = ask("Appointment ID (e.g. A00001)");
        boolean released = clinic.booking().cancelAppointment(id);
        out.println("Cancelled " + id + "." + (released
                ? " Its slot is available again. (Iteration II: eligible waitlisted patients would now be identified.)"
                : " It was not confirmed, so no slot needed releasing."));
    }

    private void listAppointments() {
        List<Appointment> all = clinic.getAppointments().all();
        if (all.isEmpty()) {
            out.println("No appointments.");
            return;
        }
        all.forEach(a -> out.println("  " + a));
    }

    // ---------------------------------------------------------------- patients, referrals, professionals

    private Patient choosePatient() {
        String criteria = ask("Find patient by RAMQ number, telephone or name (blank = register new)");
        if (!criteria.isBlank()) {
            List<Patient> found = clinic.booking().searchPatient(criteria);
            if (!found.isEmpty()) {
                for (int i = 0; i < found.size(); i++) {
                    out.printf("  %d) %s%n", i + 1, found.get(i));
                }
                int pick = askInt("Patient number (0 = register a new patient)", 1);
                if (pick >= 1 && pick <= found.size()) {
                    return found.get(pick - 1);
                }
            } else {
                out.println("No patient found.");
            }
        }
        if (!askOr("Register a new patient? (y/n)", "y").toLowerCase(Locale.ROOT).startsWith("y")) {
            return null;
        }
        return registerPatient();
    }

    private Patient registerPatient() {
        String first = ask("First name");
        String last = ask("Last name");
        String phone = ask("Telephone");
        String ramq = askOr("RAMQ number (blank if none)", "");
        Patient p = clinic.booking().registerPatient(first, last, phone, ramq);
        out.println("Registered " + p);
        return p;
    }

    private void recordReferral() {
        Patient patient = choosePatient();
        if (patient != null) {
            recordReferralFor(patient, ask("Specialty referred to (e.g. Cardiology)"));
        }
    }

    private void recordReferralFor(Patient patient, String specialty) {
        String referrer = ask("Referring professional's name");
        String reason = ask("Clinical reason");
        var referral = clinic.booking().recordReferral(patient.getPatientId(), specialty, referrer, reason);
        out.println("Recorded " + referral);
    }

    private HealthcareProfessional chooseProfessional(boolean allowNew) {
        String text = askOr("Professional name or specialty (blank = add a new professional)", "");
        if (!text.isBlank()) {
            List<HealthcareProfessional> found = clinic.getProfessionals().search(text);
            if (!found.isEmpty()) {
                for (int i = 0; i < found.size(); i++) {
                    out.printf("  %d) %s%n", i + 1, found.get(i));
                }
                int pick = askInt("Professional number (0 = add a new one)", 1);
                if (pick >= 1 && pick <= found.size()) {
                    return found.get(pick - 1);
                }
            } else {
                out.println("No professional found.");
            }
        }
        if (!allowNew) {
            return null;
        }
        String first = ask("New professional's first name");
        String last = ask("Last name");
        String title = askOr("Title (e.g. Dr., Nurse Practitioner)", "Dr.");
        String specialty = ask("Specialty");
        return clinic.availability().registerProfessional(first, last, title, specialty);
    }

    private void listProfessionals() {
        List<HealthcareProfessional> all = clinic.getProfessionals().all();
        if (all.isEmpty()) {
            out.println("No professionals yet. Import availability or make availability for a new professional.");
        }
        all.forEach(p -> out.println("  " + p.getProfessionalId() + "  " + p));
    }

    private void showActivityLog() {
        var entries = clinic.getActivityLog().entries();
        if (entries.isEmpty()) {
            out.println("The activity log is empty.");
        }
        entries.forEach(e -> out.println("  " + e));
    }

    // ---------------------------------------------------------------- input helpers

    private LocalDate today() {
        return LocalDate.now(clinic.getClock());
    }

    private String ask(String prompt) {
        out.print(prompt + ": ");
        out.flush();
        return in.nextLine().trim();
    }

    private String askOr(String prompt, String fallback) {
        out.print(prompt + (fallback.isEmpty() ? "" : " [" + fallback + "]") + ": ");
        out.flush();
        String value = in.nextLine().trim();
        return value.isEmpty() ? fallback : value;
    }

    private int askInt(String prompt, int fallback) {
        while (true) {
            String v = askOr(prompt, String.valueOf(fallback));
            try {
                return Integer.parseInt(v);
            } catch (NumberFormatException e) {
                out.println("Please enter a whole number.");
            }
        }
    }

    private LocalDate askDate(String prompt, LocalDate fallback) {
        while (true) {
            String v = askOr(prompt, fallback.toString());
            try {
                return LocalDate.parse(v);
            } catch (DateTimeParseException e) {
                out.println("Please use the format YYYY-MM-DD.");
            }
        }
    }

    private LocalTime askTime(String prompt) {
        while (true) {
            try {
                return ats.csv.CsvFormat.parseTime(ask(prompt));
            } catch (DateTimeParseException e) {
                out.println("Please use the format HH:MM, e.g. 09:00.");
            }
        }
    }

    private static DayOfWeek parseDay(String text) {
        String t = text.trim().toUpperCase(Locale.ROOT);
        for (DayOfWeek d : DayOfWeek.values()) {
            if (d.name().startsWith(t) && t.length() >= 2) {
                return d;
            }
        }
        throw new IllegalArgumentException("Unknown day of week: " + text);
    }
}
