-- Dose times and reminders on medications, appointments, and the log of doses taken or skipped.

ALTER TABLE medical_medications
  ADD COLUMN schedule_times VARCHAR(100) NULL,
  ADD COLUMN reminders BIT(1) NOT NULL DEFAULT b'0';

CREATE TABLE medical_appointments (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  user_id           BIGINT       NOT NULL,
  profile_id        BIGINT       NOT NULL,
  appointment_with  TEXT         NOT NULL,
  appointment_at    DATETIME(6)  NOT NULL,
  location          TEXT         NULL,
  notes             TEXT         NULL,
  recorded_at       DATETIME(6)  NOT NULL,
  created_at        DATETIME(6)  NOT NULL,
  updated_at        DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  KEY idx_medical_appointments_user (user_id),
  KEY idx_medical_appointments_profile (profile_id),
  CONSTRAINT fk_medical_appointments_profile FOREIGN KEY (profile_id) REFERENCES medical_profile (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE medical_dose_log (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  user_id        BIGINT       NOT NULL,
  profile_id     BIGINT       NOT NULL,
  medication_id  BIGINT       NOT NULL,
  scheduled_at   DATETIME(6)  NOT NULL,
  status         VARCHAR(10)  NOT NULL,
  recorded_at    DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_medical_dose (medication_id, scheduled_at),
  KEY idx_medical_dose_user (user_id),
  KEY idx_medical_dose_profile_time (profile_id, scheduled_at),
  CONSTRAINT fk_medical_dose_medication FOREIGN KEY (medication_id) REFERENCES medical_medications (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
