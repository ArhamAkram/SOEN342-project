package ats.csv;

import ats.domain.TimeSlot;
import ats.registry.AvailabilityCatalog;

import java.io.PrintWriter;
import java.util.List;

/**
 * Writes the entire availability database:
 * First Name, Last Name, Title, Specialty, Date, Start, End, Status
 * The first seven columns match the import format, so an export can be imported again.
 */
public class AvailabilityCsvExporter {
    public static final List<String> HEADER =
            List.of("First Name", "Last Name", "Title", "Specialty", "Date", "Start", "End", "Status");

    private final AvailabilityCatalog availability;

    public AvailabilityCsvExporter(AvailabilityCatalog availability) {
        this.availability = availability;
    }

    /** @return the number of slots written */
    public int exportTo(PrintWriter writer) {
        writer.println(CsvFormat.formatLine(HEADER));
        List<TimeSlot> slots = availability.all();
        for (TimeSlot s : slots) {
            writer.println(CsvFormat.formatLine(List.of(
                    s.getProfessional().getFirstName(),
                    s.getProfessional().getLastName(),
                    s.getProfessional().getTitle(),
                    s.getProfessional().getSpecialty(),
                    s.getDate().toString(),
                    s.getStart().format(CsvFormat.TIME),
                    s.getEnd().format(CsvFormat.TIME),
                    s.isBooked() ? "Booked" : "Available")));
        }
        writer.flush();
        return slots.size();
    }
}
