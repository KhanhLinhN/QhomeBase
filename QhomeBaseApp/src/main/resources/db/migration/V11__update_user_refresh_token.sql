-- ==============================
-- V11: Update schema for users & refresh_tokens
-- ==============================

-- ------------------------------
-- Users table
-- ------------------------------
CREATE TABLE IF NOT EXISTS qhomebaseapp.users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    reset_otp VARCHAR(20),
    otp_expiry TIMESTAMP,
    phone_number VARCHAR(20),
    created_at TIMESTAMP DEFAULT now() NOT NULL,
    updated_at TIMESTAMP DEFAULT now() NOT NULL
);

-- Indexes for fast lookup
CREATE INDEX IF NOT EXISTS idx_users_email ON qhomebaseapp.users(email);

-- ------------------------------
-- Refresh tokens table
-- ------------------------------
CREATE TABLE IF NOT EXISTS qhomebaseapp.refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES qhomebaseapp.users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT now() NOT NULL
);

-- Indexes for fast lookup
CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id ON qhomebaseapp.refresh_tokens(user_id);

-- ------------------------------
-- Trigger to update updated_at automatically
-- ------------------------------
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
   NEW.updated_at = now();
   RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_update_users_updated_at ON qhomebaseapp.users;
CREATE TRIGGER trigger_update_users_updated_at
BEFORE UPDATE ON qhomebaseapp.users
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
