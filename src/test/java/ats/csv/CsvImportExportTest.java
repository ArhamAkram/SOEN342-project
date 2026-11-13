package ats.csv;

import ats.Clinic;
import ats.TestClinics;
import ats.domain.AppointmentStatus;
import ats.domain.Patient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvImportExportTest {
    private static final Path DATA = Path.of("data");

    @Test
    void importsSampleAvailability() throws Exception {
        Clinic clinic = TestClinics.beforeSampleData();
        ImportResult r = clinic.csv().importAvailability(DATA.resolve("availability.csv"));

        assertEquals(20, r.getImportedCount());
        assertTrue(r.getSkipped().isEmpty(), r.getSkipped().toString());
        assertEquals(5, clinic.getProfessionals().all().size());
        assertTrue(clinic.getProfessionals().findByName("Amélie", "Tremblay").isPresent(), "accents survive UTF-8");
    }

    @Test
    void importsSampleAppointmentsAndBooksTheirSlots() throws Exception {
        Clinic clinic = TestClinics.beforeSampleData();
        clinic.csv().importAvailability(DATA.resolve("availability.csv"));
        ImportResult r = clinic.csv().importAppointments(DATA.resolve("appointments.csv"));

        assertEquals(5, r.getImportedCount(), r.getSkipped().toString());
        assertEquals(4, clinic.getPatients().all().size(), "Thomas Girard is matched by RAMQ the second time");
        assertTrue(clinic.getAppointments().all().stream().allMatch(a -> a.getStatus() == AppointmentStatus.CONFIRMED));
        Patient fatima = clinic.getPatients().searchByName("Fatima").get(0);
        assertFalse(fatima.hasRamq(), "the dash means no RAMQ number");
        long booked = clinic.getAvailability().all().stream().filter(s -> s.isBooked()).count();
        assertEquals(5, booked);
    }

    @Test
    void appointmentForUnknownProfessionalIsSkipped(@TempDir Path dir) throws Exception {
        Clinic clinic = TestClinics.beforeSampleData();
        Path file = dir.resolve("a.csv");
        Files.writeString(file, "Professional,Date,Start,End,Patient,Telephone,RAMQ No.\n"
                + "Nobody Here,2026-11-02,09:00,09:30,Jo Smith,514-555-9999,-\n", StandardCharsets.UTF_8);
        ImportResult r = clinic.csv().importAppointments(file);
        assertEquals(0, r.getImportedCount());
        assertEquals(1, r.getSkipped().size());
        assertTrue(r.getSkipped().get(0).startsWith("line 2"));
    }

    @Test
    void badRowsAreReportedAndGoodRowsStillImported(@TempDir Path dir) throws Exception {
        Clinic clinic = TestClinics.beforeSampleData();
        Path file = dir.resolve("bad.csv");
        Files.writeString(file, String.join("\n",
                "First Name,Last Name,Title,Specialty,Date,Start,End",
                "Ana,Lopez,Dr.,Family Medicine,2026-11-30,09:00,09:30",
                "Ana,Lopez,Dr.,Family Medicine,2026-13-01,09:00,09:30",
                "Ana,Lopez,Dr.,Family Medicine,2026-11-30,09:15,09:45",
                "too,few,columns"), StandardCharsets.UTF_8);
        ImportResult r = clinic.csv().importAvailability(file);
        assertEquals(1, r.getImportedCount());
        assertEquals(3, r.getSkipped().size());
    }

    @Test
    void exportThenImportGivesTheSameAvailability(@TempDir Path dir) throws Exception {
        Clinic original = TestClinics.beforeSampleData();
        original.csv().importAvailability(DATA.resolve("availability.csv"));
        original.csv().importAppointments(DATA.resolve("appointments.csv"));
        Path out = dir.resolve("export.csv");
        int written = original.csv().exportAvailability(out);

        List<String> lines = Files.readAllLines(out, StandardCharsets.UTF_8);
        assertEquals(written + 1, lines.size());
        assertEquals("First Name,Last Name,Title,Specialty,Date,Start,End,Status", lines.get(0));
        assertEquals(5, lines.stream().filter(l -> l.endsWith(",Booked")).count());

        Clinic copy = TestClinics.beforeSampleData();
        ImportResult r = copy.csv().importAvailability(out);
        assertEquals(written, r.getImportedCount());
        assertEquals(original.getAvailability().size(), copy.getAvailability().size());
    }

    @Test
    void quotedFieldsWithCommasRoundTrip() {
        List<String> fields = List.of("Dubois", "Palpitations, consultation requested", "say \"hi\"");
        assertEquals(fields, CsvFormat.parseLine(CsvFormat.formatLine(fields)));
    }
}
