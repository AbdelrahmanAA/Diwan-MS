package com.diwan.medical.controller;

import com.diwan.medical.dto.MedicalRecordRequest;
import com.diwan.medical.dto.MedicalRecordResponse;
import com.diwan.medical.dto.OverviewResponse;
import com.diwan.medical.dto.PageResponse;
import com.diwan.medical.dto.ProfileDtos.ProfileRequest;
import com.diwan.medical.dto.ProfileDtos.ProfileResponse;
import com.diwan.medical.service.MedicalRecordService;
import com.diwan.medical.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Without a profileId everything refers to the account holder. */
@RestController
@RequestMapping("/api/medical")
public class MedicalRecordController {

    private final MedicalRecordService records;
    private final ProfileService profiles;

    public MedicalRecordController(MedicalRecordService records, ProfileService profiles) {
        this.records = records;
        this.profiles = profiles;
    }

    @PostMapping
    public MedicalRecordResponse create(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody MedicalRecordRequest req) {
        return records.create(userId, req);
    }

    @GetMapping
    public List<MedicalRecordResponse> list(@RequestHeader("X-User-Id") Long userId,
                                            @RequestParam(required = false) Long profileId,
                                            @RequestParam(required = false) String category) {
        return records.list(userId, profileId, category);
    }

    @PutMapping("/{id:\\d+}")
    public MedicalRecordResponse update(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId,
                                        @Valid @RequestBody MedicalRecordRequest req) {
        return records.update(id, userId, req);
    }

    @DeleteMapping("/{id:\\d+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestParam String category, @RequestHeader("X-User-Id") Long userId) {
        records.delete(id, userId, category);
    }

    @GetMapping("/overview")
    public OverviewResponse overview(@RequestHeader("X-User-Id") Long userId,
                                     @RequestParam(required = false) Long profileId) {
        return records.overview(userId, profileId);
    }

    @GetMapping("/timeline")
    public PageResponse<MedicalRecordResponse> timeline(@RequestHeader("X-User-Id") Long userId,
                                                        @RequestParam(required = false) Long profileId,
                                                        @RequestParam(required = false) String category,
                                                        @RequestParam(required = false) String q,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "50") int size) {
        return records.timeline(userId, profileId, category, q, page, size);
    }

    @GetMapping("/profiles")
    public List<ProfileResponse> profiles(@RequestHeader("X-User-Id") Long userId) {
        return profiles.list(userId);
    }

    @PostMapping("/profiles")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse addProfile(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody ProfileRequest req) {
        return profiles.create(userId, req);
    }

    @PutMapping("/profiles/{id}")
    public ProfileResponse updateProfile(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id,
                                         @Valid @RequestBody ProfileRequest req) {
        return profiles.update(userId, id, req);
    }

    @DeleteMapping("/profiles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfile(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        profiles.delete(userId, id);
    }
}
