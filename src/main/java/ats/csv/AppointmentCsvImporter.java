package ats.csv;

import ats.domain.Appointment;
import ats.domain.BookingChannel;
import ats.domain.HealthcareProfessional;
import ats.domain.Patient;
import ats.domain.TimeSlot;
import ats.registry.AppointmentBook;
import ats.registry.AvailabilityCatalog;
import ats.registry.PatientRegistry;
import ats.registry.ProfessionalRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * Reads already-booked appointments:
 * Professional, Date, Start, End, Patient, Telephone, RAMQ No.
 * The professional must already exist (import availability first). The patient is found by
 * RAMQ number or telephone, or registered. The matching slot is found (or created) and booked.
 * These are existing bookings, so they are recorded as confirmed without the booking checks.
 */
public class AppointmentCsvImporter {
    private final PatientRegistry patients;
    private final ProfessionalRegistry professionals;
    private final AvailabilityCatalog availability;
    private final AppointmentBook appointments;

    public AppointmentCsvImporter(PatientRegistry patients, ProfessionalRegistry professionals,
                                  AvailabilityCatalog availability, AppointmentBook appointments) {
        this.patients = patients;
        this.professionals = professionals;
        this.availability = availability;
        this.appointments = appointments;
    }

    public ImportResult importFrom(BufferedReader reader) throws IOException {
        ImportResult result = new ImportResult();
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (lineNumber == 1) {
                line = CsvFormat.stripBom(line);
            }
            if (line.isBlank()) {
                continue;
            }
            List<String> f = CsvFormat.parseLine(line);
            if (lineNumber == 1 && f.get(0).equalsIgnoreCase("Professional")) {
                continue; // header
            }
            if (f.size() < 6) {
                result.skipped(lineNumber, "expected at least 6 columns, found " + f.size());
                continue;
            }
            try {
                Optional<HealthcareProfessional> professional = professionals.findByFullName(f.get(0));
                if (professional.isEmpty()) {
                    result.skipped(lineNumber, "unknown professional '" + f.get(0) + "' (import availability first)");
                    continue;
                }
                LocalDate date = LocalDate.parse(f.get(1));
                LocalTime start = CsvFormat.parseTime(f.get(2));
                LocalTime end = CsvFormat.parseTime(f.get(3));
                String ramq = f.size() > 6 && !CsvFormat.isNone(f.get(6)) ? f.get(6) : null;
                Patient patient = findOrRegisterPatient(f.get(4), f.get(5), ramq);

                TimeSlot slot = availability.findExact(professional.get(), date, start, end)
                        .orElseGet(() -> availability.addSlot(professional.get(), date, start, end));
                if (slot.isBooked()) {
                    result.skipped(lineNumber, "slot " + slot.getSlotId() + " is already booked");
                    continue;
                }
                Appointment appointment = appointments.create(patient, slot, BookingChannel.PHONE, null);
                appointment.confirm();
                result.imported();
            } catch (DateTimeParseException e) {
                result.skipped(lineNumber, "bad date or time (" + e.getParsedString() + ")");
            } catch (IllegalArgumentException | IllegalStateException e) {
                result.skipped(lineNumber, e.getMessage());
            }
        }
        return result;
    }

    private Patient findOrRegisterPatient(String fullName, String telephone, String ramq) {
        if (ramq != null) {
            Optional<Patient> byRamq = patients.findByRamq(ramq);
            if (byRamq.isPresent()) {
                return byRamq.get();
            }
        }
        String name = fullName.trim().replaceAll("\\s+", " ");
        Optional<Patient> byPhoneAndName = patients.findByTelephone(telephone).stream()
                .filter(p -> p.getFullName().equalsIgnoreCase(name))
                .findFirst();
        if (byPhoneAndName.isPresent()) {
            return byPhoneAndName.get();
        }
        int space = name.indexOf(' ');
        if (space < 0) {
            throw new IllegalArgumentException("patient name needs a first and last name: '" + fullName + "'");
        }
        return patients.register(name.substring(0, space), name.substring(space + 1), telephone, ramq);
    }
}
