-- Baseline of the diwan-gateway schema, extracted from the MySQL database Hibernate had created.
-- Existing databases are baselined at version 1 (this script is skipped there); new databases run it.
-- Every later schema change must be a new V<n>__*.sql file.

CREATE TABLE `app_features` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `color` varchar(20) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `display_name` varchar(100) NOT NULL,
  `icon` varchar(10) DEFAULT NULL,
  `name` varchar(50) NOT NULL,
  `path` varchar(200) DEFAULT NULL,
  `screen_name` varchar(100) DEFAULT NULL,
  `sort_order` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_qsvb0x2h4nxmvlalaaq4uee7f` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `service_routes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `base_url` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `path_prefix` varchar(255) NOT NULL,
  `service_name` varchar(255) NOT NULL,
  `timeout_ms` int NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_og9ump0g88f52c3ms1po3xhuv` (`service_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
