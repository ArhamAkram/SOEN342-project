package ats.controller;

import ats.csv.AppointmentCsvImporter;
import ats.csv.AvailabilityCsvExporter;
import ats.csv.AvailabilityCsvImporter;
import ats.csv.ImportResult;
import ats.registry.ActivityLog;
import ats.registry.AppointmentBook;
import ats.registry.AvailabilityCatalog;
import ats.registry.PatientRegistry;
import ats.registry.ProfessionalRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Use-case controller for UC-17 Import Availability from CSV and UC-18 Export Availability to CSV. */
public class CsvController {
    private final AvailabilityCsvImporter availabilityImporter;
    private final AppointmentCsvImporter appointmentImporter;
    private final AvailabilityCsvExporter exporter;
    private final ActivityLog log;

    public CsvController(PatientRegistry patients, ProfessionalRegistry professionals, AvailabilityCatalog availability,
                         AppointmentBook appointments, ActivityLog log) {
        this.availabilityImporter = new AvailabilityCsvImporter(professionals, availability);
        this.appointmentImporter = new AppointmentCsvImporter(patients, professionals, availability, appointments);
        this.exporter = new AvailabilityCsvExporter(availability);
        this.log = log;
    }

    /** importAvailability(sourceFile) */
    public ImportResult importAvailability(Path source) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            ImportResult result = availabilityImporter.importFrom(reader);
            log.success("importAvailability", source.getFileName() + ": " + result);
            return result;
        }
    }

    /** importAppointments(sourceFile) */
    public ImportResult importAppointments(Path source) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            ImportResult result = appointmentImporter.importFrom(reader);
            log.success("importAppointments", source.getFileName() + ": " + result);
            return result;
        }
    }

    /** exportAvailability(targetFile) - writes every slot, booked or not. */
    public int exportAvailability(Path target) throws IOException {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(target, StandardCharsets.UTF_8))) {
            int count = exporter.exportTo(writer);
            log.success("exportAvailability", target.getFileName() + ": " + count + " slots");
            return count;
        }
    }
}
