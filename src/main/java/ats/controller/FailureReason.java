package ats.controller;

/** Failure reasons; the first four are the ones named in the Iteration I contracts. */
public enum FailureReason {
    PROFESSIONAL_UNAVAILABLE("ProfessionalUnavailable"),
    SCHEDULING_CONSTRAINT_VIOLATION("SchedulingConstraintViolation"),
    REFERRAL_REQUIRED("ReferralRequired"),
    REFERRAL_NOT_VALID("ReferralNotValid"),
    PATIENT_NOT_FOUND("PatientNotFound"),
    SLOT_NOT_FOUND("SlotNotFound"),
    APPOINTMENT_NOT_FOUND("AppointmentNotFound"),
    INVALID_STATUS("InvalidStatus");

    private final String contractName;

    FailureReason(String contractName) {
        this.contractName = contractName;
    }

    /** The reason as it is written in the operation contracts. */
    public String contractName() {
        return contractName;
    }
}
