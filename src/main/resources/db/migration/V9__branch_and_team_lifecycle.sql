ALTER TABLE branches ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE admin_accounts ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Require an explicit start of service after adopting the accepting-orders flow.
UPDATE branches SET shop_open = FALSE;
