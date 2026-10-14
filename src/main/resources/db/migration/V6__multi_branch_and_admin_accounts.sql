CREATE TABLE branches (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    address VARCHAR(255) NOT NULL,
    shop_open BOOLEAN NOT NULL,
    delivery_radius_km INTEGER NOT NULL,
    shop_latitude DOUBLE PRECISION,
    shop_longitude DOUBLE PRECISION,
    banner_title VARCHAR(120) NOT NULL,
    banner_subtitle VARCHAR(180) NOT NULL,
    banner_image_url VARCHAR(500),
    offer_enabled BOOLEAN NOT NULL,
    offer_title VARCHAR(100),
    offer_description VARCHAR(220),
    offer_code VARCHAR(40),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO branches (
    id,
    code,
    name,
    address,
    shop_open,
    delivery_radius_km,
    shop_latitude,
    shop_longitude,
    banner_title,
    banner_subtitle,
    banner_image_url,
    offer_enabled,
    offer_title,
    offer_description,
    offer_code,
    created_at,
    updated_at
)
SELECT
    '11111111-1111-1111-1111-111111111111',
    'HQ',
    shop_name,
    shop_name,
    shop_open,
    delivery_radius_km,
    shop_latitude,
    shop_longitude,
    banner_title,
    banner_subtitle,
    banner_image_url,
    offer_enabled,
    offer_title,
    offer_description,
    offer_code,
    updated_at,
    updated_at
FROM store_settings
WHERE id = 1;

ALTER TABLE orders ADD COLUMN branch_id UUID;

UPDATE orders
SET branch_id = '11111111-1111-1111-1111-111111111111'
WHERE branch_id IS NULL;

ALTER TABLE orders ALTER COLUMN branch_id SET NOT NULL;
ALTER TABLE orders ADD CONSTRAINT fk_orders_branch FOREIGN KEY (branch_id) REFERENCES branches(id);

CREATE INDEX idx_orders_branch_created_at ON orders(branch_id, created_at DESC);

CREATE TABLE admin_accounts (
    id UUID PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    totp_secret VARCHAR(120) NOT NULL,
    role VARCHAR(40) NOT NULL,
    branch_id UUID REFERENCES branches(id),
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_admin_accounts_branch_id ON admin_accounts(branch_id);
