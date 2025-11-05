-- V46__create_resident_card_registration_table.sql
-- Create separate table for Resident Card registration service (Dịch vụ ra vào)

CREATE TABLE IF NOT EXISTS qhomebaseapp.resident_card_registration (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    resident_name VARCHAR(255) NOT NULL,
    apartment_number VARCHAR(50) NOT NULL,
    building_name VARCHAR(255) NOT NULL,
    citizen_id VARCHAR(50) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    note TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    payment_amount DECIMAL(15, 2) DEFAULT 30000,
    payment_date TIMESTAMP WITH TIME ZONE,
    payment_gateway VARCHAR(50),
    vnpay_transaction_ref VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_resident_card_user
        FOREIGN KEY (user_id)
        REFERENCES qhomebaseapp.users (id)
        ON DELETE CASCADE
);

-- Add indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_resident_card_user_id 
    ON qhomebaseapp.resident_card_registration(user_id);

CREATE INDEX IF NOT EXISTS idx_resident_card_payment_status 
    ON qhomebaseapp.resident_card_registration(payment_status);

CREATE INDEX IF NOT EXISTS idx_resident_card_status 
    ON qhomebaseapp.resident_card_registration(status);

