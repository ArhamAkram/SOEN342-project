package ats.registry;

import ats.domain.Patient;
import ats.domain.Referral;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** In-memory store of referrals; expert on whether a valid referral exists (A4). */
public class ReferralRegistry {
    private final List<Referral> referrals = new ArrayList<>();
    private int nextId = 1;

    public Referral record(Patient patient, String specialty, String referringProfessionalName, String clinicalReason, LocalDate issuedDate) {
        Referral referral = new Referral(String.format("R%04d", nextId++), patient, specialty,
                referringProfessionalName, clinicalReason, issuedDate);
        referrals.add(referral);
        return referral;
    }

    public Optional<Referral> findValid(Patient patient, String specialty, LocalDate onDate) {
        return referrals.stream().filter(r -> r.isValidFor(patient, specialty, onDate)).findFirst();
    }

    public List<Referral> all() {
        return Collections.unmodifiableList(referrals);
    }
}
