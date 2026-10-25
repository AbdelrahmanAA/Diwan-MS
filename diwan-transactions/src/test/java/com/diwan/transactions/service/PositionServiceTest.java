package com.diwan.transactions.service;

import com.diwan.transactions.dto.Positions;
import com.diwan.transactions.entity.BankProduct;
import com.diwan.transactions.entity.Transaction;
import com.diwan.transactions.repository.BankProductRepository;
import com.diwan.transactions.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The Al Rajhi SMS the user provided (Oct 2026), as imported transactions. */
class PositionServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Riyadh");
    private final TransactionRepository repo = mock(TransactionRepository.class);
    private final BankProductRepository products = mock(BankProductRepository.class);
    private final PositionService service = new PositionService(repo, products,
            Clock.fixed(ZonedDateTime.of(2026, 10, 26, 9, 0, 0, 0, ZONE).toInstant(), ZONE));

    private static Transaction.Builder sms(String type, String amount, String at) {
        return Transaction.builder().userId(7L).bank("Al Rajhi").type(type).category("x").currency("SAR")
                .amount(new BigDecimal(amount)).recordedAt(LocalDateTime.parse(at)).source("sms");
    }

    private static BigDecimal d(String v) { return new BigDecimal(v); }

    private static BankProduct product(String kind, String ref) {
        BankProduct p = new BankProduct();
        p.setUserId(7L);
        p.setBank("Al Rajhi");
        p.setKind(kind);
        p.setRef(ref);
        return p;
    }

    private Positions.Bank alRajhi() {
        when(repo.findByUserIdOrderByRecordedAtDesc(7L)).thenReturn(List.of( // newest first
                sms("transfer", "5500", "2026-10-25T22:26").accountRef("2410").counterAccountRef("8468").build(),
                sms("transfer", "4980", "2026-10-25T22:25").cardRef("1553").cardNetwork("Visa")
                        .balance(d("5844.89")).balanceCurrency("SAR").balanceOf("CARD").build(),
                sms("expense", "103.51", "2026-10-25T21:18").cardRef("1553").cardNetwork("Visa")
                        .balance(d("864.89")).balanceCurrency("SAR").balanceOf("CARD").build(),
                sms("expense", "5210", "2026-10-25T20:05").accountRef("2410")
                        .balance(d("41680")).balanceCurrency("SAR").balanceOf("LOAN").build(),
                sms("expense", "5000", "2026-10-25T16:10").accountRef("2410").cardRef("5101").cardNetwork("mada").build(),
                sms("income", "34965", "2026-10-25T00:23").accountRef("2410").build(),
                sms("expense", "3000", "2026-10-20T11:44").cardRef("5101").cardNetwork("mada").build(), // ATM, no account in the SMS
                sms("income", "30000", "2026-09-25T00:20").accountRef("2410").build(),
                Transaction.builder().userId(7L).bank("Unknown").type("expense").category("food").currency("SAR")
                        .amount(d("50")).recordedAt(LocalDateTime.parse("2026-10-24T12:00")).build())); // manual: ignored
        BankProduct account = product(BankProduct.ACCOUNT, "2410");
        account.setOpeningBalance(d("10000"));
        account.setOpeningAt(LocalDateTime.parse("2026-10-25T00:00"));
        account.setLabel("Main");
        BankProduct card = product(BankProduct.CARD, "1553");
        card.setCreditLimit(d("10000"));
        when(products.findByUserId(7L)).thenReturn(List.of(account, card));

        List<Positions.Bank> banks = service.positions(7L);
        assertEquals(1, banks.size());
        return banks.get(0);
    }

    @Test
    void accountBalanceIsTheEnteredBalancePlusEverySmsMovementSince() {
        Positions.Account main = alRajhi().accounts().get(0);

        assertEquals("2410", main.ref());
        assertEquals("Main", main.label());
        // 10000 + salary 34965 - barq 5000 - loan 5210 - own transfer 5500 (the ATM withdrawal was before)
        assertEquals(0, d("29255").compareTo(main.balance()));
        assertEquals(0, d("34965").compareTo(main.monthIn()));
        assertEquals(0, d("18710").compareTo(main.monthOut())); // 5000 + 3000 (ATM by its mada card) + 5500 + 5210
        assertEquals(List.of("5101"), main.cards());
    }

    @Test
    void theOtherSideOfAnOwnTransferIsMoneyIn() {
        Positions.Account savings = alRajhi().accounts().get(1);

        assertEquals("8468", savings.ref());
        assertNull(savings.balance()); // never entered
        assertEquals(0, d("5500").compareTo(savings.monthIn()));
    }

    @Test
    void creditCardShowsTheLatestAvailableAndWhatIsOwed() {
        List<Positions.Card> cards = alRajhi().cards();
        Positions.Card visa = cards.get(0);

        assertEquals("1553", visa.ref());
        assertEquals("Visa", visa.network());
        assertEquals(0, d("5844.89").compareTo(visa.available()));
        assertEquals(LocalDateTime.parse("2026-10-25T22:25"), visa.availableAt());
        assertEquals(0, d("4155.11").compareTo(visa.owed()));
        assertEquals(0, d("103.51").compareTo(visa.monthSpent())); // the payment is a transfer, not spending

        Positions.Card mada = cards.get(1);
        assertEquals("5101", mada.ref());
        assertEquals("2410", mada.account());
        assertNull(mada.available());
        assertEquals(0, d("8000").compareTo(mada.monthSpent()));
    }

    @Test
    void loanShowsTheRemainingAmountOfTheLatestInstalment() {
        Positions.Loan loan = alRajhi().loans().get(0);

        assertEquals(0, d("41680").compareTo(loan.remaining()));
        assertEquals(0, d("5210").compareTo(loan.lastInstalment()));
        assertEquals("2410", loan.account());
    }

    @Test
    void enteringABalanceStartsTheEstimateNow() {
        when(products.findByUserIdAndBankAndKindAndRef(7L, "Al Rajhi", "ACCOUNT", "2410")).thenReturn(Optional.empty());
        when(products.save(any(BankProduct.class))).thenAnswer(inv -> inv.getArgument(0));
        Positions.ProductRequest req = new Positions.ProductRequest();
        req.setBank("Al Rajhi");
        req.setKind("ACCOUNT");
        req.setRef("2410");
        req.setBalance(d("1200.50"));
        req.setCreditLimit(d("999")); // ignored for an account

        service.saveProduct(7L, req);

        ArgumentCaptor<BankProduct> saved = ArgumentCaptor.forClass(BankProduct.class);
        verify(products).save(saved.capture());
        assertEquals(d("1200.50"), saved.getValue().getOpeningBalance());
        assertEquals(LocalDateTime.parse("2026-10-26T09:00"), saved.getValue().getOpeningAt());
        assertNull(saved.getValue().getCreditLimit());
        assertEquals(7L, saved.getValue().getUserId());
    }

    @Test
    void theBalanceMomentFromThePhoneWins() {
        when(products.findByUserIdAndBankAndKindAndRef(7L, "Al Rajhi", "ACCOUNT", "2410")).thenReturn(Optional.empty());
        when(products.save(any(BankProduct.class))).thenAnswer(inv -> inv.getArgument(0));
        Positions.ProductRequest req = new Positions.ProductRequest();
        req.setBank("Al Rajhi");
        req.setKind("ACCOUNT");
        req.setRef("2410");
        req.setBalance(d("5"));
        req.setBalanceAt(LocalDateTime.parse("2026-10-26T12:00")); // phone in UTC+3, server clock in UTC

        assertEquals(LocalDateTime.parse("2026-10-26T12:00"), service.saveProduct(7L, req).getOpeningAt());
    }
}
