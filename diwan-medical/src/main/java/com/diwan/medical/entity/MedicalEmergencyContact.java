package com.diwan.medical.entity;

import com.diwan.common.crypto.EncryptedStringConverter;
import jakarta.persistence.*;

@Entity
@Table(name = "medical_emergency_contacts")
public class MedicalEmergencyContact extends MedicalEntry {

    @Convert(converter = EncryptedStringConverter.class) @Column(columnDefinition = "TEXT", nullable = false) private String contactName;
    @Convert(converter = EncryptedStringConverter.class) @Column(columnDefinition = "TEXT", nullable = false) private String phone;
    @Convert(converter = EncryptedStringConverter.class) @Column(columnDefinition = "TEXT") private String contactRelation;

    public String getContactName() { return contactName; }
    public void setContactName(String v) { contactName = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { phone = v; }
    public String getContactRelation() { return contactRelation; }
    public void setContactRelation(String v) { contactRelation = v; }
}
