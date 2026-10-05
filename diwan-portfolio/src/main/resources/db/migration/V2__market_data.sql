-- Market data refreshed by the scheduled jobs (public data, not encrypted)

CREATE TABLE instrument_quote (
  instrument_id   BIGINT         NOT NULL,
  price           DECIMAL(28,8)  NOT NULL,
  previous_close  DECIMAL(28,8)  NULL,
  day_change      DECIMAL(28,8)  NULL,
  change_percent  DECIMAL(12,4)  NULL,
  day_high        DECIMAL(28,8)  NULL,
  day_low         DECIMAL(28,8)  NULL,
  quote_time      DATETIME(6)    NOT NULL,
  fetched_at      DATETIME(6)    NOT NULL,
  provider        VARCHAR(16)    NOT NULL,
  PRIMARY KEY (instrument_id),
  CONSTRAINT fk_quote_instrument FOREIGN KEY (instrument_id) REFERENCES instrument (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- One closing price per instrument and day, recorded by the daily-close job
CREATE TABLE price_history (
  instrument_id  BIGINT         NOT NULL,
  price_date     DATE           NOT NULL,
  close_price    DECIMAL(28,8)  NOT NULL,
  PRIMARY KEY (instrument_id, price_date),
  CONSTRAINT fk_history_instrument FOREIGN KEY (instrument_id) REFERENCES instrument (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE news_article (
  id             BIGINT         NOT NULL AUTO_INCREMENT,
  instrument_id  BIGINT         NOT NULL,
  url_hash       VARCHAR(64)    NOT NULL,          -- sha-256 of the url, for de-duplication
  url            VARCHAR(2048)  NOT NULL,
  headline       VARCHAR(512)   NOT NULL,
  summary        TEXT           NULL,
  source         VARCHAR(128)   NULL,
  image_url      VARCHAR(2048)  NULL,
  language       VARCHAR(8)     NOT NULL,
  published_at   DATETIME(6)    NOT NULL,
  fetched_at     DATETIME(6)    NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_news_instrument_url (instrument_id, url_hash),
  KEY idx_news_published (published_at),
  CONSTRAINT fk_news_instrument FOREIGN KEY (instrument_id) REFERENCES instrument (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Symbols a user follows without owning them
CREATE TABLE watchlist_item (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  user_id        BIGINT       NOT NULL,
  instrument_id  BIGINT       NOT NULL,
  created_at     DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_watchlist (user_id, instrument_id),
  CONSTRAINT fk_watch_instrument FOREIGN KEY (instrument_id) REFERENCES instrument (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ShedLock: only one instance runs each scheduled job
CREATE TABLE shedlock (
  name        VARCHAR(64)   NOT NULL,
  lock_until  TIMESTAMP(3)  NOT NULL,
  locked_at   TIMESTAMP(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  locked_by   VARCHAR(255)  NOT NULL,
  PRIMARY KEY (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
