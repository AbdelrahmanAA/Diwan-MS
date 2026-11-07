package com.diwan.medical.service;

import com.diwan.common.web.ApiException;
import com.diwan.medical.domain.ProfileRelation;
import com.diwan.medical.dto.ProfileDtos.ProfileRequest;
import com.diwan.medical.dto.ProfileDtos.ProfileResponse;
import com.diwan.medical.entity.MedicalProfile;
import com.diwan.medical.repository.MedicalProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** The people an account keeps records for. The account holder's own profile exists from the first use. */
@Service
public class ProfileService {

    static final int MAX_PROFILES = 20;
    private static final String AUDIT_CATEGORY = "profile";

    private final MedicalProfileRepository profiles;
    private final AuditService audit;

    public ProfileService(MedicalProfileRepository profiles, AuditService audit) {
        this.profiles = profiles;
        this.audit = audit;
    }

    /** The given person of this account, or the account holder when no id is given. A foreign id is a 404. */
    @Transactional
    public MedicalProfile require(Long userId, Long profileId) {
        if (profileId == null) {
            return self(userId);
        }
        return profiles.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> ApiException.notFound("medical.profile.not_found"));
    }

    private MedicalProfile self(Long userId) {
        return profiles.findByUserIdAndRelation(userId, ProfileRelation.SELF).orElseGet(() -> {
            profiles.createSelf(userId);
            return profiles.findByUserIdAndRelation(userId, ProfileRelation.SELF).orElseThrow();
        });
    }

    @Transactional
    public List<ProfileResponse> list(Long userId) {
        self(userId);
        return profiles.findByUserIdOrderByIdAsc(userId).stream().map(ProfileService::view).toList();
    }

    @Transactional
    public ProfileResponse create(Long userId, ProfileRequest req) {
        if (req.relation() == ProfileRelation.SELF) {
            throw ApiException.badRequest("medical.profile.self_exists");
        }
        if (profiles.countByUserId(userId) >= MAX_PROFILES) {
            throw ApiException.conflict("medical.profile.limit_reached", MAX_PROFILES);
        }
        MedicalProfile p = new MedicalProfile();
        p.setUserId(userId);
        p.setRelation(req.relation());
        apply(p, req);
        profiles.save(p);
        audit.record(userId, p.getId(), "CREATE", AUDIT_CATEGORY, p.getId());
        return view(p);
    }

    @Transactional
    public ProfileResponse update(Long userId, Long id, ProfileRequest req) {
        MedicalProfile p = require(userId, id);
        boolean isSelf = p.getRelation() == ProfileRelation.SELF;
        if (isSelf != (req.relation() == ProfileRelation.SELF)) {
            throw ApiException.badRequest("medical.profile.self_fixed");
        }
        p.setRelation(req.relation());
        apply(p, req);
        audit.record(userId, p.getId(), "UPDATE", AUDIT_CATEGORY, p.getId());
        return view(p);
    }

    /** The database removes the person's records with the profile. */
    @Transactional
    public void delete(Long userId, Long id) {
        MedicalProfile p = require(userId, id);
        if (p.getRelation() == ProfileRelation.SELF) {
            throw ApiException.badRequest("medical.profile.self_fixed");
        }
        profiles.delete(p);
        audit.record(userId, id, "DELETE", AUDIT_CATEGORY, id);
    }

    private static void apply(MedicalProfile p, ProfileRequest req) {
        p.setName(req.name().trim());
        p.setBirthDate(req.birthDate());
    }

    static ProfileResponse view(MedicalProfile p) {
        return new ProfileResponse(p.getId(), p.getName(), p.getRelation(), p.getBirthDate(),
                p.getRelation() == ProfileRelation.SELF);
    }
}
