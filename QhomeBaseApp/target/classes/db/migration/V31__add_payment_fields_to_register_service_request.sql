ALTER TABLE qhomebaseapp.register_service_request
ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20) DEFAULT 'UNPAID',
ADD COLUMN IF NOT EXISTS payment_amount DECIMAL(19,2),
ADD COLUMN IF NOT EXISTS payment_date TIMESTAMP WITH TIME ZONE,
ADD COLUMN IF NOT EXISTS payment_gateway VARCHAR(50),
ADD COLUMN IF NOT EXISTS vnpay_transaction_ref VARCHAR(255);

COMMENT ON COLUMN qhomebaseapp.register_service_request.payment_status IS 'Trạng thái thanh toán: UNPAID, PENDING, PAID';
COMMENT ON COLUMN qhomebaseapp.register_service_request.payment_amount IS 'Số tiền thanh toán (30.000 VNĐ cho mỗi thẻ xe)';
COMMENT ON COLUMN qhomebaseapp.register_service_request.payment_gateway IS 'Cổng thanh toán (VNPAY)';
COMMENT ON COLUMN qhomebaseapp.register_service_request.vnpay_transaction_ref IS 'Mã giao dịch VNPAY';

