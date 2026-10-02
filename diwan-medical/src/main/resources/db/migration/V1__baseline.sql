-- Baseline of the diwan-medical schema, extracted from the MySQL database Hibernate had created.
-- Existing databases are baselined at version 1 (this script is skipped there); new databases run it.
-- Every later schema change must be a new V<n>__*.sql file.

CREATE TABLE `medical_allergies` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `allergen` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `notes` text,
  `reaction` varchar(255) DEFAULT NULL,
  `recorded_at` datetime(6) NOT NULL,
  `severity` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `medical_blood_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `blood_type` varchar(10) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `notes` text,
  `recorded_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `medical_chronic_diseases` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `current_treatment` text,
  `diagnosed_at` date DEFAULT NULL,
  `disease_name` varchar(255) NOT NULL,
  `notes` text,
  `recorded_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `medical_family_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `condition_name` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `notes` text,
  `recorded_at` datetime(6) NOT NULL,
  `relation` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `medical_lab_results` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `notes` text,
  `recorded_at` datetime(6) NOT NULL,
  `reference_range` varchar(255) DEFAULT NULL,
  `result` varchar(255) DEFAULT NULL,
  `test_date` date DEFAULT NULL,
  `test_name` varchar(255) NOT NULL,
  `unit` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `medical_medications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `dosage` varchar(255) DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `frequency` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `notes` text,
  `recorded_at` datetime(6) NOT NULL,
  `start_date` date DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `medical_surgeries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `hospital` varchar(255) DEFAULT NULL,
  `notes` text,
  `recorded_at` datetime(6) NOT NULL,
  `surgeon` varchar(255) DEFAULT NULL,
  `surgery_date` date DEFAULT NULL,
  `surgery_name` varchar(255) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `medical_vaccines` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `dose_number` int DEFAULT NULL,
  `next_dose_date` date DEFAULT NULL,
  `notes` text,
  `recorded_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `vaccination_date` date DEFAULT NULL,
  `vaccine_name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
