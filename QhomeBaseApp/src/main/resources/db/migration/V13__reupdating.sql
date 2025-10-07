-- ==============================
-- V11: Update existing schema with ALTER TABLE
-- ==============================

-- ------------------------------
-- Users table updates
-- ------------------------------
ALTER TABLE qhomebaseapp.users
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT now() NOT NULL;

ALTER TABLE qhomebaseapp.users
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT now() NOT NULL;

ALTER TABLE qhomebaseapp.users
    ADD COLUMN IF NOT EXISTS reset_otp VARCHAR(20);

ALTER TABLE qhomebaseapp.users
    ADD COLUMN IF NOT EXISTS otp_expiry TIMESTAMP;

ALTER TABLE qhomebaseapp.users
    ADD COLUMN IF NOT EXISTS enabled BOOLEAN DEFAULT TRUE;

-- ------------------------------
-- Refresh tokens table updates
-- ------------------------------
ALTER TABLE qhomebaseapp.refresh_tokens
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT now() NOT NULL;

-- ------------------------------
-- Trigger to update updated_at automatically for users
-- ------------------------------
CREATE OR REPLACE FUNCTION update_users_updated_at()
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
EXECUTE FUNCTION update_users_updated_at();
