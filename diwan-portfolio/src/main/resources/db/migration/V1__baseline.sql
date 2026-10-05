-- Portfolio core. Columns marked "enc" hold AES-GCM ciphertext (diwan-common EncryptedStringConverter):
-- they are sized for the ciphertext and can never be searched or aggregated in SQL.
-- Amounts and codes stay plain so totals and grouping run in the database.

CREATE TABLE user_preference (
  user_id        BIGINT       NOT NULL,
  language       VARCHAR(8)   NOT NULL,
  base_currency  VARCHAR(3)   NOT NULL,
  updated_at     DATETIME(6)  NOT NULL,
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Public market instruments (shared by all users, not sensitive)
CREATE TABLE instrument (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  symbol        VARCHAR(32)  NOT NULL,
  name          VARCHAR(200) NULL,
  exchange      VARCHAR(64)  NULL,
  country_code  VARCHAR(2)   NULL,
  currency      VARCHAR(3)   NULL,
  asset_type    VARCHAR(32)  NULL,
  created_at    DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_instrument_symbol (symbol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE portfolio (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  user_id        BIGINT        NOT NULL,
  name           VARCHAR(1024) NOT NULL,           -- enc
  description    TEXT          NULL,               -- enc
  base_currency  VARCHAR(3)    NOT NULL,
  color          VARCHAR(16)   NULL,
  icon           VARCHAR(32)   NULL,
  sort_order     INT           NOT NULL,
  created_at     DATETIME(6)   NOT NULL,
  updated_at     DATETIME(6)   NOT NULL,
  version        BIGINT        NOT NULL,
  PRIMARY KEY (id),
  KEY idx_portfolio_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- quantity / average_cost / realized_pnl / net_income are derived by replaying portfolio_transaction
CREATE TABLE holding (
  id             BIGINT         NOT NULL AUTO_INCREMENT,
  portfolio_id   BIGINT         NOT NULL,
  asset_type     VARCHAR(32)    NOT NULL,
  country_code   VARCHAR(2)     NOT NULL,
  currency       VARCHAR(3)     NOT NULL,
  name           VARCHAR(1024)  NOT NULL,          -- enc
  instrument_id  BIGINT         NULL,
  quantity       DECIMAL(28,8)  NOT NULL,
  average_cost   DECIMAL(28,8)  NOT NULL,
  realized_pnl   DECIMAL(24,4)  NOT NULL,
  net_income     DECIMAL(24,4)  NOT NULL,
  notes          TEXT           NULL,              -- enc
  attributes     TEXT           NULL,              -- enc (JSON: address, area, rent, account number, ...)
  closed         BIT(1)         NOT NULL,
  created_at     DATETIME(6)    NOT NULL,
  updated_at     DATETIME(6)    NOT NULL,
  version        BIGINT         NOT NULL,
  PRIMARY KEY (id),
  KEY idx_holding_portfolio (portfolio_id),
  KEY idx_holding_instrument (instrument_id),
  CONSTRAINT fk_holding_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio (id) ON DELETE CASCADE,
  CONSTRAINT fk_holding_instrument FOREIGN KEY (instrument_id) REFERENCES instrument (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE portfolio_transaction (
  id          BIGINT         NOT NULL AUTO_INCREMENT,
  holding_id  BIGINT         NOT NULL,
  type        VARCHAR(16)    NOT NULL,
  trade_date  DATE           NOT NULL,
  quantity    DECIMAL(28,8)  NULL,
  price       DECIMAL(28,8)  NULL,
  amount      DECIMAL(24,4)  NULL,
  fees        DECIMAL(24,4)  NOT NULL,
  note        TEXT           NULL,                 -- enc
  created_at  DATETIME(6)    NOT NULL,
  PRIMARY KEY (id),
  KEY idx_tx_holding_date (holding_id, trade_date),
  CONSTRAINT fk_tx_holding FOREIGN KEY (holding_id) REFERENCES holding (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Manual market value for assets without a quote (real estate, private business, funds...)
CREATE TABLE valuation (
  id              BIGINT         NOT NULL AUTO_INCREMENT,
  holding_id      BIGINT         NOT NULL,
  valuation_date  DATE           NOT NULL,
  value           DECIMAL(24,4)  NOT NULL,
  note            TEXT           NULL,             -- enc
  created_at      DATETIME(6)    NOT NULL,
  PRIMARY KEY (id),
  KEY idx_valuation_holding_date (holding_id, valuation_date),
  CONSTRAINT fk_valuation_holding FOREIGN KEY (holding_id) REFERENCES holding (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE allocation_target (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  portfolio_id    BIGINT        NOT NULL,
  dimension       VARCHAR(16)   NOT NULL,          -- ASSET_TYPE | COUNTRY
  target_key      VARCHAR(32)   NOT NULL,
  target_percent  DECIMAL(5,2)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_target (portfolio_id, dimension, target_key),
  CONSTRAINT fk_target_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE goal (
  id             BIGINT         NOT NULL AUTO_INCREMENT,
  portfolio_id   BIGINT         NOT NULL,
  name           VARCHAR(1024)  NOT NULL,          -- enc
  target_amount  DECIMAL(24,4)  NOT NULL,
  target_date    DATE           NULL,
  created_at     DATETIME(6)    NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_goal_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tag (
  id          BIGINT         NOT NULL AUTO_INCREMENT,
  user_id     BIGINT         NOT NULL,
  name        VARCHAR(512)   NOT NULL,             -- enc
  color       VARCHAR(16)    NULL,
  created_at  DATETIME(6)    NOT NULL,
  PRIMARY KEY (id),
  KEY idx_tag_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE holding_tag (
  holding_id  BIGINT NOT NULL,
  tag_id      BIGINT NOT NULL,
  PRIMARY KEY (holding_id, tag_id),
  KEY idx_holding_tag_tag (tag_id),
  CONSTRAINT fk_ht_holding FOREIGN KEY (holding_id) REFERENCES holding (id) ON DELETE CASCADE,
  CONSTRAINT fk_ht_tag FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Exchange rates as "1 USD = rate units of currency", one row per currency and day
CREATE TABLE fx_rate (
  currency   VARCHAR(3)     NOT NULL,
  rate_date  DATE           NOT NULL,
  rate       DECIMAL(24,10) NOT NULL,
  PRIMARY KEY (currency, rate_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
