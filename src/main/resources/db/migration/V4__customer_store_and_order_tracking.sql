CREATE TABLE customer_accounts (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    phone VARCHAR(30) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE customer_sessions (
    token VARCHAR(80) PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer_accounts(id) ON DELETE CASCADE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE store_settings (
    id INTEGER PRIMARY KEY,
    shop_name VARCHAR(120) NOT NULL,
    shop_open BOOLEAN NOT NULL,
    delivery_radius_km INTEGER NOT NULL,
    shop_latitude DOUBLE PRECISION,
    shop_longitude DOUBLE PRECISION,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO store_settings (id, shop_name, shop_open, delivery_radius_km, shop_latitude, shop_longitude, updated_at)
VALUES (1, 'Annapurna Dosa', TRUE, 5, 18.520430, 73.856743, NOW());

ALTER TABLE orders ADD COLUMN customer_id UUID REFERENCES customer_accounts(id);
ALTER TABLE orders ADD COLUMN delivery_latitude DOUBLE PRECISION;
ALTER TABLE orders ADD COLUMN delivery_longitude DOUBLE PRECISION;
ALTER TABLE orders ADD COLUMN reject_reason VARCHAR(255);

CREATE TABLE order_events (
    id BIGSERIAL PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    message VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_orders_customer_created_at ON orders(customer_id, created_at DESC);
CREATE INDEX idx_order_events_order_id_created_at ON order_events(order_id, created_at DESC);
