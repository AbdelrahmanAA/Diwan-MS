package com.diwan.transactions.service;

import com.diwan.transactions.dto.Positions;
import com.diwan.transactions.entity.BankProduct;
import com.diwan.transactions.entity.Transaction;
import com.diwan.transactions.repository.BankProductRepository;
import com.diwan.transactions.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The user's cards, accounts and loans per bank, worked out from the transactions imported from bank SMS:
 * a card's available amount and a loan's remaining amount come from the latest SMS that reported them; an
 * account's balance (never in the SMS) is the balance the user entered once plus every movement since.
 */
@Service
public class PositionService {

    static final String DEFAULT_CURRENCY = "SAR";

    private final TransactionRepository transactions;
    private final BankProductRepository products;
    private final Clock clock;

    @Autowired
    public PositionService(TransactionRepository transactions, BankProductRepository products) {
        this(transactions, products, Clock.systemDefaultZone());
    }

    PositionService(TransactionRepository transactions, BankProductRepository products, Clock clock) {
        this.transactions = transactions;
        this.products = products;
        this.clock = clock;
    }

    public List<Positions.Bank> positions(Long userId) {
        List<Transaction> all = transactions.findByUserIdOrderByRecordedAtDesc(userId).stream()
                .filter(t -> t.getAccountRef() != null || t.getCardRef() != null || t.getBalanceOf() != null)
                .toList(); // newest first
        Map<String, BankProduct> settings = products.findByUserId(userId).stream()
                .collect(Collectors.toMap(p -> key(p.getBank(), p.getKind(), p.getRef()), p -> p, (a, b) -> a));
        LocalDateTime monthStart = LocalDate.now(clock).withDayOfMonth(1).atStartOfDay();

        Map<String, List<Transaction>> byBank = all.stream()
                .collect(Collectors.groupingBy(Transaction::getBank, TreeMap::new, Collectors.toList()));
        List<Positions.Bank> result = new ArrayList<>();
        byBank.forEach((bank, txs) -> result.add(new Positions.Bank(bank,
                accounts(bank, txs, settings, monthStart), cards(bank, txs, settings, monthStart), loans(bank, txs, settings))));
        return result;
    }

    // ── cards ────────────────────────────────────────────────────────────────

    private List<Positions.Card> cards(String bank, List<Transaction> txs, Map<String, BankProduct> settings,
                                       LocalDateTime monthStart) {
        Map<String, String> cardAccount = cardAccounts(txs);
        List<Positions.Card> cards = new ArrayList<>();
        for (String ref : new TreeSet<>(refs(txs, Transaction::getCardRef))) {
            List<Transaction> mine = txs.stream().filter(t -> ref.equals(t.getCardRef())).toList();
            BankProduct p = settings.get(key(bank, BankProduct.CARD, ref));
            Optional<Transaction> withBalance = mine.stream()
                    .filter(t -> BankProduct.CARD.equals(t.getBalanceOf()) && t.getBalance() != null).findFirst();
            BigDecimal available = withBalance.map(Transaction::getBalance).orElse(null);
            String currency = withBalance.map(Transaction::getBalanceCurrency)
                    .orElse(p != null && p.getCurrency() != null ? p.getCurrency() : DEFAULT_CURRENCY);
            BigDecimal limit = p == null ? null : p.getCreditLimit();
            BigDecimal owed = limit != null && available != null ? limit.subtract(available) : null;
            BigDecimal spent = sum(mine, t -> "expense".equals(t.getType()) && currency.equals(t.getCurrency())
                    && !t.getRecordedAt().isBefore(monthStart));
            cards.add(new Positions.Card(ref, first(mine, Transaction::getCardNetwork), p == null ? null : p.getLabel(),
                    currency, available, withBalance.map(Transaction::getRecordedAt).orElse(null), limit, owed,
                    cardAccount.get(ref), spent, mine.get(0).getRecordedAt()));
        }
        return cards;
    }

    /** Debit cards are seen with their account in some SMS ("By:5101;mada  From:2410"). */
    private static Map<String, String> cardAccounts(List<Transaction> txs) {
        Map<String, String> links = new HashMap<>();
        for (Transaction t : txs) {
            if (t.getCardRef() != null && t.getAccountRef() != null) links.putIfAbsent(t.getCardRef(), t.getAccountRef());
        }
        return links;
    }

    // ── accounts ─────────────────────────────────────────────────────────────

    private List<Positions.Account> accounts(String bank, List<Transaction> txs, Map<String, BankProduct> settings,
                                             LocalDateTime monthStart) {
        Map<String, String> cardAccount = cardAccounts(txs);
        Set<String> refs = new TreeSet<>(refs(txs, Transaction::getAccountRef));
        refs.addAll(refs(txs, Transaction::getCounterAccountRef));
        List<Positions.Account> accounts = new ArrayList<>();
        for (String ref : refs) {
            BankProduct p = settings.get(key(bank, BankProduct.ACCOUNT, ref));
            String currency = p != null && p.getCurrency() != null ? p.getCurrency() : DEFAULT_CURRENCY;
            List<String> cards = cardAccount.entrySet().stream().filter(e -> ref.equals(e.getValue()))
                    .map(Map.Entry::getKey).sorted().toList();
            List<Transaction> moves = txs.stream()
                    .filter(t -> currency.equals(t.getCurrency()) && movement(t, ref, cards).signum() != 0).toList();

            BigDecimal in = BigDecimal.ZERO, out = BigDecimal.ZERO, sinceOpening = BigDecimal.ZERO;
            for (Transaction t : moves) {
                BigDecimal m = movement(t, ref, cards);
                if (!t.getRecordedAt().isBefore(monthStart)) {
                    if (m.signum() > 0) in = in.add(m); else out = out.add(m.negate());
                }
                if (p != null && p.getOpeningAt() != null && t.getRecordedAt().isAfter(p.getOpeningAt())) {
                    sinceOpening = sinceOpening.add(m);
                }
            }
            BigDecimal balance = p == null || p.getOpeningBalance() == null ? null : p.getOpeningBalance().add(sinceOpening);
            LocalDateTime last = moves.isEmpty() ? null : moves.get(0).getRecordedAt();
            accounts.add(new Positions.Account(ref, p == null ? null : p.getLabel(), currency, balance,
                    p == null ? null : p.getOpeningAt(), in, out, cards, last));
        }
        return accounts;
    }

    /**
     * What [t] did to account [ref]: + money in, - money out, 0 not this account. A debit card's purchases and
     * ATM withdrawals that name no account belong to the card's account.
     */
    static BigDecimal movement(Transaction t, String ref, List<String> cardsOfAccount) {
        BigDecimal amount = t.getAmount();
        if (ref.equals(t.getCounterAccountRef())) return amount; // the receiving side of an own transfer
        boolean mine = ref.equals(t.getAccountRef())
                || (t.getAccountRef() == null && t.getCardRef() != null && cardsOfAccount.contains(t.getCardRef())
                    && !"transfer".equals(t.getType()));
        if (!mine) return BigDecimal.ZERO;
        return "income".equals(t.getType()) ? amount : amount.negate();
    }

    // ── loans ────────────────────────────────────────────────────────────────

    private static List<Positions.Loan> loans(String bank, List<Transaction> txs, Map<String, BankProduct> settings) {
        Optional<Transaction> latest = txs.stream()
                .filter(t -> BankProduct.LOAN.equals(t.getBalanceOf()) && t.getBalance() != null).findFirst();
        if (latest.isEmpty()) return List.of();
        Transaction t = latest.get();
        BankProduct p = settings.get(key(bank, BankProduct.LOAN, "loan"));
        return List.of(new Positions.Loan("loan", p == null ? null : p.getLabel(),
                t.getBalanceCurrency() != null ? t.getBalanceCurrency() : t.getCurrency(), t.getBalance(),
                t.getRecordedAt(), t.getAmount(), t.getAccountRef()));
    }

    // ── what the user enters ─────────────────────────────────────────────────

    public BankProduct saveProduct(Long userId, Positions.ProductRequest req) {
        String bank = req.getBank().trim();
        String ref = req.getRef().trim();
        BankProduct p = products.findByUserIdAndBankAndKindAndRef(userId, bank, req.getKind(), ref).orElseGet(() -> {
            BankProduct n = new BankProduct();
            n.setUserId(userId);
            n.setBank(bank);
            n.setKind(req.getKind());
            n.setRef(ref);
            return n;
        });
        p.setLabel(req.getLabel() == null || req.getLabel().isBlank() ? null : req.getLabel().trim());
        if (req.getCurrency() != null && !req.getCurrency().isBlank()) p.setCurrency(req.getCurrency().trim().toUpperCase());
        if (BankProduct.CARD.equals(req.getKind())) p.setCreditLimit(req.getCreditLimit());
        if (BankProduct.ACCOUNT.equals(req.getKind()) && req.getBalance() != null) {
            p.setOpeningBalance(req.getBalance());
            p.setOpeningAt(req.getBalanceAt() != null ? req.getBalanceAt() : LocalDateTime.now(clock));
        }
        return products.save(p);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static String key(String bank, String kind, String ref) { return bank + "|" + kind + "|" + ref; }

    private static Set<String> refs(List<Transaction> txs, java.util.function.Function<Transaction, String> f) {
        return txs.stream().map(f).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private static String first(List<Transaction> txs, java.util.function.Function<Transaction, String> f) {
        return txs.stream().map(f).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private static BigDecimal sum(List<Transaction> txs, Predicate<Transaction> filter) {
        return txs.stream().filter(filter).map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
