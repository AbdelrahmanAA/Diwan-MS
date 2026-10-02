-- Transactional outbox: events are written in the same transaction as the change that caused them and
-- relayed to Kafka afterwards (OutboxRelay), so a Kafka outage can no longer lose a "user invalidated" event.
CREATE TABLE `outbox_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` varchar(36) NOT NULL,
  `topic` varchar(255) NOT NULL,
  `message_key` varchar(255) DEFAULT NULL,
  `payload` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `attempts` int NOT NULL,
  `last_error` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_outbox_event_id` (`event_id`),
  KEY `idx_outbox_pending` (`published_at`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
