package com.diwan.medical.service;

import com.diwan.common.web.ApiException;
import com.diwan.medical.domain.ProfileRelation;
import com.diwan.medical.dto.ReminderDtos.AppointmentReminder;
import com.diwan.medical.dto.ReminderDtos.DoseRequest;
import com.diwan.medical.dto.ReminderDtos.DoseResponse;
import com.diwan.medical.dto.ReminderDtos.MedicationReminder;
import com.diwan.medical.dto.ReminderDtos.ReminderPlan;
import com.diwan.medical.entity.MedicalDose;
import com.diwan.medical.entity.MedicalMedication;
import com.diwan.medical.entity.MedicalProfile;
import com.diwan.medical.repository.MedicalAppointmentRepository;
import com.diwan.medical.repository.MedicalDoseRepository;
import com.diwan.medical.repository.MedicalMedicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Dose schedules and appointments for the phone to remind about, and the log of doses taken or skipped. The
 * phone schedules the notifications itself (no push service); times are the phone's local time.
 */
@Service
public class ReminderService {

    /** How far ahead appointment reminders are planned */
    static final int APPOINTMENT_DAYS = 60;

    private final MedicalMedicationRepository medications;
    private final MedicalAppointmentRepository appointments;
    private final MedicalDoseRepository doses;
    private final ProfileService profiles;
    private final AuditService audit;
    private final Clock clock;

    public ReminderService(MedicalMedicationRepository medications, MedicalAppointmentRepository appointments,
                           MedicalDoseRepository doses, ProfileService profiles, AuditService audit, Clock clock) {
        this.medications = medications;
        this.appointments = appointments;
        this.doses = doses;
        this.profiles = profiles;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ReminderPlan plan(Long userId) {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);
        List<MedicationReminder> meds = medications.findByUserIdOrderByIdAsc(userId).stream()
                .filter(m -> m.isReminders() && m.getScheduleTimes() != null)
                .filter(m -> m.getEndDate() == null || !m.getEndDate().isBefore(today))
                .map(m -> new MedicationReminder(m.getId(), m.getProfile().getId(), m.getProfile().getName(), isSelf(m.getProfile()),
                        m.getName(), m.getDosage(), RecordCategories.timesOf(m.getScheduleTimes()), m.getStartDate(), m.getEndDate()))
                .toList();
        List<AppointmentReminder> visits = appointments.findByUserIdOrderByIdAsc(userId).stream()
                .filter(a -> a.getAppointmentAt().isAfter(now) && a.getAppointmentAt().isBefore(now.plusDays(APPOINTMENT_DAYS)))
                .sorted(Comparator.comparing(a -> a.getAppointmentAt()))
                .map(a -> new AppointmentReminder(a.getId(), a.getProfile().getId(), a.getProfile().getName(), isSelf(a.getProfile()),
                        a.getAppointmentWith(), a.getAppointmentAt(), a.getLocation()))
                .toList();
        return new ReminderPlan(meds, visits);
    }

    private static boolean isSelf(MedicalProfile p) {
        return p.getRelation() == ProfileRelation.SELF;
    }

    /** Answering the same dose again replaces the answer. */
    @Transactional
    public DoseResponse record(Long userId, DoseRequest req) {
        MedicalMedication m = medications.findByIdAndUserId(req.medicationId(), userId)
                .orElseThrow(() -> ApiException.notFound("medical.record.not_found"));
        MedicalDose.Status status;
        try {
            status = MedicalDose.Status.valueOf(req.status().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("medical.dose.status_invalid");
        }
        LocalDateTime at = req.scheduledAt().withSecond(0).withNano(0);
        MedicalDose d = doses.findByMedicationIdAndScheduledAt(m.getId(), at).orElseGet(MedicalDose::new);
        d.setUserId(userId);
        d.setProfileId(m.getProfile().getId());
        d.setMedicationId(m.getId());
        d.setScheduledAt(at);
        d.setStatus(status);
        d.setRecordedAt(clock.instant());
        doses.save(d);
        audit.record(userId, m.getProfile().getId(), "DOSE", RecordCategories.MEDICATIONS, m.getId());
        return view(d);
    }

    /** The answered doses of one person on one day (may create the account holder's profile on first use). */
    @Transactional
    public List<DoseResponse> doses(Long userId, Long profileId, LocalDate day) {
        MedicalProfile p = profiles.require(userId, profileId);
        return doses.findByProfileIdAndScheduledAtBetweenOrderByScheduledAtAsc(p.getId(), day.atStartOfDay(),
                day.plusDays(1).atStartOfDay().minusNanos(1)).stream().map(ReminderService::view).toList();
    }

    private static DoseResponse view(MedicalDose d) {
        return new DoseResponse(d.getId(), d.getMedicationId(), d.getProfileId(), d.getScheduledAt(), d.getStatus().name(),
                d.getRecordedAt());
    }
}
