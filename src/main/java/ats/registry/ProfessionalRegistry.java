package ats.registry;

import ats.domain.HealthcareProfessional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** In-memory store of healthcare professionals (FR-05, FR-06). */
public class ProfessionalRegistry {
    private final List<HealthcareProfessional> professionals = new ArrayList<>();
    private int nextId = 1;

    /** Returns the professional with this name, creating them if they are new (used by CSV import). */
    public HealthcareProfessional findOrCreate(String firstName, String lastName, String title, String specialty) {
        return findByName(firstName, lastName).orElseGet(() -> {
            HealthcareProfessional p = new HealthcareProfessional(
                    String.format("H%03d", nextId++), firstName, lastName, title, specialty);
            professionals.add(p);
            return p;
        });
    }

    public Optional<HealthcareProfessional> findByName(String firstName, String lastName) {
        return professionals.stream().filter(p -> p.hasName(firstName, lastName)).findFirst();
    }

    /** Finds by "First Last" as written in the booked-appointments CSV. */
    public Optional<HealthcareProfessional> findByFullName(String fullName) {
        String key = fullName.trim().replaceAll("\\s+", " ");
        return professionals.stream().filter(p -> p.getFullName().equalsIgnoreCase(key)).findFirst();
    }

    public Optional<HealthcareProfessional> findById(String professionalId) {
        return professionals.stream().filter(p -> p.getProfessionalId().equalsIgnoreCase(professionalId.trim())).findFirst();
    }

    /** Partial, case-insensitive match on name or specialty. */
    public List<HealthcareProfessional> search(String text) {
        String key = text.trim().toLowerCase(Locale.ROOT);
        return professionals.stream()
                .filter(p -> p.getFullName().toLowerCase(Locale.ROOT).contains(key)
                        || p.getSpecialty().toLowerCase(Locale.ROOT).contains(key))
                .toList();
    }

    public List<HealthcareProfessional> all() {
        return Collections.unmodifiableList(professionals);
    }
}
