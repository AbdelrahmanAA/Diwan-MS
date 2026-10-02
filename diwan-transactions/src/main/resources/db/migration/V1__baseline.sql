-- Baseline of the diwan-transactions schema, extracted from the MySQL database Hibernate had created.
-- Existing databases are baselined at version 1 (this script is skipped there); new databases run it.
-- Every later schema change must be a new V<n>__*.sql file.

CREATE TABLE `transactions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(14,2) NOT NULL,
  `bank` varchar(255) NOT NULL,
  `category` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `currency` varchar(10) NOT NULL,
  `description` text,
  `merchant_name` varchar(200) DEFAULT NULL,
  `notes` text,
  `payment_method` varchar(50) DEFAULT NULL,
  `recorded_at` datetime(6) NOT NULL,
  `reference_number` varchar(100) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `tags` text,
  `type` varchar(255) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_bank` (`bank`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`),
  KEY `idx_recordedAt` (`recorded_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
