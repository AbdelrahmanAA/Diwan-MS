-- Positions of the user's cards, accounts and loans per bank, from bank SMS (mobile app):
-- which of the user's own accounts / cards a transaction moved, and the balance the SMS reported after it.
ALTER TABLE `transactions`
  ADD COLUMN `account_ref` varchar(8) DEFAULT NULL,          -- own account debited/credited (last 4 digits)
  ADD COLUMN `counter_account_ref` varchar(8) DEFAULT NULL,  -- own account on the other side of a transfer between own accounts
  ADD COLUMN `card_ref` varchar(8) DEFAULT NULL,             -- card used (last 4 digits)
  ADD COLUMN `card_network` varchar(50) DEFAULT NULL,        -- Visa, mada, ...
  ADD COLUMN `balance` decimal(14,2) DEFAULT NULL,           -- balance reported by the SMS after the transaction
  ADD COLUMN `balance_currency` varchar(10) DEFAULT NULL,
  ADD COLUMN `balance_of` varchar(10) DEFAULT NULL;          -- CARD (available) | ACCOUNT | LOAN (remaining)

-- What the user entered once: an account's balance at a moment (then kept up to date from the SMS flows),
-- a card's credit limit (owed = limit - available), a nickname.
CREATE TABLE `bank_products` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `bank` varchar(100) NOT NULL,
  `kind` varchar(10) NOT NULL,          -- ACCOUNT | CARD | LOAN
  `ref` varchar(8) NOT NULL,            -- last 4 digits, or "loan"
  `label` varchar(100) DEFAULT NULL,
  `credit_limit` decimal(14,2) DEFAULT NULL,
  `opening_balance` decimal(14,2) DEFAULT NULL,
  `opening_at` datetime(6) DEFAULT NULL,
  `currency` varchar(10) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bank_product` (`user_id`, `bank`, `kind`, `ref`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
