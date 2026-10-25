package com.diwan.transactions.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** A batch of transactions detected by the mobile app (bank SMS), each with the fingerprint of its message. */
public class ImportTransactionsRequest {

    @NotEmpty(message = "transactions is required")
    @Size(max = 500, message = "At most 500 transactions per request")
    private List<@Valid Item> transactions;

    public List<Item> getTransactions()            { return transactions; }
    public void setTransactions(List<Item> v)      { this.transactions = v; }

    public static class Item extends TransactionRequest {
        /** SHA-256 of the SMS (hex); the same message is stored only once per user. */
        @NotBlank(message = "sourceRef is required")
        @Size(max = 64, message = "sourceRef is at most 64 characters")
        private String sourceRef;

        private String accountRef;
        private String counterAccountRef;
        private String cardRef;
        private String cardNetwork;
        private BigDecimal balance;
        private String balanceCurrency;
        private String balanceOf;

        public String getSourceRef()               { return sourceRef; }
        public void setSourceRef(String v)         { this.sourceRef = v; }
        public String getAccountRef() { return accountRef; }
        public void setAccountRef(String v) { this.accountRef = v; }
        public String getCounterAccountRef() { return counterAccountRef; }
        public void setCounterAccountRef(String v) { this.counterAccountRef = v; }
        public String getCardRef() { return cardRef; }
        public void setCardRef(String v) { this.cardRef = v; }
        public String getCardNetwork() { return cardNetwork; }
        public void setCardNetwork(String v) { this.cardNetwork = v; }
        public BigDecimal getBalance() { return balance; }
        public void setBalance(BigDecimal v) { this.balance = v; }
        public String getBalanceCurrency() { return balanceCurrency; }
        public void setBalanceCurrency(String v) { this.balanceCurrency = v; }
        public String getBalanceOf() { return balanceOf; }
        public void setBalanceOf(String v) { this.balanceOf = v; }
    }
}
