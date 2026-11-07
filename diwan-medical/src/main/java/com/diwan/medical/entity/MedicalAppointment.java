package com.diwan.medical.entity;

import com.diwan.common.crypto.EncryptedStringConverter;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** A visit to a doctor, clinic or lab, at the phone's local time. */
@Entity
@Table(name = "medical_appointments")
public class MedicalAppointment extends MedicalEntry {

    @Convert(converter = EncryptedStringConverter.class) @Column(columnDefinition = "TEXT", nullable = false) private String appointmentWith;
    @Column(nullable = false) private LocalDateTime appointmentAt;
    @Convert(converter = EncryptedStringConverter.class) @Column(columnDefinition = "TEXT") private String location;

    public String getAppointmentWith() { return appointmentWith; }
    public void setAppointmentWith(String v) { appointmentWith = v; }
    public LocalDateTime getAppointmentAt() { return appointmentAt; }
    public void setAppointmentAt(LocalDateTime v) { appointmentAt = v; }
    public String getLocation() { return location; }
    public void setLocation(String v) { location = v; }
}
