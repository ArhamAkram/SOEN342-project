package ats.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A referral (consultation request) for a patient to a specialty (FR-21, FR-22).
 * The PoC keeps only what the booking check needs (Iteration I, assumption A4).
 */
public class Referral {
    private final String referralId;
    private final Patient patient;
    private final String requestedSpecialty;
    private final String referringProfessionalName;
    private final String clinicalReason;
    private final LocalDate issuedDate;
    private final LocalDate expiryDate;
    private ReferralStatus status;

    public Referral(String referralId, Patient patient, String requestedSpecialty, String referringProfessionalName,
                    String clinicalReason, LocalDate issuedDate) {
        if (clinicalReason == null || clinicalReason.isBlank()) {
            throw new IllegalArgumentException("A referral needs a clinical reason");
        }
        this.referralId = Objects.requireNonNull(referralId);
        this.patient = Objects.requireNonNull(patient);
        this.requestedSpecialty = requestedSpecialty.trim();
        this.referringProfessionalName = referringProfessionalName.trim();
        this.clinicalReason = clinicalReason.trim();
        this.issuedDate = Objects.requireNonNull(issuedDate);
        this.expiryDate = issuedDate.plusMonths(12);
        this.status = ReferralStatus.SUBMITTED;
    }

    public String getReferralId() { return referralId; }
    public Patient getPatient() { return patient; }
    public String getRequestedSpecialty() { return requestedSpecialty; }
    public String getReferringProfessionalName() { return referringProfessionalName; }
    public String getClinicalReason() { return clinicalReason; }
    public LocalDate getIssuedDate() { return issuedDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public ReferralStatus getStatus() { return status; }
    public void setStatus(ReferralStatus status) { this.status = Objects.requireNonNull(status); }

    /** A4 rules 1-4: valid status, not expired, same patient, same specialty. */
    public boolean isValidFor(Patient p, String specialty, LocalDate onDate) {
        boolean usableStatus = status == ReferralStatus.SUBMITTED || status == ReferralStatus.ACCEPTED;
        return usableStatus
                && !onDate.isAfter(expiryDate)
                && patient == p
                && requestedSpecialty.equalsIgnoreCase(specialty);
    }

    @Override
    public String toString() {
        return referralId + " " + patient.getFullName() + " -> " + requestedSpecialty + " (" + status + ", expires " + expiryDate + ")";
    }
}
