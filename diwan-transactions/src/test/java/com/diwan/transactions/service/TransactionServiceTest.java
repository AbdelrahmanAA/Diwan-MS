package com.diwan.transactions.service;

import com.diwan.transactions.dto.TransactionRequest;
import com.diwan.transactions.dto.TransactionResponse;
import com.diwan.transactions.entity.Transaction;
import com.diwan.transactions.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionServiceTest {

    private final TransactionRepository repo = mock(TransactionRepository.class);
    private final TransactionService service = new TransactionService(repo);

    private static Transaction tx(Long userId, String type, String currency, String amount) {
        return Transaction.builder().userId(userId).type(type).currency(currency)
                .amount(new BigDecimal(amount)).category("food").build();
    }

    private static TransactionRequest request() {
        TransactionRequest r = new TransactionRequest();
        r.setType("expense");
        r.setCategory("FOOD");
        r.setAmount(new BigDecimal("12.50"));
        r.setCurrency("egp");
        return r;
    }

    @Test
    void saveBelongsToTheCallerAndNormalizesCategoryAndCurrency() {
        when(repo.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse res = service.save(request(), 7L);

        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(repo).save(saved.capture());
        assertEquals(7L, saved.getValue().getUserId());
        assertEquals("food", saved.getValue().getCategory());
        assertEquals("EGP", saved.getValue().getCurrency());
        assertEquals("Unknown", saved.getValue().getBank());
        assertEquals("EGP", res.getCurrency());
    }

    @Test
    void cannotUpdateAnotherUsersTransaction() {
        when(repo.findById(1L)).thenReturn(Optional.of(tx(8L, "expense", "EGP", "5")));

        assertThrows(RuntimeException.class, () -> service.update(1L, 7L, request()));
        verify(repo, never()).save(any());
    }

    @Test
    void cannotDeleteAnotherUsersTransaction() {
        when(repo.findById(1L)).thenReturn(Optional.of(tx(8L, "expense", "EGP", "5")));

        assertThrows(RuntimeException.class, () -> service.delete(1L, 7L));
        verify(repo, never()).delete(any());
    }

    @Test
    void ownerCanUpdateOnlyTheGivenFields() {
        Transaction existing = tx(7L, "expense", "EGP", "5");
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        TransactionRequest patch = new TransactionRequest();
        patch.setAmount(new BigDecimal("99"));

        TransactionResponse res = service.update(1L, 7L, patch);

        assertEquals(new BigDecimal("99"), res.getAmount());
        assertEquals("food", res.getCategory()); // untouched
    }

    @Test
    void ownerCanDelete() {
        Transaction existing = tx(7L, "expense", "EGP", "5");
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        service.delete(1L, 7L);
        verify(repo).delete(existing);
    }

    @Test
    void summaryTotalsPerCurrencyAndType() {
        when(repo.findByUserIdOrderByRecordedAtDesc(7L)).thenReturn(List.of(
                tx(7L, "expense", "EGP", "10"), tx(7L, "expense", "EGP", "5.5"),
                tx(7L, "income", "EGP", "100"), tx(7L, "expense", "USD", "3")));

        Map<String, Map<String, BigDecimal>> summary = service.getSummary(7L);

        assertEquals(new BigDecimal("15.5"), summary.get("EGP").get("expense"));
        assertEquals(new BigDecimal("100"), summary.get("EGP").get("income"));
        assertEquals(new BigDecimal("3"), summary.get("USD").get("expense"));
    }
}
