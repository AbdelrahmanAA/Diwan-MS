-- Baseline of the diwan-smarthome schema, extracted from the MySQL database Hibernate had created.
-- Existing databases are baselined at version 1 (this script is skipped there); new databases run it.
-- Every later schema change must be a new V<n>__*.sql file.

CREATE TABLE `smart_devices` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `device_id` varchar(100) NOT NULL,
  `mqtt_topic` varchar(200) NOT NULL,
  `name` varchar(100) NOT NULL,
  `state` varchar(20) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK5skg60ha32imq88m8vtr8ts5a` (`user_id`,`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
