package com.diwan.portfolio.web;

import com.diwan.portfolio.service.GoalService;
import com.diwan.portfolio.service.HoldingService;
import com.diwan.portfolio.service.ManualValuationService;
import com.diwan.portfolio.service.TransactionService;
import com.diwan.portfolio.web.dto.Requests.GoalRequest;
import com.diwan.portfolio.web.dto.Requests.HoldingRequest;
import com.diwan.portfolio.web.dto.Requests.HoldingTagsRequest;
import com.diwan.portfolio.web.dto.Requests.TransactionRequest;
import com.diwan.portfolio.web.dto.Requests.ValuationRequest;
import com.diwan.portfolio.web.dto.Responses.GoalResponse;
import com.diwan.portfolio.web.dto.Responses.HoldingResponse;
import com.diwan.portfolio.web.dto.Responses.TransactionResponse;
import com.diwan.portfolio.web.dto.Responses.ValuationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Holdings, their transactions and valuations, and goals - addressed by their own id. */
@RestController
@RequestMapping("/api/portfolio")
public class HoldingController {

    private final HoldingService holdings;
    private final TransactionService transactions;
    private final ManualValuationService valuations;
    private final GoalService goals;

    public HoldingController(HoldingService holdings, TransactionService transactions,
                             ManualValuationService valuations, GoalService goals) {
        this.holdings = holdings;
        this.transactions = transactions;
        this.valuations = valuations;
        this.goals = goals;
    }

    @GetMapping("/holdings/{id}")
    public HoldingResponse get(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        return holdings.get(userId, id);
    }

    @PutMapping("/holdings/{id}")
    public HoldingResponse update(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id,
                                  @Valid @RequestBody HoldingRequest req) {
        return holdings.update(userId, id, req);
    }

    @DeleteMapping("/holdings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        holdings.delete(userId, id);
    }

    /** Archive (closed=true) hides the holding from summaries but keeps its history. */
    @PostMapping("/holdings/{id}/close")
    public HoldingResponse close(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        return holdings.setClosed(userId, id, true);
    }

    @PostMapping("/holdings/{id}/reopen")
    public HoldingResponse reopen(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        return holdings.setClosed(userId, id, false);
    }

    @PutMapping("/holdings/{id}/tags")
    public HoldingResponse setTags(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id,
                                   @Valid @RequestBody HoldingTagsRequest req) {
        return holdings.setTags(userId, id, req.tagIds());
    }

    @GetMapping("/holdings/{id}/transactions")
    public List<TransactionResponse> transactions(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        return transactions.list(userId, id);
    }

    @PostMapping("/holdings/{id}/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse addTransaction(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id,
                                              @Valid @RequestBody TransactionRequest req) {
        return transactions.add(userId, id, req);
    }

    @PutMapping("/transactions/{id}")
    public TransactionResponse updateTransaction(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id,
                                                 @Valid @RequestBody TransactionRequest req) {
        return transactions.update(userId, id, req);
    }

    @DeleteMapping("/transactions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransaction(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        transactions.delete(userId, id);
    }

    @GetMapping("/holdings/{id}/valuations")
    public List<ValuationResponse> valuations(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        return valuations.list(userId, id);
    }

    @PostMapping("/holdings/{id}/valuations")
    @ResponseStatus(HttpStatus.CREATED)
    public ValuationResponse addValuation(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id,
                                          @Valid @RequestBody ValuationRequest req) {
        return valuations.add(userId, id, req);
    }

    @DeleteMapping("/valuations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteValuation(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        valuations.delete(userId, id);
    }

    @PutMapping("/goals/{id}")
    public GoalResponse updateGoal(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id,
                                   @Valid @RequestBody GoalRequest req) {
        return goals.update(userId, id, req);
    }

    @DeleteMapping("/goals/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGoal(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        goals.delete(userId, id);
    }
}
