package ats.controller;

import ats.Clinic;
import ats.TestClinics;
import ats.domain.HealthcareProfessional;
import ats.domain.TimeSlot;
import ats.domain.recurrence.MonthlyByDatePattern;
import ats.domain.recurrence.MonthlyByWeekdayPattern;
import ats.domain.recurrence.WeeklyPattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvailabilityControllerTest {
    private Clinic clinic;
    private HealthcareProfessional drX;

    @BeforeEach
    void setUp() {
        clinic = TestClinics.beforeSampleData();
        drX = clinic.availability().registerProfessional("Xavier", "Leblanc", "Dr.", "Family Medicine");
    }

    /** The example from the brief: during November, Dr. X is available every Monday 9am to 12pm. */
    @Test
    void everyMondayInNovemberNineToNoonInThirtyMinuteSlots() {
        AvailabilityResult r = clinic.availability().makeAvailability(drX.getProfessionalId(),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                new WeeklyPattern(Set.of(DayOfWeek.MONDAY)), LocalTime.of(9, 0), LocalTime.of(12, 0), 30);

        // Mondays in November 2026: 2, 9, 16, 23, 30 -> 5 days x 6 slots
        assertEquals(30, r.created().size());
        assertTrue(r.skipped().isEmpty());
        assertTrue(r.created().stream().allMatch(s -> s.getDate().getDayOfWeek() == DayOfWeek.MONDAY));
    }

    @Test
    void wholeBlockAsOneSlot() {
        AvailabilityResult r = clinic.availability().makeAvailability(drX.getProfessionalId(),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                new WeeklyPattern(Set.of(DayOfWeek.MONDAY)), LocalTime.of(9, 0), LocalTime.of(12, 0), 0);
        assertEquals(5, r.created().size());
        assertEquals(LocalTime.of(12, 0), r.created().get(0).getEnd());
    }

    @Test
    void overlappingSlotsAreSkippedNotDuplicated() {
        clinic.getAvailability().addSlot(drX, LocalDate.of(2026, 11, 2), LocalTime.of(9, 0), LocalTime.of(9, 30));
        AvailabilityResult r = clinic.availability().makeAvailability(drX.getProfessionalId(),
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 2),
                new WeeklyPattern(Set.of(DayOfWeek.MONDAY)), LocalTime.of(9, 0), LocalTime.of(10, 0), 30);
        assertEquals(1, r.created().size());
        assertEquals(1, r.skipped().size());
    }

    @Test
    void monthlyPatterns() {
        AvailabilityResult byDate = clinic.availability().makeAvailability(drX.getProfessionalId(),
                LocalDate.of(2026, 11, 1), LocalDate.of(2027, 1, 31),
                new MonthlyByDatePattern(15), LocalTime.of(13, 0), LocalTime.of(14, 0), 0);
        assertEquals(List.of(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 12, 15), LocalDate.of(2027, 1, 15)),
                byDate.created().stream().map(TimeSlot::getDate).toList());

        AvailabilityResult firstTuesday = clinic.availability().makeAvailability(drX.getProfessionalId(),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31),
                new MonthlyByWeekdayPattern(1, DayOfWeek.TUESDAY), LocalTime.of(8, 0), LocalTime.of(9, 0), 0);
        assertEquals(List.of(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 12, 1)),
                firstTuesday.created().stream().map(TimeSlot::getDate).toList());
    }

    @Test
    void slotLengthMustDivideTheBlock() {
        assertThrows(IllegalArgumentException.class, () -> clinic.availability().makeAvailability(drX.getProfessionalId(),
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 2),
                new WeeklyPattern(Set.of(DayOfWeek.MONDAY)), LocalTime.of(9, 0), LocalTime.of(10, 0), 45));
    }

    @Test
    void searchBySpecialtyAndDateRange() {
        HealthcareProfessional derm = clinic.availability().registerProfessional("Nadia", "Haddad", "Dr.", "Dermatology");
        clinic.getAvailability().addSlot(derm, LocalDate.of(2026, 11, 3), LocalTime.of(13, 0), LocalTime.of(13, 30));
        clinic.getAvailability().addSlot(drX, LocalDate.of(2026, 11, 3), LocalTime.of(13, 0), LocalTime.of(13, 30));
        clinic.getAvailability().addSlot(derm, LocalDate.of(2026, 12, 3), LocalTime.of(13, 0), LocalTime.of(13, 30));

        List<TimeSlot> found = clinic.availability().searchAvailability(null, "dermatology",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), true);
        assertEquals(1, found.size());
        assertEquals(derm, found.get(0).getProfessional());
    }
}
