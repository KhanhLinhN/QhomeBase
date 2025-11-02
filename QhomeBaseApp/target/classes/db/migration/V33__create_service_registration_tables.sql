-- Flyway migration V33
-- Create tables for Service Registration feature

-- Service Category table
CREATE TABLE IF NOT EXISTS qhomebaseapp.service_category (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    icon VARCHAR(255), -- Icon name or URL
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Service table (specific services within a category)
CREATE TABLE IF NOT EXISTS qhomebaseapp.service (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES qhomebaseapp.service_category(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    location VARCHAR(500), -- Location description or map reference
    map_url VARCHAR(1000), -- Map image URL or embedded map
    price_per_hour DECIMAL(15,2),
    price_per_session DECIMAL(15,2),
    pricing_type VARCHAR(50) DEFAULT 'HOURLY', -- HOURLY, SESSION, FREE
    max_capacity INT, -- Maximum number of people
    min_duration_hours INT DEFAULT 1, -- Minimum booking duration in hours
    max_duration_hours INT, -- Maximum booking duration in hours
    advance_booking_days INT DEFAULT 30, -- How many days in advance can book
    rules TEXT, -- Rules and regulations
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_service_category FOREIGN KEY (category_id) REFERENCES qhomebaseapp.service_category(id)
);

-- Service Booking table
CREATE TABLE IF NOT EXISTS qhomebaseapp.service_booking (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL REFERENCES qhomebaseapp.service(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES qhomebaseapp.users(id) ON DELETE CASCADE,
    booking_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    duration_hours DECIMAL(5,2) NOT NULL,
    number_of_people INT DEFAULT 1,
    purpose TEXT, -- Purpose of use
    total_amount DECIMAL(15,2) NOT NULL,
    payment_status VARCHAR(20) DEFAULT 'UNPAID', -- UNPAID, PAID, PENDING, CANCELLED
    payment_date TIMESTAMP WITH TIME ZONE,
    payment_gateway VARCHAR(50), -- VNPAY
    vnpay_transaction_ref VARCHAR(255),
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED, COMPLETED, CANCELLED
    approved_by BIGINT REFERENCES qhomebaseapp.users(id),
    approved_at TIMESTAMP WITH TIME ZONE,
    rejection_reason TEXT,
    terms_accepted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_service_booking_service FOREIGN KEY (service_id) REFERENCES qhomebaseapp.service(id),
    CONSTRAINT fk_service_booking_user FOREIGN KEY (user_id) REFERENCES qhomebaseapp.users(id)
);

-- Service Availability Schedule (time slots availability)
CREATE TABLE IF NOT EXISTS qhomebaseapp.service_availability (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL REFERENCES qhomebaseapp.service(id) ON DELETE CASCADE,
    day_of_week INT NOT NULL, -- 0=Sunday, 1=Monday, ..., 6=Saturday
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_availability_service FOREIGN KEY (service_id) REFERENCES qhomebaseapp.service(id),
    CONSTRAINT unique_service_day_time UNIQUE (service_id, day_of_week, start_time, end_time)
);

-- Service Booking Time Slot (blocked/booked time slots)
CREATE TABLE IF NOT EXISTS qhomebaseapp.service_booking_slot (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES qhomebaseapp.service_booking(id) ON DELETE CASCADE,
    service_id BIGINT NOT NULL REFERENCES qhomebaseapp.service(id) ON DELETE CASCADE,
    slot_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_slot_booking FOREIGN KEY (booking_id) REFERENCES qhomebaseapp.service_booking(id),
    CONSTRAINT fk_slot_service FOREIGN KEY (service_id) REFERENCES qhomebaseapp.service(id)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_service_category_code ON qhomebaseapp.service_category(code);
CREATE INDEX IF NOT EXISTS idx_service_category_id ON qhomebaseapp.service(category_id);
CREATE INDEX IF NOT EXISTS idx_service_code ON qhomebaseapp.service(code);
CREATE INDEX IF NOT EXISTS idx_service_booking_service_id ON qhomebaseapp.service_booking(service_id);
CREATE INDEX IF NOT EXISTS idx_service_booking_user_id ON qhomebaseapp.service_booking(user_id);
CREATE INDEX IF NOT EXISTS idx_service_booking_date ON qhomebaseapp.service_booking(booking_date);
CREATE INDEX IF NOT EXISTS idx_service_availability_service_id ON qhomebaseapp.service_availability(service_id);
CREATE INDEX IF NOT EXISTS idx_service_booking_slot_service_date ON qhomebaseapp.service_booking_slot(service_id, slot_date);

-- Insert sample service categories
INSERT INTO qhomebaseapp.service_category(code, name, description, icon, sort_order) VALUES
('ENTERTAINMENT', 'Tiện ích giải trí', 'BBQ, hồ bơi, sân chơi', 'entertainment', 1),
('RENTAL', 'Dịch vụ thuê mặt bằng', 'Gian hàng lễ hội, sự kiện', 'rental', 2),
('TECHNICAL', 'Dịch vụ kỹ thuật', 'Sửa điện nước, bảo trì', 'technical', 3),
('OPERATION', 'Dịch vụ vận hành', 'Đăng ký xe, thẻ ra vào, chuyển nhượng', 'operation', 4)
ON CONFLICT (code) DO NOTHING;

-- Insert sample services for OPERATION category (only Vehicle Registration for now)
INSERT INTO qhomebaseapp.service(code, category_id, name, description, pricing_type, price_per_session, is_active)
SELECT 'VEHICLE_REGISTRATION', sc.id, 'Đăng ký xe', 'Đăng ký thẻ xe cho cư dân', 'SESSION', 30000.00, TRUE
FROM qhomebaseapp.service_category sc
WHERE sc.code = 'OPERATION'
ON CONFLICT (code) DO NOTHING;

