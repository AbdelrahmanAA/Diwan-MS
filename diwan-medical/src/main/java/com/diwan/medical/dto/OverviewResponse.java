package com.diwan.medical.dto;

import com.diwan.medical.dto.ProfileDtos.ProfileResponse;

import java.util.List;
import java.util.Map;

/** What matters about a person at a glance; also what the app keeps offline for the emergency card. */
public record OverviewResponse(ProfileResponse profile, String bloodType, List<MedicalRecordResponse> allergies,
                               List<MedicalRecordResponse> currentMedications, List<MedicalRecordResponse> conditions,
                               List<MedicalRecordResponse> emergencyContacts, List<MedicalRecordResponse> upcoming,
                               Map<String, Long> counts) {}
