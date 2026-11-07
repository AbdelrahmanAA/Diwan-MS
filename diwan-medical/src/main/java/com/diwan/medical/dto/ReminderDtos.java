package com.diwan.medical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ReminderDtos {

    private ReminderDtos() {}

    /** @param person null for the account holder without a name */
    public record MedicationReminder(Long id, Long profileId, String person, boolean self, String name, String dosage,
                                     List<String> times, LocalDate startDate, LocalDate endDate) {}

    public record AppointmentReminder(Long id, Long profileId, String person, boolean self, String with,
                                      LocalDateTime at, String location) {}

    /** Everything the phone schedules notifications for, across the account's people. */
    public record ReminderPlan(List<MedicationReminder> medications, List<AppointmentReminder> appointments) {}

    public record DoseRequest(@NotNull(message = "{validation.required}") Long medicationId,
                              @NotNull(message = "{validation.required}") LocalDateTime scheduledAt,
                              @NotBlank(message = "{validation.required}") String status) {}

    public record DoseResponse(Long id, Long medicationId, Long profileId, LocalDateTime scheduledAt, String status,
                               Instant recordedAt) {}
}
