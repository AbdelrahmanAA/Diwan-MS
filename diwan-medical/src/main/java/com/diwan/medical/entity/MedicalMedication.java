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
    /** Dose times of the day, "HH:mm" separated by commas, earliest first; null without a schedule */
    @Column(length = 100) private String scheduleTimes;
    /** Remind on the phone at the dose times */
    @Column(nullable = false) private boolean reminders;

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
    public String getScheduleTimes() { return scheduleTimes; }
    public void setScheduleTimes(String v) { scheduleTimes = v; }
    public boolean isReminders() { return reminders; }
    public void setReminders(boolean v) { reminders = v; }
}
