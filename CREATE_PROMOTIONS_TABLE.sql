-- =====================================================
-- Create Promotions Table
-- =====================================================
-- This table stores promotional offers for menu items
-- Supports three types: Buy X Pay Y, Percentage Discount, and Fixed Amount Discount
-- =====================================================

CREATE TABLE IF NOT EXISTS promotions (
    id_promotion BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Basic Information
    name VARCHAR(200) NOT NULL,
    description TEXT,
    
    -- Promotion Type
    promotion_type VARCHAR(30) NOT NULL,
    -- Valid values: 'BUY_X_PAY_Y', 'PERCENTAGE_DISCOUNT', 'FIXED_AMOUNT_DISCOUNT'
    
    -- Discount Configuration (fields used depend on promotion_type)
    buy_quantity INT,                        -- For BUY_X_PAY_Y: quantity to buy (X)
    pay_quantity INT,                        -- For BUY_X_PAY_Y: quantity to pay for (Y)
    discount_percentage DECIMAL(5,2),        -- For PERCENTAGE_DISCOUNT: percentage (0-100)
    discount_amount DECIMAL(10,2),           -- For FIXED_AMOUNT_DISCOUNT: fixed amount
    
    -- Validity Period
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    valid_days VARCHAR(100) NOT NULL,        -- Comma-separated: MONDAY,FRIDAY,SATURDAY
    
    -- Status and Priority
    active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 1,         -- Higher = higher priority
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes
    INDEX idx_promotion_type (promotion_type),
    INDEX idx_promotion_active (active),
    INDEX idx_promotion_dates (start_date, end_date),
    INDEX idx_promotion_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Add constraints
-- =====================================================

-- Ensure start_date is before end_date
ALTER TABLE promotions
ADD CONSTRAINT chk_promotion_dates 
CHECK (start_date <= end_date);

-- Ensure buy_quantity > pay_quantity for BUY_X_PAY_Y type
ALTER TABLE promotions
ADD CONSTRAINT chk_buy_x_pay_y 
CHECK (
    promotion_type != 'BUY_X_PAY_Y' 
    OR (buy_quantity IS NOT NULL AND pay_quantity IS NOT NULL AND buy_quantity > pay_quantity AND buy_quantity > 0 AND pay_quantity > 0)
);

-- Ensure discount_percentage is valid for PERCENTAGE_DISCOUNT type
ALTER TABLE promotions
ADD CONSTRAINT chk_percentage_discount 
CHECK (
    promotion_type != 'PERCENTAGE_DISCOUNT' 
    OR (discount_percentage IS NOT NULL AND discount_percentage > 0 AND discount_percentage <= 100)
);

-- Ensure discount_amount is valid for FIXED_AMOUNT_DISCOUNT type
ALTER TABLE promotions
ADD CONSTRAINT chk_fixed_discount 
CHECK (
    promotion_type != 'FIXED_AMOUNT_DISCOUNT' 
    OR (discount_amount IS NOT NULL AND discount_amount > 0)
);

-- =====================================================
-- Comments
-- =====================================================

ALTER TABLE promotions COMMENT = 'Promotional offers for menu items with various discount types';
