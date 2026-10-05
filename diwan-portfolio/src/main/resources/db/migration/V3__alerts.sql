-- Alert rules set by users and the alerts they produced

CREATE TABLE alert_rule (
  id                 BIGINT         NOT NULL AUTO_INCREMENT,
  user_id            BIGINT         NOT NULL,
  type               VARCHAR(32)    NOT NULL,
  instrument_id      BIGINT         NULL,          -- PRICE_ABOVE / PRICE_BELOW / DAILY_MOVE_PERCENT
  holding_id         BIGINT         NULL,          -- GAIN_PERCENT / LOSS_PERCENT
  portfolio_id       BIGINT         NULL,          -- ALLOCATION_DRIFT
  threshold          DECIMAL(24,8)  NOT NULL,
  active             BIT(1)         NOT NULL,
  note               TEXT           NULL,          -- enc
  last_triggered_at  DATETIME(6)    NULL,
  created_at         DATETIME(6)    NOT NULL,
  PRIMARY KEY (id),
  KEY idx_rule_user (user_id),
  KEY idx_rule_instrument (instrument_id),
  CONSTRAINT fk_rule_instrument FOREIGN KEY (instrument_id) REFERENCES instrument (id),
  CONSTRAINT fk_rule_holding FOREIGN KEY (holding_id) REFERENCES holding (id) ON DELETE CASCADE,
  CONSTRAINT fk_rule_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- A triggered alert. subject is a snapshot of what it was about (holding/portfolio name or symbol), so the
-- alert still reads correctly after the holding is deleted; holding_id / portfolio_id are plain references.
CREATE TABLE portfolio_alert (
  id              BIGINT         NOT NULL AUTO_INCREMENT,
  user_id         BIGINT         NOT NULL,
  rule_id         BIGINT         NULL,
  type            VARCHAR(32)    NOT NULL,
  symbol          VARCHAR(32)    NULL,
  holding_id      BIGINT         NULL,
  portfolio_id    BIGINT         NULL,
  subject         VARCHAR(1024)  NOT NULL,       -- enc
  observed_value  DECIMAL(28,8)  NOT NULL,
  threshold       DECIMAL(24,8)  NOT NULL,
  triggered_at    DATETIME(6)    NOT NULL,
  read_at         DATETIME(6)    NULL,
  PRIMARY KEY (id),
  KEY idx_alert_user_time (user_id, triggered_at),
  CONSTRAINT fk_alert_rule FOREIGN KEY (rule_id) REFERENCES alert_rule (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
