-- Baseline of the diwan-logging schema, extracted from the MySQL database Hibernate had created.
-- Existing databases are baselined at version 1 (this script is skipped there); new databases run it.
-- Every later schema change must be a new V<n>__*.sql file.

CREATE TABLE `request_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `client_ip` varchar(64) DEFAULT NULL,
  `duration_ms` bigint DEFAULT NULL,
  `error_message` varchar(1024) DEFAULT NULL,
  `method` varchar(255) DEFAULT NULL,
  `path` varchar(512) DEFAULT NULL,
  `request_time` datetime(6) DEFAULT NULL,
  `source_service` varchar(255) DEFAULT NULL,
  `status_code` int DEFAULT NULL,
  `target_service` varchar(255) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
