package com.diwan.portfolio.web;

import com.diwan.portfolio.service.PreferenceService;
import com.diwan.portfolio.service.ReferenceService;
import com.diwan.portfolio.service.TagService;
import com.diwan.portfolio.web.dto.Requests.PreferencesRequest;
import com.diwan.portfolio.web.dto.Requests.TagRequest;
import com.diwan.portfolio.web.dto.Responses.PreferencesResponse;
import com.diwan.portfolio.web.dto.Responses.ReferenceResponse;
import com.diwan.portfolio.web.dto.Responses.TagResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Per-user settings, tags, and reference data (labels in the request language). */
@RestController
@RequestMapping("/api/portfolio")
public class AccountController {

    private final PreferenceService preferences;
    private final TagService tags;
    private final ReferenceService reference;

    public AccountController(PreferenceService preferences, TagService tags, ReferenceService reference) {
        this.preferences = preferences;
        this.tags = tags;
        this.reference = reference;
    }

    @GetMapping("/reference")
    public ReferenceResponse reference() {
        return reference.reference();
    }

    @GetMapping("/preferences")
    public PreferencesResponse preferences(@RequestHeader("X-User-Id") Long userId) {
        return preferences.get(userId);
    }

    @PutMapping("/preferences")
    public PreferencesResponse updatePreferences(@RequestHeader("X-User-Id") Long userId,
                                                 @Valid @RequestBody PreferencesRequest req) {
        return preferences.update(userId, req);
    }

    @GetMapping("/tags")
    public List<TagResponse> tags(@RequestHeader("X-User-Id") Long userId) {
        return tags.list(userId);
    }

    @PostMapping("/tags")
    @ResponseStatus(HttpStatus.CREATED)
    public TagResponse createTag(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody TagRequest req) {
        return tags.create(userId, req);
    }

    @DeleteMapping("/tags/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTag(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        tags.delete(userId, id);
    }
}
