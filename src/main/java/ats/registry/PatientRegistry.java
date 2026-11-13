package ats.registry;

import ats.domain.Patient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** In-memory store of patients; information expert for patient lookup (FR-01, FR-03). */
public class PatientRegistry {
    private final Map<String, Patient> patients = new LinkedHashMap<>();
    private int nextId = 1;

    /** Registers a new patient. A RAMQ number, when given, must be unique (FR-01). */
    public Patient register(String firstName, String lastName, String telephone, String ramqNumber) {
        if (ramqNumber != null && !ramqNumber.isBlank() && findByRamq(ramqNumber).isPresent()) {
            throw new IllegalArgumentException("A patient with RAMQ number " + ramqNumber + " is already registered");
        }
        Patient patient = new Patient(String.format("P%04d", nextId++), firstName, lastName, telephone, ramqNumber);
        patients.put(patient.getPatientId(), patient);
        return patient;
    }

    public Optional<Patient> findById(String patientId) {
        return Optional.ofNullable(patients.get(patientId));
    }

    public Optional<Patient> findByRamq(String ramqNumber) {
        String key = ramqNumber.trim().toUpperCase(Locale.ROOT);
        return patients.values().stream().filter(p -> key.equals(p.getRamqNumber())).findFirst();
    }

    public List<Patient> findByTelephone(String telephone) {
        String key = digits(telephone);
        return patients.values().stream().filter(p -> digits(p.getTelephone()).equals(key)).toList();
    }

    /** Case-insensitive partial match on first or last name (Iteration I, A8.5). */
    public List<Patient> searchByName(String partialName) {
        String key = partialName.trim().toLowerCase(Locale.ROOT);
        return patients.values().stream()
                .filter(p -> p.getFirstName().toLowerCase(Locale.ROOT).contains(key)
                        || p.getLastName().toLowerCase(Locale.ROOT).contains(key)
                        || p.getFullName().toLowerCase(Locale.ROOT).contains(key))
                .toList();
    }

    /** One search box for the CLI: tries RAMQ number, then telephone, then name. */
    public List<Patient> search(String criteria) {
        Optional<Patient> byRamq = findByRamq(criteria);
        if (byRamq.isPresent()) {
            return List.of(byRamq.get());
        }
        if (digits(criteria).length() >= 7) {
            List<Patient> byPhone = findByTelephone(criteria);
            if (!byPhone.isEmpty()) {
                return byPhone;
            }
        }
        return searchByName(criteria);
    }

    public List<Patient> all() {
        return Collections.unmodifiableList(new ArrayList<>(patients.values()));
    }

    private static String digits(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }
}
