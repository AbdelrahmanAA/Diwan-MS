package com.diwan.portfolio.service;

import com.diwan.common.web.ApiException;
import com.diwan.portfolio.entity.Tag;
import com.diwan.portfolio.repository.TagRepository;
import com.diwan.portfolio.web.dto.Requests.TagRequest;
import com.diwan.portfolio.web.dto.Responses.TagResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Tag names are encrypted, so uniqueness is checked here (a user has few tags) instead of by an index. */
@Service
public class TagService {

    static final int MAX_TAGS = 100;

    private final TagRepository tags;
    private final Views views;

    public TagService(TagRepository tags, Views views) {
        this.tags = tags;
        this.views = views;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> list(Long userId) {
        return tags.findByUserIdOrderByIdAsc(userId).stream().map(views::tag).toList();
    }

    @Transactional
    public TagResponse create(Long userId, TagRequest req) {
        List<Tag> existing = tags.findByUserIdOrderByIdAsc(userId);
        String name = req.name().trim();
        if (existing.size() >= MAX_TAGS) {
            throw ApiException.conflict("tag.limit_reached", MAX_TAGS);
        }
        if (existing.stream().anyMatch(t -> t.getName().equalsIgnoreCase(name))) {
            throw ApiException.conflict("tag.duplicate", name);
        }
        Tag t = new Tag();
        t.setUserId(userId);
        t.setName(name);
        t.setColor(req.color());
        return views.tag(tags.save(t));
    }

    @Transactional
    public void delete(Long userId, Long tagId) {
        tags.delete(tags.findByIdAndUserId(tagId, userId).orElseThrow(() -> ApiException.notFound("tag.not_found")));
    }
}
