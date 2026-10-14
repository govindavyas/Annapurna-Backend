CREATE TABLE customer_push_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer_accounts (id),
    platform VARCHAR(24) NOT NULL,
    token VARCHAR(512) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customer_push_subscriptions_customer_active
    ON customer_push_subscriptions (customer_id, active);