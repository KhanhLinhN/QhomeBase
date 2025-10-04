CREATE TABLE IF NOT EXISTS qhomebaseapp.register_service_request (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    service_type VARCHAR(100) NOT NULL,
    note TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES qhomebaseapp.users(id)
);
