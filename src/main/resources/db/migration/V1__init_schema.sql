CREATE TABLE menu_items (
    id UUID PRIMARY KEY,
    category VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(255) NOT NULL,
    price_paise INTEGER NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    customer_name VARCHAR(120) NOT NULL,
    customer_phone VARCHAR(30) NOT NULL,
    delivery_address VARCHAR(255) NOT NULL,
    subtotal_paise INTEGER NOT NULL DEFAULT 0,
    delivery_fee_paise INTEGER NOT NULL DEFAULT 0,
    total_paise INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    payment_mode VARCHAR(30) NOT NULL,
    payment_status VARCHAR(30) NOT NULL,
    cancel_reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    menu_item_id UUID NOT NULL REFERENCES menu_items(id),
    quantity INTEGER NOT NULL,
    unit_price_paise INTEGER NOT NULL,
    line_total_paise INTEGER NOT NULL
);

CREATE INDEX idx_orders_status_created_at ON orders(status, created_at DESC);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
