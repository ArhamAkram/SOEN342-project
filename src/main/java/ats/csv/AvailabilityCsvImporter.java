package ats.csv;

import ats.domain.HealthcareProfessional;
import ats.registry.AvailabilityCatalog;
import ats.registry.ProfessionalRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Reads professional availability rows:
 * First Name, Last Name, Title, Specialty, Date, Start, End [, Status]
 * A header row is skipped. The optional Status column (written by the exporter) is ignored:
 * bookings are imported from the booked-appointments file instead.
 */
public class AvailabilityCsvImporter {
    private final ProfessionalRegistry professionals;
    private final AvailabilityCatalog availability;

    public AvailabilityCsvImporter(ProfessionalRegistry professionals, AvailabilityCatalog availability) {
        this.professionals = professionals;
        this.availability = availability;
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
            if (lineNumber == 1 && f.get(0).equalsIgnoreCase("First Name")) {
                continue; // header
            }
            if (f.size() < 7) {
                result.skipped(lineNumber, "expected 7 columns, found " + f.size());
                continue;
            }
            try {
                LocalDate date = LocalDate.parse(f.get(4));
                LocalTime start = CsvFormat.parseTime(f.get(5));
                LocalTime end = CsvFormat.parseTime(f.get(6));
                if (f.get(0).isBlank() || f.get(1).isBlank() || f.get(3).isBlank()) {
                    result.skipped(lineNumber, "first name, last name and specialty are required");
                    continue;
                }
                HealthcareProfessional professional = professionals.findOrCreate(f.get(0), f.get(1), f.get(2), f.get(3));
                availability.addSlot(professional, date, start, end);
                result.imported();
            } catch (DateTimeParseException e) {
                result.skipped(lineNumber, "bad date or time (" + e.getParsedString() + ")");
            } catch (IllegalArgumentException e) {
                result.skipped(lineNumber, e.getMessage());
            }
        }
        return result;
    }
}
