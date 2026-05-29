package com.voicetransactions.service;

import com.voicetransactions.dto.MedicalRecordRequest;
import com.voicetransactions.dto.MedicalRecordResponse;
import com.voicetransactions.entity.*;
import com.voicetransactions.repository.*;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class MedicalRecordService {

    private final MedicalMedicationRepository medications;
    private final MedicalAllergyRepository allergies;
    private final MedicalBloodTypeRepository bloodTypes;
    private final MedicalSurgeryRepository surgeries;
    private final MedicalChronicDiseaseRepository chronicDiseases;
    private final MedicalLabResultRepository labResults;
    private final MedicalVaccineRepository vaccines;
    private final MedicalFamilyHistoryRepository familyHistory;

    public MedicalRecordService(
            MedicalMedicationRepository medications, MedicalAllergyRepository allergies,
            MedicalBloodTypeRepository bloodTypes, MedicalSurgeryRepository surgeries,
            MedicalChronicDiseaseRepository chronicDiseases, MedicalLabResultRepository labResults,
            MedicalVaccineRepository vaccines, MedicalFamilyHistoryRepository familyHistory) {
        this.medications=medications; this.allergies=allergies; this.bloodTypes=bloodTypes;
        this.surgeries=surgeries; this.chronicDiseases=chronicDiseases; this.labResults=labResults;
        this.vaccines=vaccines; this.familyHistory=familyHistory;
    }

    public MedicalRecordResponse create(MedicalRecordRequest req) {
        String cat = req.getCategory();
        if ("medications".equals(cat)) {
            MedicalMedication e = new MedicalMedication();
            e.setUserId(req.getUserId()); e.setName(req.getName()); e.setDosage(req.getDosage());
            e.setFrequency(req.getFrequency()); e.setStartDate(req.getStartDate()); e.setEndDate(req.getEndDate());
            e.setNotes(req.getNotes()); e.setRecordedAt(req.getRecordedAt());
            return toResponse(medications.save(e));
        } else if ("allergies".equals(cat)) {
            MedicalAllergy e = new MedicalAllergy();
            e.setUserId(req.getUserId()); e.setAllergen(req.getAllergen()); e.setSeverity(req.getSeverity());
            e.setReaction(req.getReaction()); e.setNotes(req.getNotes()); e.setRecordedAt(req.getRecordedAt());
            return toResponse(allergies.save(e));
        } else if ("bloodType".equals(cat)) {
            MedicalBloodType e = new MedicalBloodType();
            e.setUserId(req.getUserId()); e.setBloodType(req.getBloodType());
            e.setNotes(req.getNotes()); e.setRecordedAt(req.getRecordedAt());
            return toResponse(bloodTypes.save(e));
        } else if ("surgeries".equals(cat)) {
            MedicalSurgery e = new MedicalSurgery();
            e.setUserId(req.getUserId()); e.setSurgeryName(req.getSurgeryName()); e.setSurgeryDate(req.getSurgeryDate());
            e.setHospital(req.getHospital()); e.setSurgeon(req.getSurgeon());
            e.setNotes(req.getNotes()); e.setRecordedAt(req.getRecordedAt());
            return toResponse(surgeries.save(e));
        } else if ("chronicDiseases".equals(cat)) {
            MedicalChronicDisease e = new MedicalChronicDisease();
            e.setUserId(req.getUserId()); e.setDiseaseName(req.getDiseaseName()); e.setDiagnosedAt(req.getDiagnosedAt());
            e.setCurrentTreatment(req.getCurrentTreatment()); e.setNotes(req.getNotes()); e.setRecordedAt(req.getRecordedAt());
            return toResponse(chronicDiseases.save(e));
        } else if ("labResults".equals(cat)) {
            MedicalLabResult e = new MedicalLabResult();
            e.setUserId(req.getUserId()); e.setTestName(req.getTestName()); e.setResult(req.getResult());
            e.setUnit(req.getUnit()); e.setReferenceRange(req.getReferenceRange()); e.setTestDate(req.getTestDate());
            e.setNotes(req.getNotes()); e.setRecordedAt(req.getRecordedAt());
            return toResponse(labResults.save(e));
        } else if ("vaccines".equals(cat)) {
            MedicalVaccine e = new MedicalVaccine();
            e.setUserId(req.getUserId()); e.setVaccineName(req.getVaccineName()); e.setDoseNumber(req.getDoseNumber());
            e.setVaccinationDate(req.getVaccinationDate()); e.setNextDoseDate(req.getNextDoseDate());
            e.setNotes(req.getNotes()); e.setRecordedAt(req.getRecordedAt());
            return toResponse(vaccines.save(e));
        } else if ("familyHistory".equals(cat)) {
            MedicalFamilyHistory e = new MedicalFamilyHistory();
            e.setUserId(req.getUserId()); e.setCondition(req.getCondition()); e.setRelation(req.getRelation());
            e.setNotes(req.getNotes()); e.setRecordedAt(req.getRecordedAt());
            return toResponse(familyHistory.save(e));
        }
        throw new IllegalArgumentException("Unknown category: " + cat);
    }

    public List<MedicalRecordResponse> getByUserAndCategory(Long userId, String category) {
        List<MedicalRecordResponse> list = new ArrayList<>();
        if (category == null) {
            medications.findByUserId(userId).forEach(e -> list.add(toResponse(e)));
            allergies.findByUserId(userId).forEach(e -> list.add(toResponse(e)));
            bloodTypes.findByUserId(userId).forEach(e -> list.add(toResponse(e)));
            surgeries.findByUserId(userId).forEach(e -> list.add(toResponse(e)));
            chronicDiseases.findByUserId(userId).forEach(e -> list.add(toResponse(e)));
            labResults.findByUserId(userId).forEach(e -> list.add(toResponse(e)));
            vaccines.findByUserId(userId).forEach(e -> list.add(toResponse(e)));
            familyHistory.findByUserId(userId).forEach(e -> list.add(toResponse(e)));
        } else {
            switch (category) {
                case "medications": medications.findByUserId(userId).forEach(e -> list.add(toResponse(e))); break;
                case "allergies": allergies.findByUserId(userId).forEach(e -> list.add(toResponse(e))); break;
                case "bloodType": bloodTypes.findByUserId(userId).forEach(e -> list.add(toResponse(e))); break;
                case "surgeries": surgeries.findByUserId(userId).forEach(e -> list.add(toResponse(e))); break;
                case "chronicDiseases": chronicDiseases.findByUserId(userId).forEach(e -> list.add(toResponse(e))); break;
                case "labResults": labResults.findByUserId(userId).forEach(e -> list.add(toResponse(e))); break;
                case "vaccines": vaccines.findByUserId(userId).forEach(e -> list.add(toResponse(e))); break;
                case "familyHistory": familyHistory.findByUserId(userId).forEach(e -> list.add(toResponse(e))); break;
            }
        }
        return list;
    }

    public MedicalRecordResponse update(Long id, String category, MedicalRecordRequest req) {
        if ("medications".equals(category)) {
            MedicalMedication e = medications.findById(id).orElseThrow();
            if(req.getName()!=null) e.setName(req.getName()); if(req.getDosage()!=null) e.setDosage(req.getDosage());
            if(req.getFrequency()!=null) e.setFrequency(req.getFrequency()); if(req.getStartDate()!=null) e.setStartDate(req.getStartDate());
            if(req.getEndDate()!=null) e.setEndDate(req.getEndDate()); if(req.getNotes()!=null) e.setNotes(req.getNotes());
            return toResponse(medications.save(e));
        } else if ("allergies".equals(category)) {
            MedicalAllergy e = allergies.findById(id).orElseThrow();
            if(req.getAllergen()!=null) e.setAllergen(req.getAllergen()); if(req.getSeverity()!=null) e.setSeverity(req.getSeverity());
            if(req.getReaction()!=null) e.setReaction(req.getReaction()); if(req.getNotes()!=null) e.setNotes(req.getNotes());
            return toResponse(allergies.save(e));
        } else if ("bloodType".equals(category)) {
            MedicalBloodType e = bloodTypes.findById(id).orElseThrow();
            if(req.getBloodType()!=null) e.setBloodType(req.getBloodType()); if(req.getNotes()!=null) e.setNotes(req.getNotes());
            return toResponse(bloodTypes.save(e));
        } else if ("surgeries".equals(category)) {
            MedicalSurgery e = surgeries.findById(id).orElseThrow();
            if(req.getSurgeryName()!=null) e.setSurgeryName(req.getSurgeryName()); if(req.getSurgeryDate()!=null) e.setSurgeryDate(req.getSurgeryDate());
            if(req.getHospital()!=null) e.setHospital(req.getHospital()); if(req.getSurgeon()!=null) e.setSurgeon(req.getSurgeon());
            if(req.getNotes()!=null) e.setNotes(req.getNotes());
            return toResponse(surgeries.save(e));
        } else if ("chronicDiseases".equals(category)) {
            MedicalChronicDisease e = chronicDiseases.findById(id).orElseThrow();
            if(req.getDiseaseName()!=null) e.setDiseaseName(req.getDiseaseName()); if(req.getDiagnosedAt()!=null) e.setDiagnosedAt(req.getDiagnosedAt());
            if(req.getCurrentTreatment()!=null) e.setCurrentTreatment(req.getCurrentTreatment()); if(req.getNotes()!=null) e.setNotes(req.getNotes());
            return toResponse(chronicDiseases.save(e));
        } else if ("labResults".equals(category)) {
            MedicalLabResult e = labResults.findById(id).orElseThrow();
            if(req.getTestName()!=null) e.setTestName(req.getTestName()); if(req.getResult()!=null) e.setResult(req.getResult());
            if(req.getUnit()!=null) e.setUnit(req.getUnit()); if(req.getReferenceRange()!=null) e.setReferenceRange(req.getReferenceRange());
            if(req.getTestDate()!=null) e.setTestDate(req.getTestDate()); if(req.getNotes()!=null) e.setNotes(req.getNotes());
            return toResponse(labResults.save(e));
        } else if ("vaccines".equals(category)) {
            MedicalVaccine e = vaccines.findById(id).orElseThrow();
            if(req.getVaccineName()!=null) e.setVaccineName(req.getVaccineName()); if(req.getDoseNumber()!=null) e.setDoseNumber(req.getDoseNumber());
            if(req.getVaccinationDate()!=null) e.setVaccinationDate(req.getVaccinationDate()); if(req.getNextDoseDate()!=null) e.setNextDoseDate(req.getNextDoseDate());
            if(req.getNotes()!=null) e.setNotes(req.getNotes());
            return toResponse(vaccines.save(e));
        } else if ("familyHistory".equals(category)) {
            MedicalFamilyHistory e = familyHistory.findById(id).orElseThrow();
            if(req.getCondition()!=null) e.setCondition(req.getCondition()); if(req.getRelation()!=null) e.setRelation(req.getRelation());
            if(req.getNotes()!=null) e.setNotes(req.getNotes());
            return toResponse(familyHistory.save(e));
        }
        throw new IllegalArgumentException("Unknown category: " + category);
    }

    public void delete(Long id, String category) {
        switch (category) {
            case "medications": medications.deleteById(id); break;
            case "allergies": allergies.deleteById(id); break;
            case "bloodType": bloodTypes.deleteById(id); break;
            case "surgeries": surgeries.deleteById(id); break;
            case "chronicDiseases": chronicDiseases.deleteById(id); break;
            case "labResults": labResults.deleteById(id); break;
            case "vaccines": vaccines.deleteById(id); break;
            case "familyHistory": familyHistory.deleteById(id); break;
            default: throw new IllegalArgumentException("Unknown category: " + category);
        }
    }

    // ── toResponse overloads ──────────────────────────────────────────────────
    private MedicalRecordResponse toResponse(MedicalMedication e) {
        MedicalRecordResponse r = base(e.getId(), e.getUserId(), "medications", e.getNotes(), e.getRecordedAt(), e.getCreatedAt(), e.getUpdatedAt());
        r.setName(e.getName()); r.setDosage(e.getDosage()); r.setFrequency(e.getFrequency());
        r.setStartDate(e.getStartDate()); r.setEndDate(e.getEndDate()); return r;
    }
    private MedicalRecordResponse toResponse(MedicalAllergy e) {
        MedicalRecordResponse r = base(e.getId(), e.getUserId(), "allergies", e.getNotes(), e.getRecordedAt(), e.getCreatedAt(), e.getUpdatedAt());
        r.setAllergen(e.getAllergen()); r.setSeverity(e.getSeverity()); r.setReaction(e.getReaction()); return r;
    }
    private MedicalRecordResponse toResponse(MedicalBloodType e) {
        MedicalRecordResponse r = base(e.getId(), e.getUserId(), "bloodType", e.getNotes(), e.getRecordedAt(), e.getCreatedAt(), e.getUpdatedAt());
        r.setBloodType(e.getBloodType()); return r;
    }
    private MedicalRecordResponse toResponse(MedicalSurgery e) {
        MedicalRecordResponse r = base(e.getId(), e.getUserId(), "surgeries", e.getNotes(), e.getRecordedAt(), e.getCreatedAt(), e.getUpdatedAt());
        r.setSurgeryName(e.getSurgeryName()); r.setSurgeryDate(e.getSurgeryDate()); r.setHospital(e.getHospital()); r.setSurgeon(e.getSurgeon()); return r;
    }
    private MedicalRecordResponse toResponse(MedicalChronicDisease e) {
        MedicalRecordResponse r = base(e.getId(), e.getUserId(), "chronicDiseases", e.getNotes(), e.getRecordedAt(), e.getCreatedAt(), e.getUpdatedAt());
        r.setDiseaseName(e.getDiseaseName()); r.setDiagnosedAt(e.getDiagnosedAt()); r.setCurrentTreatment(e.getCurrentTreatment()); return r;
    }
    private MedicalRecordResponse toResponse(MedicalLabResult e) {
        MedicalRecordResponse r = base(e.getId(), e.getUserId(), "labResults", e.getNotes(), e.getRecordedAt(), e.getCreatedAt(), e.getUpdatedAt());
        r.setTestName(e.getTestName()); r.setResult(e.getResult()); r.setUnit(e.getUnit()); r.setReferenceRange(e.getReferenceRange()); r.setTestDate(e.getTestDate()); return r;
    }
    private MedicalRecordResponse toResponse(MedicalVaccine e) {
        MedicalRecordResponse r = base(e.getId(), e.getUserId(), "vaccines", e.getNotes(), e.getRecordedAt(), e.getCreatedAt(), e.getUpdatedAt());
        r.setVaccineName(e.getVaccineName()); r.setDoseNumber(e.getDoseNumber()); r.setVaccinationDate(e.getVaccinationDate()); r.setNextDoseDate(e.getNextDoseDate()); return r;
    }
    private MedicalRecordResponse toResponse(MedicalFamilyHistory e) {
        MedicalRecordResponse r = base(e.getId(), e.getUserId(), "familyHistory", e.getNotes(), e.getRecordedAt(), e.getCreatedAt(), e.getUpdatedAt());
        r.setCondition(e.getCondition()); r.setRelation(e.getRelation()); return r;
    }
    private MedicalRecordResponse base(Long id, Long userId, String cat, String notes, java.time.LocalDateTime recordedAt, java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {
        MedicalRecordResponse r = new MedicalRecordResponse();
        r.setId(id); r.setUserId(userId); r.setCategory(cat); r.setNotes(notes);
        r.setRecordedAt(recordedAt); r.setCreatedAt(createdAt); r.setUpdatedAt(updatedAt);
        return r;
    }
}