package com.diwan.transactions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Cards, accounts and loans per bank, as known from the user's bank SMS plus what they entered. */
public final class Positions {

    private Positions() {}

    public record Bank(String bank, List<Account> accounts, List<Card> cards, List<Loan> loans) {}

    /**
     * {@code balance} is null until the user entered the account's balance once ({@code balanceSince}); after
     * that it is that balance plus every SMS movement of the account since, i.e. an estimate.
     */
    public record Account(String ref, String label, String currency, BigDecimal balance, LocalDateTime balanceSince,
                          BigDecimal monthIn, BigDecimal monthOut, List<String> cards, LocalDateTime lastActivity) {}

    /** {@code available} from the latest SMS with a balance; {@code owed} = limit - available when a limit is set. */
    public record Card(String ref, String network, String label, String currency, BigDecimal available,
                       LocalDateTime availableAt, BigDecimal creditLimit, BigDecimal owed, String account,
                       BigDecimal monthSpent, LocalDateTime lastActivity) {}

    public record Loan(String ref, String label, String currency, BigDecimal remaining, LocalDateTime remainingAt,
                       BigDecimal lastInstalment, String account) {}

    /** PUT /api/transactions/positions/products: the user's own figures for one product. */
    public static class ProductRequest {
        @NotBlank private String bank;
        @Pattern(regexp = "ACCOUNT|CARD|LOAN", message = "kind must be ACCOUNT, CARD or LOAN") private String kind;
        @NotBlank @Size(max = 8) private String ref;
        @Size(max = 100) private String label;
        @PositiveOrZero private BigDecimal creditLimit;
        /** The account's balance now; movements after this moment are added to it. */
        private BigDecimal balance;
        /** When [balance] was read, in the phone's local time like the SMS transactions; default now. */
        private LocalDateTime balanceAt;
        @Size(max = 10) private String currency;

        public String getBank()                    { return bank; }
        public void setBank(String v)              { this.bank = v; }
        public String getKind()                    { return kind; }
        public void setKind(String v)              { this.kind = v; }
        public String getRef()                     { return ref; }
        public void setRef(String v)               { this.ref = v; }
        public String getLabel()                   { return label; }
        public void setLabel(String v)             { this.label = v; }
        public BigDecimal getCreditLimit()         { return creditLimit; }
        public void setCreditLimit(BigDecimal v)   { this.creditLimit = v; }
        public BigDecimal getBalance()             { return balance; }
        public void setBalance(BigDecimal v)       { this.balance = v; }
        public LocalDateTime getBalanceAt()        { return balanceAt; }
        public void setBalanceAt(LocalDateTime v)  { this.balanceAt = v; }
        public String getCurrency()                { return currency; }
        public void setCurrency(String v)          { this.currency = v; }
    }
}
