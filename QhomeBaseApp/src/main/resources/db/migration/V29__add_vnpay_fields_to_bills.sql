-- Add new columns for VNPAY transaction tracking
ALTER TABLE qhomebaseapp.bills
ADD COLUMN vnp_transaction_no VARCHAR(50),
ADD COLUMN vnp_bank_code VARCHAR(50),
ADD COLUMN vnp_card_type VARCHAR(50),
ADD COLUMN vnp_pay_date TIMESTAMP,
ADD COLUMN payment_gateway VARCHAR(30) DEFAULT 'VNPAY';