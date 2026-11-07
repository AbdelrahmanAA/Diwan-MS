package com.diwan.medical.entity;

import com.diwan.common.crypto.EncryptedStringConverter;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "medical_lab_results")
public class MedicalLabResult extends MedicalEntry {

    @Convert(converter = EncryptedStringConverter.class) @Column(columnDefinition = "TEXT", nullable = false) private String testName;
    @Convert(converter = EncryptedStringConverter.class) @Column(columnDefinition = "TEXT") private String result;
    @Convert(converter = EncryptedStringConverter.class) @Column(columnDefinition = "TEXT") private String unit;
    @Convert(converter = EncryptedStringConverter.class) @Column(columnDefinition = "TEXT") private String referenceRange;
    private LocalDate testDate;

    public String getTestName() { return testName; }
    public void setTestName(String v) { testName = v; }
    public String getResult() { return result; }
    public void setResult(String v) { result = v; }
    public String getUnit() { return unit; }
    public void setUnit(String v) { unit = v; }
    public String getReferenceRange() { return referenceRange; }
    public void setReferenceRange(String v) { referenceRange = v; }
    public LocalDate getTestDate() { return testDate; }
    public void setTestDate(LocalDate v) { testDate = v; }
}
