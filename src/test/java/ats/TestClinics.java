package ats;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Shared test fixtures: a clinic whose clock is fixed, so "now" never moves. */
public final class TestClinics {
    public static final ZoneId ZONE = ZoneId.of("America/Toronto");

    private TestClinics() {
    }

    public static Clinic at(LocalDateTime now) {
        return new Clinic(Clock.fixed(now.atZone(ZONE).toInstant(), ZONE));
    }

    /** Sunday 2026-11-01 08:00 - before every slot in the sample data. */
    public static Clinic beforeSampleData() {
        return at(LocalDateTime.of(2026, 11, 1, 8, 0));
    }
}
