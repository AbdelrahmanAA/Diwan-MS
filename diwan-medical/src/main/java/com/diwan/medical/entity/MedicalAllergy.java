package com.diwan.medical.entity;

import com.diwan.common.crypto.EncryptedStringConverter;
import jakarta.persistence.*;

@Entity
@Table(name = "medical_allergies")
public class MedicalAllergy extends MedicalEntry {

    @Convert(converter = EncryptedStringConverter.class) @Column(columnDefinition = "TEXT", nullable = false) private String allergen;
    private String severity;
    @Convert(converter = EncryptedStringConverter.class) @Column(columnDefinition = "TEXT") private String reaction;

    public String getAllergen() { return allergen; }
    public void setAllergen(String v) { allergen = v; }
    public String getSeverity() { return severity; }
    public void setSeverity(String v) { severity = v; }
    public String getReaction() { return reaction; }
    public void setReaction(String v) { reaction = v; }
}
