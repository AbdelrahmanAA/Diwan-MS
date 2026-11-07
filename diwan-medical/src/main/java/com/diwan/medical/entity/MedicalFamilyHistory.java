package com.diwan.medical.entity;

import com.diwan.common.crypto.EncryptedStringConverter;
import jakarta.persistence.*;

@Entity
@Table(name = "medical_family_history")
public class MedicalFamilyHistory extends MedicalEntry {

    @Convert(converter = EncryptedStringConverter.class) @Column(name = "condition_name", columnDefinition = "TEXT", nullable = false) private String condition;
    @Convert(converter = EncryptedStringConverter.class) @Column(columnDefinition = "TEXT") private String relation;

    public String getCondition() { return condition; }
    public void setCondition(String v) { condition = v; }
    public String getRelation() { return relation; }
    public void setRelation(String v) { relation = v; }
}
