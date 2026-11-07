package com.diwan.medical.entity;

import com.diwan.common.crypto.EncryptedStringConverter;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "medical_medications")
public class MedicalMedication extends MedicalEntry {

    @Convert(converter = EncryptedStringConverter.class) @Column(columnDefinition = "TEXT", nullable = false) private String name;
    @Convert(converter = EncryptedStringConverter.class) @Column(columnDefinition = "TEXT") private String dosage;
    @Convert(converter = EncryptedStringConverter.class) @Column(columnDefinition = "TEXT") private String frequency;
    private LocalDate startDate;
    private LocalDate endDate;

    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getDosage() { return dosage; }
    public void setDosage(String v) { dosage = v; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String v) { frequency = v; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate v) { startDate = v; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate v) { endDate = v; }
}
