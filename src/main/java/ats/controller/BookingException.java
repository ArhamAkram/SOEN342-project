package ats.controller;

/** A system operation failed for a reason the actor should be told about. */
public class BookingException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final FailureReason reason;

    public BookingException(FailureReason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public FailureReason getReason() {
        return reason;
    }
}
