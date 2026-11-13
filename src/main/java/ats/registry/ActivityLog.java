package ats.registry;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Records system operations and their outcomes for accountability (FR-30, Iteration I A6). */
public class ActivityLog {
    public record Entry(LocalDateTime timestamp, String operation, String outcome, String detail) {
        @Override
        public String toString() {
            return timestamp.withNano(0) + "  " + operation + "  " + outcome + (detail.isEmpty() ? "" : "  " + detail);
        }
    }

    private final Clock clock;
    private final List<Entry> entries = new ArrayList<>();

    public ActivityLog(Clock clock) {
        this.clock = clock;
    }

    public void success(String operation, String detail) {
        entries.add(new Entry(LocalDateTime.now(clock), operation, "Success", detail));
    }

    public void failure(String operation, String reason) {
        entries.add(new Entry(LocalDateTime.now(clock), operation, "Failure", reason));
    }

    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }
}
