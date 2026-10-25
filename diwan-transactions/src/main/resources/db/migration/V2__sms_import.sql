-- Transactions imported from bank SMS by the mobile app: where a row came from, and a per-user fingerprint of
-- the SMS so the same message is never stored twice (re-imports, the same SMS seen by receiver and inbox scan).
ALTER TABLE `transactions`
  ADD COLUMN `source` varchar(20) NOT NULL DEFAULT 'manual',
  ADD COLUMN `source_ref` varchar(64) DEFAULT NULL,
  ADD UNIQUE KEY `uk_user_source_ref` (`user_id`, `source_ref`);
