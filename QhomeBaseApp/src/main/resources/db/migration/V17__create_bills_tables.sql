-- V17__create_billing_tables.sql

CREATE TABLE IF NOT EXISTS qhomebaseapp.bills (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES qhomebaseapp.users(id) ON DELETE CASCADE,
    bill_type VARCHAR(50) NOT NULL,          -- ELECTRICITY, WATER, INTERNET, etc.
    amount DECIMAL(12, 2) NOT NULL,
    billing_month DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNPAID', -- UNPAID / PAID
    description TEXT,
    payment_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_bills_user_id ON qhomebaseapp.bills(user_id);
CREATE INDEX idx_bills_month ON qhomebaseapp.bills(billing_month);
