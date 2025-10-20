CREATE TABLE IF NOT EXISTS qhomebaseapp.register_service_image (
    id SERIAL PRIMARY KEY,
    register_request_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_register_request FOREIGN KEY (register_request_id)
        REFERENCES qhomebaseapp.register_service_request (id)
        ON DELETE CASCADE
);
ALTER TABLE qhomebaseapp.register_service_request
DROP COLUMN IF EXISTS image_url;