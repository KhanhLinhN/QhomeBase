ALTER TABLE qhomebaseapp.users
ADD COLUMN role VARCHAR(50) NOT NULL DEFAULT 'RESIDENT';

ALTER TABLE qhomebaseapp.users
ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE qhomebaseapp.refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id)
        REFERENCES qhomebaseapp.users (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token_token ON qhomebaseapp.refresh_tokens (token);
