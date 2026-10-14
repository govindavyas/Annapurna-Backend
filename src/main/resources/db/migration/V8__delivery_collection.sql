ALTER TABLE branches ADD COLUMN delivery_qr_image_url VARCHAR(500);
ALTER TABLE orders ADD COLUMN collection_method VARCHAR(20);
ALTER TABLE orders ADD COLUMN collected_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE orders ADD COLUMN collected_by UUID REFERENCES admin_accounts(id);
