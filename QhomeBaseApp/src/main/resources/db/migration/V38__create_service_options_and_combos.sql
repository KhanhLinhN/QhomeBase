-- Flyway migration V38
-- Create tables for Service Options, Combos, Tickets, and Bar Slots

-- Service Options table (for BBQ: thịt, than, cồn, lửa, thuê thêm giờ)
CREATE TABLE IF NOT EXISTS qhomebaseapp.service_option (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL REFERENCES qhomebaseapp.service(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL, -- BBQ_MEAT, BBQ_COAL, BBQ_ALCOHOL, BBQ_FIRE, BBQ_EXTRA_HOUR
    name VARCHAR(255) NOT NULL, -- Tên option (ví dụ: "Mua hộ thịt", "Thêm than")
    description TEXT, -- Mô tả chi tiết
    price DECIMAL(15,2) NOT NULL DEFAULT 0, -- Giá option
    unit VARCHAR(50), -- Đơn vị (ví dụ: "kg", "giờ", "lần")
    is_required BOOLEAN DEFAULT FALSE, -- Bắt buộc phải chọn
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_service_option_service FOREIGN KEY (service_id) REFERENCES qhomebaseapp.service(id),
    CONSTRAINT unique_service_option_code UNIQUE (service_id, code)
);

-- Service Combo table (for SPA, Bar, Playground)
CREATE TABLE IF NOT EXISTS qhomebaseapp.service_combo (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL REFERENCES qhomebaseapp.service(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL, -- Mã combo (ví dụ: SPA_COMBO1, BAR_COMBO1)
    name VARCHAR(255) NOT NULL, -- Tên combo (ví dụ: "Chill 1", "Combo Massage")
    description TEXT, -- Mô tả combo
    services_included TEXT, -- Dịch vụ bao gồm (JSON hoặc text)
    duration_minutes INT, -- Thời lượng (phút)
    price DECIMAL(15,2) NOT NULL, -- Giá combo
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_service_combo_service FOREIGN KEY (service_id) REFERENCES qhomebaseapp.service(id),
    CONSTRAINT unique_service_combo_code UNIQUE (service_id, code)
);

-- Service Ticket table (for Pool, Playground)
CREATE TABLE IF NOT EXISTS qhomebaseapp.service_ticket (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL REFERENCES qhomebaseapp.service(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL, -- Mã vé (ví dụ: POOL_DAY, POOL_NIGHT, PLAYGROUND_1H)
    name VARCHAR(255) NOT NULL, -- Tên vé (ví dụ: "Vé ngày", "Vé 1 giờ")
    ticket_type VARCHAR(50) NOT NULL, -- DAY, NIGHT, HOURLY, DAILY, FAMILY
    duration_hours DECIMAL(5,2), -- Thời lượng (giờ)
    price DECIMAL(15,2) NOT NULL, -- Giá vé
    max_people INT, -- Số người tối đa (cho vé gia đình)
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_service_ticket_service FOREIGN KEY (service_id) REFERENCES qhomebaseapp.service(id),
    CONSTRAINT unique_service_ticket_code UNIQUE (service_id, code)
);

-- Bar Slot table (các slot giờ cố định cho Bar: 17:00-02:00)
CREATE TABLE IF NOT EXISTS qhomebaseapp.bar_slot (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL REFERENCES qhomebaseapp.service(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL, -- SLOT_1, SLOT_2, SLOT_3, SLOT_4, SLOT_5
    name VARCHAR(255) NOT NULL, -- Tên slot (ví dụ: "Giờ Happy Hour")
    start_time TIME NOT NULL, -- Giờ bắt đầu
    end_time TIME NOT NULL, -- Giờ kết thúc
    note TEXT, -- Ghi chú (ví dụ: "Giờ cao điểm")
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bar_slot_service FOREIGN KEY (service_id) REFERENCES qhomebaseapp.service(id),
    CONSTRAINT unique_bar_slot_code UNIQUE (service_id, code)
);

-- Service Booking Items table (lưu các option/combo/ticket đã chọn trong booking)
CREATE TABLE IF NOT EXISTS qhomebaseapp.service_booking_item (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES qhomebaseapp.service_booking(id) ON DELETE CASCADE,
    item_type VARCHAR(50) NOT NULL, -- OPTION, COMBO, TICKET
    item_id BIGINT NOT NULL, -- ID của option/combo/ticket
    item_code VARCHAR(50) NOT NULL, -- Mã của item (để dễ tra cứu)
    item_name VARCHAR(255) NOT NULL, -- Tên item
    quantity INT DEFAULT 1, -- Số lượng (ví dụ: 2kg than)
    unit_price DECIMAL(15,2) NOT NULL, -- Giá đơn vị
    total_price DECIMAL(15,2) NOT NULL, -- Tổng giá (quantity * unit_price)
    metadata TEXT, -- JSON metadata (ví dụ: dịch vụ bao gồm trong combo)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_item_booking FOREIGN KEY (booking_id) REFERENCES qhomebaseapp.service_booking(id)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_service_option_service_id ON qhomebaseapp.service_option(service_id);
CREATE INDEX IF NOT EXISTS idx_service_option_code ON qhomebaseapp.service_option(code);
CREATE INDEX IF NOT EXISTS idx_service_combo_service_id ON qhomebaseapp.service_combo(service_id);
CREATE INDEX IF NOT EXISTS idx_service_combo_code ON qhomebaseapp.service_combo(code);
CREATE INDEX IF NOT EXISTS idx_service_ticket_service_id ON qhomebaseapp.service_ticket(service_id);
CREATE INDEX IF NOT EXISTS idx_service_ticket_code ON qhomebaseapp.service_ticket(code);
CREATE INDEX IF NOT EXISTS idx_bar_slot_service_id ON qhomebaseapp.bar_slot(service_id);
CREATE INDEX IF NOT EXISTS idx_bar_slot_code ON qhomebaseapp.bar_slot(code);
CREATE INDEX IF NOT EXISTS idx_booking_item_booking_id ON qhomebaseapp.service_booking_item(booking_id);
CREATE INDEX IF NOT EXISTS idx_booking_item_type_id ON qhomebaseapp.service_booking_item(item_type, item_id);

