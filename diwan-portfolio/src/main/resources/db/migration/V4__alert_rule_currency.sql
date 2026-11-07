-- The currency of a price rule's threshold. NULL = the instrument's own currency. Gold rules are set in the
-- user's preferred currency and keep it, so changing the preference later does not change what a rule means.

ALTER TABLE alert_rule ADD COLUMN currency VARCHAR(3) NULL AFTER threshold;
