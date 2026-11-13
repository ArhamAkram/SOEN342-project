package ats.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** A healthcare professional (FR-05, FR-06). */
public class HealthcareProfessional {
    /** Specialties that are primary care: no referral is needed to see them (Iteration I, A3). */
    private static final Set<String> PRIMARY_CARE = Set.of("family medicine", "primary care", "general practice", "nursing");

    private final String professionalId;
    private final String firstName;
    private final String lastName;
    private final String title;
    private final String specialty;

    public HealthcareProfessional(String professionalId, String firstName, String lastName, String title, String specialty) {
        this.professionalId = Objects.requireNonNull(professionalId);
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
        this.title = title == null ? "" : title.trim();
        this.specialty = specialty.trim();
    }

    public String getProfessionalId() { return professionalId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return firstName + " " + lastName; }
    public String getTitle() { return title; }
    public String getSpecialty() { return specialty; }

    /** A specialist is anyone outside primary care; a first visit requires a referral (A3). */
    public boolean isSpecialist() {
        return !PRIMARY_CARE.contains(specialty.toLowerCase(Locale.ROOT));
    }

    public boolean hasName(String first, String last) {
        return firstName.equalsIgnoreCase(first.trim()) && lastName.equalsIgnoreCase(last.trim());
    }

    @Override
    public String toString() {
        return (title.isEmpty() ? "" : title + " ") + getFullName() + " - " + specialty;
    }
}
