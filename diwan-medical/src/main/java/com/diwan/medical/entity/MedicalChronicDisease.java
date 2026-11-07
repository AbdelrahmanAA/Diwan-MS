package com.diwan.medical.entity;

import com.diwan.common.crypto.EncryptedStringConverter;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "medical_chronic_diseases")
public class MedicalChronicDisease extends MedicalEntry {

    @Convert(converter = EncryptedStringConverter.class) @Column(columnDefinition = "TEXT", nullable = false) private String diseaseName;
    private LocalDate diagnosedAt;
    @Convert(converter = EncryptedStringConverter.class) @Column(columnDefinition = "TEXT") private String currentTreatment;

    public String getDiseaseName() { return diseaseName; }
    public void setDiseaseName(String v) { diseaseName = v; }
    public LocalDate getDiagnosedAt() { return diagnosedAt; }
    public void setDiagnosedAt(LocalDate v) { diagnosedAt = v; }
    public String getCurrentTreatment() { return currentTreatment; }
    public void setCurrentTreatment(String v) { currentTreatment = v; }
}
