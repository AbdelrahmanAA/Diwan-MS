package com.diwan.portfolio.web;

import com.diwan.portfolio.domain.AllocationDimension;
import com.diwan.portfolio.service.AllocationService;
import com.diwan.portfolio.service.GoalService;
import com.diwan.portfolio.service.HoldingService;
import com.diwan.portfolio.service.PortfolioService;
import com.diwan.portfolio.service.SummaryService;
import com.diwan.portfolio.web.dto.Requests.GoalRequest;
import com.diwan.portfolio.web.dto.Requests.HoldingRequest;
import com.diwan.portfolio.web.dto.Requests.PortfolioRequest;
import com.diwan.portfolio.web.dto.Requests.TargetsRequest;
import com.diwan.portfolio.web.dto.Responses.AllocationResponse;
import com.diwan.portfolio.web.dto.Responses.GoalResponse;
import com.diwan.portfolio.web.dto.Responses.HoldingResponse;
import com.diwan.portfolio.web.dto.Responses.PortfolioOverview;
import com.diwan.portfolio.web.dto.Responses.PortfolioResponse;
import com.diwan.portfolio.web.dto.Responses.SummaryResponse;
import com.diwan.portfolio.web.dto.Responses.TargetResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Portfolios and everything scoped to one portfolio. The user is the gateway-signed X-User-Id. */
@RestController
@RequestMapping("/api/portfolio/portfolios")
public class PortfolioController {

    private final PortfolioService portfolios;
    private final SummaryService summaries;
    private final HoldingService holdings;
    private final AllocationService allocation;
    private final GoalService goals;

    public PortfolioController(PortfolioService portfolios, SummaryService summaries, HoldingService holdings,
                               AllocationService allocation, GoalService goals) {
        this.portfolios = portfolios;
        this.summaries = summaries;
        this.holdings = holdings;
        this.allocation = allocation;
        this.goals = goals;
    }

    /** All portfolios with their headline numbers (the app's home screen). */
    @GetMapping
    public List<PortfolioOverview> list(@RequestHeader("X-User-Id") Long userId) {
        return summaries.overviews(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioResponse create(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody PortfolioRequest req) {
        return portfolios.create(userId, req);
    }

    @GetMapping("/{id}")
    public PortfolioResponse get(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        return portfolios.get(userId, id);
    }

    @PutMapping("/{id}")
    public PortfolioResponse update(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id,
                                    @Valid @RequestBody PortfolioRequest req) {
        return portfolios.update(userId, id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        portfolios.delete(userId, id);
    }

    /**
     * Grouped view. groupBy: up to 3 of COUNTRY, ASSET_TYPE, CURRENCY (default COUNTRY,ASSET_TYPE);
     * tagId: only holdings with this tag.
     */
    @GetMapping("/{id}/summary")
    public SummaryResponse summary(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id,
                                   @RequestParam(required = false) List<AllocationDimension> groupBy,
                                   @RequestParam(required = false) Long tagId) {
        return summaries.summary(userId, id, groupBy, tagId);
    }

    @GetMapping("/{id}/holdings")
    public List<HoldingResponse> holdings(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id,
                                          @RequestParam(required = false) Long tagId,
                                          @RequestParam(defaultValue = "false") boolean includeClosed) {
        return summaries.holdingViews(userId, id, tagId, includeClosed);
    }

    /** Add an investment; initialTransaction (e.g. the first BUY) is optional. */
    @PostMapping("/{id}/holdings")
    @ResponseStatus(HttpStatus.CREATED)
    public HoldingResponse addHolding(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id,
                                      @Valid @RequestBody HoldingRequest req) {
        return holdings.create(userId, id, req);
    }

    @GetMapping("/{id}/allocation")
    public AllocationResponse allocation(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id,
                                         @RequestParam(defaultValue = "ASSET_TYPE") AllocationDimension dimension) {
        return allocation.allocation(userId, id, dimension);
    }

    @GetMapping("/{id}/targets")
    public List<TargetResponse> targets(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        return allocation.getTargets(userId, id);
    }

    /** Replaces the targets of one dimension (they must add up to 100; an empty list clears them). */
    @PutMapping("/{id}/targets")
    public List<TargetResponse> setTargets(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id,
                                           @Valid @RequestBody TargetsRequest req) {
        return allocation.setTargets(userId, id, req);
    }

    @GetMapping("/{id}/goals")
    public List<GoalResponse> goals(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        return goals.list(userId, id);
    }

    @PostMapping("/{id}/goals")
    @ResponseStatus(HttpStatus.CREATED)
    public GoalResponse addGoal(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id,
                                @Valid @RequestBody GoalRequest req) {
        return goals.create(userId, id, req);
    }
}
