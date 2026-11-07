package com.diwan.medical.entity;

import com.diwan.common.crypto.EncryptedStringConverter;
import jakarta.persistence.*;

@Entity
@Table(name = "medical_blood_type")
public class MedicalBloodType extends MedicalEntry {

    @Convert(converter = EncryptedStringConverter.class) @Column(columnDefinition = "TEXT", nullable = false) private String bloodType;

    public String getBloodType() { return bloodType; }
    public void setBloodType(String v) { bloodType = v; }
}
