package ats.domain;

import java.util.Locale;
import java.util.Objects;

/** A registered patient (FR-01 to FR-04). */
public class Patient {
    private final String patientId;
    private final String firstName;
    private final String lastName;
    private final String telephone;
    private final String ramqNumber; // null when the patient has no RAMQ coverage

    public Patient(String patientId, String firstName, String lastName, String telephone, String ramqNumber) {
        this.patientId = Objects.requireNonNull(patientId);
        this.firstName = requireText(firstName, "first name");
        this.lastName = requireText(lastName, "last name");
        this.telephone = requireText(telephone, "telephone");
        this.ramqNumber = (ramqNumber == null || ramqNumber.isBlank()) ? null : ramqNumber.trim().toUpperCase(Locale.ROOT);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Patient " + field + " is required");
        }
        return value.trim();
    }

    public String getPatientId() { return patientId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return firstName + " " + lastName; }
    public String getTelephone() { return telephone; }
    public String getRamqNumber() { return ramqNumber; }
    public boolean hasRamq() { return ramqNumber != null; }

    @Override
    public String toString() {
        return patientId + " " + getFullName() + " (" + telephone + (hasRamq() ? ", RAMQ " + ramqNumber : ", no RAMQ") + ")";
    }
}
