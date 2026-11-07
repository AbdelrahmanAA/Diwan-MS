package com.diwan.medical.entity;

import com.diwan.common.crypto.EncryptedStringConverter;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "medical_vaccines")
public class MedicalVaccine extends MedicalEntry {

    @Convert(converter = EncryptedStringConverter.class) @Column(columnDefinition = "TEXT", nullable = false) private String vaccineName;
    private Integer doseNumber;
    private LocalDate vaccinationDate;
    private LocalDate nextDoseDate;

    public String getVaccineName() { return vaccineName; }
    public void setVaccineName(String v) { vaccineName = v; }
    public Integer getDoseNumber() { return doseNumber; }
    public void setDoseNumber(Integer v) { doseNumber = v; }
    public LocalDate getVaccinationDate() { return vaccinationDate; }
    public void setVaccinationDate(LocalDate v) { vaccinationDate = v; }
    public LocalDate getNextDoseDate() { return nextDoseDate; }
    public void setNextDoseDate(LocalDate v) { nextDoseDate = v; }
}
