package ats.controller;

import ats.domain.TimeSlot;

import java.util.List;

/** Outcome of makeAvailability: the slots created and the ones skipped (with why). */
public record AvailabilityResult(List<TimeSlot> created, List<String> skipped) {
}
