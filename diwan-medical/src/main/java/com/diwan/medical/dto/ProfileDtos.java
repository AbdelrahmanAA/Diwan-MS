package com.diwan.medical.dto;

import com.diwan.medical.domain.ProfileRelation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class ProfileDtos {

    private ProfileDtos() {}

    public record ProfileRequest(
            @NotBlank(message = "{validation.required}") @Size(max = 120, message = "{validation.size}") String name,
            @NotNull(message = "{validation.required}") ProfileRelation relation,
            @PastOrPresent(message = "{medical.birth_date.future}") LocalDate birthDate) {}

    /** @param name null for the account holder until they give one */
    public record ProfileResponse(Long id, String name, ProfileRelation relation, LocalDate birthDate, boolean self) {}
}
