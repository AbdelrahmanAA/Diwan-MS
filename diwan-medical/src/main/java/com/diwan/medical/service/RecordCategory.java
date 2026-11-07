package com.diwan.medical.service;

import com.diwan.medical.dto.MedicalRecordRequest;
import com.diwan.medical.dto.MedicalRecordResponse;
import com.diwan.medical.entity.MedicalEntry;
import com.diwan.medical.repository.MedicalEntryRepository;

import java.time.LocalDate;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Everything that differs between record categories.
 * @param single only one record per person (a blood type): saving another replaces it
 * @param onTimeline shown in the medical history (emergency contacts are not events)
 * @param title the record's main text
 * @param date when it happened, null when the category or record has no such date
 */
public record RecordCategory<E extends MedicalEntry>(
        String code, MedicalEntryRepository<E> repository, Supplier<E> factory, boolean single, boolean onTimeline,
        Function<E, String> title, Function<E, LocalDate> date, Consumer<MedicalRecordRequest> validator,
        BiConsumer<MedicalRecordRequest, E> writer, BiConsumer<E, MedicalRecordResponse> reader) {

    MedicalRecordResponse view(E e) {
        MedicalRecordResponse r = new MedicalRecordResponse();
        r.setId(e.getId());
        r.setProfileId(e.getProfile().getId());
        r.setCategory(code);
        r.setTitle(title.apply(e));
        LocalDate happened = date.apply(e);
        r.setEventDate(happened != null ? happened : e.getRecordedAt() == null ? null : e.getRecordedAt().toLocalDate());
        r.setNotes(e.getNotes());
        r.setRecordedAt(e.getRecordedAt());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());
        reader.accept(e, r);
        return r;
    }
}
