package com.diwan.medical.service;

import com.diwan.medical.dto.MedicalRecordRequest;
import com.diwan.medical.entity.MedicalAllergy;
import com.diwan.medical.entity.MedicalMedication;
import com.diwan.medical.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Medical data is private: only the owner may change or delete a record. */
class MedicalRecordServiceTest {

    private final MedicalMedicationRepository medications = mock(MedicalMedicationRepository.class);
    private final MedicalAllergyRepository allergies = mock(MedicalAllergyRepository.class);
    private final MedicalRecordService service = new MedicalRecordService(
            medications, allergies, mock(MedicalBloodTypeRepository.class), mock(MedicalSurgeryRepository.class),
            mock(MedicalChronicDiseaseRepository.class), mock(MedicalLabResultRepository.class),
            mock(MedicalVaccineRepository.class), mock(MedicalFamilyHistoryRepository.class));

    private static MedicalMedication medicationOf(Long userId) {
        MedicalMedication m = new MedicalMedication();
        m.setUserId(userId);
        m.setName("Metformin");
        m.setDosage("500mg");
        return m;
    }

    private static MedicalRecordRequest dosage(String value) {
        MedicalRecordRequest r = new MedicalRecordRequest();
        r.setCategory("medications");
        r.setDosage(value);
        return r;
    }

    private static HttpStatus statusOf(Runnable action) {
        return (HttpStatus) assertThrows(ResponseStatusException.class, action::run).getStatusCode();
    }

    @Test
    void ownerCanUpdate() {
        MedicalMedication m = medicationOf(7L);
        when(medications.findById(1L)).thenReturn(Optional.of(m));
        when(medications.save(m)).thenReturn(m);

        service.update(1L, 7L, "medications", dosage("1000mg"));

        assertEquals("1000mg", m.getDosage());
    }

    @Test
    void anotherUserCannotUpdateAndNothingIsSaved() {
        when(medications.findById(1L)).thenReturn(Optional.of(medicationOf(7L)));

        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> service.update(1L, 8L, "medications", dosage("HACKED"))));
        verify(medications, never()).save(any());
    }

    @Test
    void anotherUserCannotDelete() {
        when(medications.findById(1L)).thenReturn(Optional.of(medicationOf(7L)));

        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> service.delete(1L, 8L, "medications")));
        verify(medications, never()).delete(any());
        verify(medications, never()).deleteById(any());
    }

    @Test
    void ownerCanDelete() {
        MedicalMedication m = medicationOf(7L);
        when(medications.findById(1L)).thenReturn(Optional.of(m));

        service.delete(1L, 7L, "medications");

        verify(medications).delete(m);
    }

    @Test
    void missingAndForeignRecordsLookTheSame() {
        when(medications.findById(404L)).thenReturn(Optional.empty());
        when(medications.findById(1L)).thenReturn(Optional.of(medicationOf(7L)));

        assertEquals(statusOf(() -> service.delete(404L, 8L, "medications")),
                statusOf(() -> service.delete(1L, 8L, "medications")));
    }

    @Test
    void theRuleAppliesToEveryCategoryNotJustMedications() {
        MedicalAllergy a = new MedicalAllergy();
        a.setUserId(7L);
        when(allergies.findById(5L)).thenReturn(Optional.of(a));

        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> service.delete(5L, 8L, "allergies")));
        verify(allergies, never()).delete(any());
    }
}
