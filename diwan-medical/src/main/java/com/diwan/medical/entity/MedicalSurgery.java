package com.diwan.medical.entity;

import com.diwan.common.crypto.EncryptedStringConverter;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "medical_surgeries")
public class MedicalSurgery extends MedicalEntry {

    @Convert(converter = EncryptedStringConverter.class) @Column(columnDefinition = "TEXT", nullable = false) private String surgeryName;
    private LocalDate surgeryDate;
    @Convert(converter = EncryptedStringConverter.class) @Column(columnDefinition = "TEXT") private String hospital;
    @Convert(converter = EncryptedStringConverter.class) @Column(columnDefinition = "TEXT") private String surgeon;

    public String getSurgeryName() { return surgeryName; }
    public void setSurgeryName(String v) { surgeryName = v; }
    public LocalDate getSurgeryDate() { return surgeryDate; }
    public void setSurgeryDate(LocalDate v) { surgeryDate = v; }
    public String getHospital() { return hospital; }
    public void setHospital(String v) { hospital = v; }
    public String getSurgeon() { return surgeon; }
    public void setSurgeon(String v) { surgeon = v; }
}
