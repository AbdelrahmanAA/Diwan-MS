package com.diwan.medical.service;

import com.diwan.common.web.ApiException;
import com.diwan.medical.domain.Severity;
import com.diwan.medical.dto.MedicalRecordRequest;
import com.diwan.medical.dto.MedicalRecordResponse;
import com.diwan.medical.dto.OverviewResponse;
import com.diwan.medical.dto.PageResponse;
import com.diwan.medical.entity.MedicalEntry;
import com.diwan.medical.entity.MedicalProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Records of the people of an account. It keeps and shows what the user entered and makes no medical judgement:
 * no interpretation of results, no advice (see SFDA MDS-G-027 on software that analyses medical data).
 * Text is encrypted, so filtering and sorting happen here rather than in SQL; one person has few records.
 */
@Service
public class MedicalRecordService {

    private static final int MAX_PAGE = 100;

    private final RecordCategories categories;
    private final ProfileService profiles;
    private final AuditService audit;
    private final Clock clock;

    public MedicalRecordService(RecordCategories categories, ProfileService profiles, AuditService audit, Clock clock) {
        this.categories = categories;
        this.profiles = profiles;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public MedicalRecordResponse create(Long userId, MedicalRecordRequest req) {
        return create(categories.require(req.getCategory()), userId, req);
    }

    private <E extends MedicalEntry> MedicalRecordResponse create(RecordCategory<E> c, Long userId, MedicalRecordRequest req) {
        c.validator().accept(req);
        MedicalProfile profile = profiles.require(userId, req.getProfileId());
        E e = c.single() ? c.repository().findFirstByProfileIdOrderByIdDesc(profile.getId()).orElseGet(c.factory())
                : c.factory().get();
        String action = e.getId() == null ? "CREATE" : "UPDATE";
        e.setUserId(userId);
        e.setProfile(profile);
        return save(c, e, req, action);
    }

    /** Replaces the record: a field left out is cleared. */
    @Transactional
    public MedicalRecordResponse update(Long id, Long userId, MedicalRecordRequest req) {
        return update(categories.require(req.getCategory()), id, userId, req);
    }

    private <E extends MedicalEntry> MedicalRecordResponse update(RecordCategory<E> c, Long id, Long userId, MedicalRecordRequest req) {
        c.validator().accept(req);
        return save(c, owned(c, id, userId), req, "UPDATE");
    }

    private <E extends MedicalEntry> MedicalRecordResponse save(RecordCategory<E> c, E e, MedicalRecordRequest req, String action) {
        c.writer().accept(req, e);
        e.setNotes(req.getNotes() == null || req.getNotes().isBlank() ? null : req.getNotes().trim());
        if (req.getRecordedAt() != null) {
            e.setRecordedAt(req.getRecordedAt());
        }
        E saved = c.repository().saveAndFlush(e);
        audit.record(e.getUserId(), e.getProfile().getId(), action, c.code(), saved.getId());
        return c.view(saved);
    }

    @Transactional
    public void delete(Long id, Long userId, String category) {
        delete(categories.require(category), id, userId);
    }

    private <E extends MedicalEntry> void delete(RecordCategory<E> c, Long id, Long userId) {
        E e = owned(c, id, userId);
        c.repository().delete(e);
        audit.record(userId, e.getProfile().getId(), "DELETE", c.code(), id);
    }

    /** A foreign or missing id gives the same 404, so the response does not reveal that a record exists. */
    private <E extends MedicalEntry> E owned(RecordCategory<E> c, Long id, Long userId) {
        return c.repository().findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("medical.record.not_found"));
    }

    @Transactional
    public List<MedicalRecordResponse> list(Long userId, Long profileId, String category) {
        MedicalProfile profile = profiles.require(userId, profileId);
        if (category != null && !category.isBlank()) {
            return records(categories.require(category), profile).toList();
        }
        return categories.all().stream().flatMap(c -> records(c, profile)).toList();
    }

    private <E extends MedicalEntry> Stream<MedicalRecordResponse> records(RecordCategory<E> c, MedicalProfile profile) {
        return c.repository().findByProfileIdOrderByIdDesc(profile.getId()).stream().map(c::view);
    }

    /** The person's history, newest first. */
    @Transactional
    public PageResponse<MedicalRecordResponse> timeline(Long userId, Long profileId, String category, String query,
                                                        int page, int size) {
        MedicalProfile profile = profiles.require(userId, profileId);
        Stream<RecordCategory<?>> shown = category == null || category.isBlank()
                ? categories.all().stream().filter(RecordCategory::onTimeline) : Stream.of(categories.require(category));
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<MedicalRecordResponse> all = shown.flatMap(c -> records(c, profile))
                .filter(r -> q.isEmpty() || contains(r.getTitle(), q) || contains(r.getNotes(), q))
                .sorted(Comparator.comparing(MedicalRecordResponse::getEventDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MedicalRecordResponse::getId, Comparator.reverseOrder()))
                .toList();
        int pageSize = Math.min(Math.max(1, size), MAX_PAGE);
        int number = Math.max(0, page);
        int from = (int) Math.min((long) number * pageSize, all.size());
        return new PageResponse<>(new ArrayList<>(all.subList(from, Math.min(from + pageSize, all.size()))), number, pageSize,
                all.size(), (all.size() + pageSize - 1) / pageSize);
    }

    private static boolean contains(String text, String lowerCaseQuery) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(lowerCaseQuery);
    }

    @Transactional
    public OverviewResponse overview(Long userId, Long profileId) {
        MedicalProfile profile = profiles.require(userId, profileId);
        LocalDate today = LocalDate.now(clock);
        Map<String, Long> counts = new LinkedHashMap<>();
        categories.all().forEach(c -> counts.put(c.code(), c.repository().countByProfileId(profile.getId())));
        return new OverviewResponse(ProfileService.view(profile),
                of(RecordCategories.BLOOD_TYPE, profile).stream().findFirst().map(MedicalRecordResponse::getBloodType).orElse(null),
                of(RecordCategories.ALLERGIES, profile).stream()
                        .sorted(Comparator.comparingInt(r -> Severity.rank(r.getSeverity()))).toList(),
                of(RecordCategories.MEDICATIONS, profile).stream()
                        .filter(r -> r.getEndDate() == null || !r.getEndDate().isBefore(today)).toList(),
                of(RecordCategories.CHRONIC_DISEASES, profile),
                of(RecordCategories.EMERGENCY_CONTACTS, profile),
                of(RecordCategories.VACCINES, profile).stream()
                        .filter(r -> r.getNextDoseDate() != null && !r.getNextDoseDate().isBefore(today))
                        .sorted(Comparator.comparing(MedicalRecordResponse::getNextDoseDate)).toList(),
                counts);
    }

    private List<MedicalRecordResponse> of(String category, MedicalProfile profile) {
        return records(categories.require(category), profile).toList();
    }
}
