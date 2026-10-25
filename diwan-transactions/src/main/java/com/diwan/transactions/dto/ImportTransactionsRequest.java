package com.diwan.transactions.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

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

        public String getSourceRef()               { return sourceRef; }
        public void setSourceRef(String v)         { this.sourceRef = v; }
    }
}
