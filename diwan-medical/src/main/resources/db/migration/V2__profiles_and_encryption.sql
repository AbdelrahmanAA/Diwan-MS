-- People: an account keeps records for itself (SELF) and for family members. Existing records go to SELF.
-- Text columns become TEXT because they now hold ciphertext; LegacyDataEncryptor encrypts the existing values
-- at the next start. Severity becomes a fixed code; values that fit no code are kept in the notes.

CREATE TABLE medical_profile (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  user_id       BIGINT       NOT NULL,
  name          TEXT         NULL,
  relation      VARCHAR(20)  NOT NULL,
  birth_date    DATE         NULL,
  created_at    DATETIME(6)  NOT NULL,
  updated_at    DATETIME(6)  NOT NULL,
  self_user_id  BIGINT GENERATED ALWAYS AS (CASE WHEN relation = 'SELF' THEN user_id END) STORED,
  PRIMARY KEY (id),
  UNIQUE KEY uk_medical_profile_self (self_user_id),
  KEY idx_medical_profile_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO medical_profile (user_id, relation, created_at, updated_at)
SELECT u.user_id, 'SELF', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM (
  SELECT user_id FROM medical_medications
  UNION SELECT user_id FROM medical_allergies
  UNION SELECT user_id FROM medical_blood_type
  UNION SELECT user_id FROM medical_surgeries
  UNION SELECT user_id FROM medical_chronic_diseases
  UNION SELECT user_id FROM medical_lab_results
  UNION SELECT user_id FROM medical_vaccines
  UNION SELECT user_id FROM medical_family_history
) u;

ALTER TABLE medical_medications ADD COLUMN profile_id BIGINT NULL AFTER user_id;
UPDATE medical_medications r JOIN medical_profile p ON p.user_id = r.user_id AND p.relation = 'SELF' SET r.profile_id = p.id;
ALTER TABLE medical_medications
  MODIFY profile_id BIGINT NOT NULL,
  ADD INDEX idx_medical_medications_user (user_id),
  ADD INDEX idx_medical_medications_profile (profile_id),
  ADD CONSTRAINT fk_medical_medications_profile FOREIGN KEY (profile_id) REFERENCES medical_profile (id) ON DELETE CASCADE;

ALTER TABLE medical_allergies ADD COLUMN profile_id BIGINT NULL AFTER user_id;
UPDATE medical_allergies r JOIN medical_profile p ON p.user_id = r.user_id AND p.relation = 'SELF' SET r.profile_id = p.id;
ALTER TABLE medical_allergies
  MODIFY profile_id BIGINT NOT NULL,
  ADD INDEX idx_medical_allergies_user (user_id),
  ADD INDEX idx_medical_allergies_profile (profile_id),
  ADD CONSTRAINT fk_medical_allergies_profile FOREIGN KEY (profile_id) REFERENCES medical_profile (id) ON DELETE CASCADE;

ALTER TABLE medical_blood_type ADD COLUMN profile_id BIGINT NULL AFTER user_id;
UPDATE medical_blood_type r JOIN medical_profile p ON p.user_id = r.user_id AND p.relation = 'SELF' SET r.profile_id = p.id;
ALTER TABLE medical_blood_type
  MODIFY profile_id BIGINT NOT NULL,
  ADD INDEX idx_medical_blood_type_user (user_id),
  ADD INDEX idx_medical_blood_type_profile (profile_id),
  ADD CONSTRAINT fk_medical_blood_type_profile FOREIGN KEY (profile_id) REFERENCES medical_profile (id) ON DELETE CASCADE;

ALTER TABLE medical_surgeries ADD COLUMN profile_id BIGINT NULL AFTER user_id;
UPDATE medical_surgeries r JOIN medical_profile p ON p.user_id = r.user_id AND p.relation = 'SELF' SET r.profile_id = p.id;
ALTER TABLE medical_surgeries
  MODIFY profile_id BIGINT NOT NULL,
  ADD INDEX idx_medical_surgeries_user (user_id),
  ADD INDEX idx_medical_surgeries_profile (profile_id),
  ADD CONSTRAINT fk_medical_surgeries_profile FOREIGN KEY (profile_id) REFERENCES medical_profile (id) ON DELETE CASCADE;

ALTER TABLE medical_chronic_diseases ADD COLUMN profile_id BIGINT NULL AFTER user_id;
UPDATE medical_chronic_diseases r JOIN medical_profile p ON p.user_id = r.user_id AND p.relation = 'SELF' SET r.profile_id = p.id;
ALTER TABLE medical_chronic_diseases
  MODIFY profile_id BIGINT NOT NULL,
  ADD INDEX idx_medical_chronic_diseases_user (user_id),
  ADD INDEX idx_medical_chronic_diseases_profile (profile_id),
  ADD CONSTRAINT fk_medical_chronic_diseases_profile FOREIGN KEY (profile_id) REFERENCES medical_profile (id) ON DELETE CASCADE;

ALTER TABLE medical_lab_results ADD COLUMN profile_id BIGINT NULL AFTER user_id;
UPDATE medical_lab_results r JOIN medical_profile p ON p.user_id = r.user_id AND p.relation = 'SELF' SET r.profile_id = p.id;
ALTER TABLE medical_lab_results
  MODIFY profile_id BIGINT NOT NULL,
  ADD INDEX idx_medical_lab_results_user (user_id),
  ADD INDEX idx_medical_lab_results_profile (profile_id),
  ADD CONSTRAINT fk_medical_lab_results_profile FOREIGN KEY (profile_id) REFERENCES medical_profile (id) ON DELETE CASCADE;

ALTER TABLE medical_vaccines ADD COLUMN profile_id BIGINT NULL AFTER user_id;
UPDATE medical_vaccines r JOIN medical_profile p ON p.user_id = r.user_id AND p.relation = 'SELF' SET r.profile_id = p.id;
ALTER TABLE medical_vaccines
  MODIFY profile_id BIGINT NOT NULL,
  ADD INDEX idx_medical_vaccines_user (user_id),
  ADD INDEX idx_medical_vaccines_profile (profile_id),
  ADD CONSTRAINT fk_medical_vaccines_profile FOREIGN KEY (profile_id) REFERENCES medical_profile (id) ON DELETE CASCADE;

ALTER TABLE medical_family_history ADD COLUMN profile_id BIGINT NULL AFTER user_id;
UPDATE medical_family_history r JOIN medical_profile p ON p.user_id = r.user_id AND p.relation = 'SELF' SET r.profile_id = p.id;
ALTER TABLE medical_family_history
  MODIFY profile_id BIGINT NOT NULL,
  ADD INDEX idx_medical_family_history_user (user_id),
  ADD INDEX idx_medical_family_history_profile (profile_id),
  ADD CONSTRAINT fk_medical_family_history_profile FOREIGN KEY (profile_id) REFERENCES medical_profile (id) ON DELETE CASCADE;

ALTER TABLE medical_medications MODIFY name TEXT NOT NULL, MODIFY dosage TEXT NULL, MODIFY frequency TEXT NULL;
ALTER TABLE medical_allergies MODIFY allergen TEXT NOT NULL, MODIFY reaction TEXT NULL;
ALTER TABLE medical_blood_type MODIFY blood_type TEXT NOT NULL;
ALTER TABLE medical_surgeries MODIFY surgery_name TEXT NOT NULL, MODIFY hospital TEXT NULL, MODIFY surgeon TEXT NULL;
ALTER TABLE medical_chronic_diseases MODIFY disease_name TEXT NOT NULL;
ALTER TABLE medical_lab_results MODIFY test_name TEXT NOT NULL, MODIFY result TEXT NULL, MODIFY unit TEXT NULL,
  MODIFY reference_range TEXT NULL;
ALTER TABLE medical_vaccines MODIFY vaccine_name TEXT NOT NULL;
ALTER TABLE medical_family_history MODIFY condition_name TEXT NOT NULL, MODIFY relation TEXT NULL;

UPDATE medical_allergies SET severity = CASE LOWER(TRIM(severity))
    WHEN 'mild' THEN 'MILD' WHEN 'low' THEN 'MILD'
    WHEN 'moderate' THEN 'MODERATE' WHEN 'medium' THEN 'MODERATE'
    WHEN 'severe' THEN 'SEVERE' WHEN 'high' THEN 'SEVERE'
    ELSE severity END
  WHERE severity IS NOT NULL;
UPDATE medical_allergies SET notes = CONCAT_WS('\n', notes, severity), severity = NULL
  WHERE severity IS NOT NULL AND severity NOT IN ('MILD', 'MODERATE', 'SEVERE', 'LIFE_THREATENING');

CREATE TABLE medical_emergency_contacts (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  user_id           BIGINT       NOT NULL,
  profile_id        BIGINT       NOT NULL,
  contact_name      TEXT         NOT NULL,
  phone             TEXT         NOT NULL,
  contact_relation  TEXT         NULL,
  notes             TEXT         NULL,
  recorded_at       DATETIME(6)  NOT NULL,
  created_at        DATETIME(6)  NOT NULL,
  updated_at        DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  KEY idx_medical_emergency_contacts_user (user_id),
  KEY idx_medical_emergency_contacts_profile (profile_id),
  CONSTRAINT fk_medical_emergency_contacts_profile FOREIGN KEY (profile_id) REFERENCES medical_profile (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Who changed what. No medical content, so it needs no encryption.
CREATE TABLE medical_audit (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL,
  profile_id  BIGINT       NULL,
  action      VARCHAR(10)  NOT NULL,
  category    VARCHAR(32)  NOT NULL,
  record_id   BIGINT       NULL,
  request_id  VARCHAR(64)  NULL,
  created_at  DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  KEY idx_medical_audit_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
