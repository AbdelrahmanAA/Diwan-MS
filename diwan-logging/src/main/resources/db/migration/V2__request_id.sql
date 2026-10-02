-- Correlation id assigned by the gateway (X-Request-Id), to follow one request across services.
ALTER TABLE `request_logs`
  ADD COLUMN `request_id` varchar(64) DEFAULT NULL,
  ADD KEY `idx_request_logs_request_id` (`request_id`);
