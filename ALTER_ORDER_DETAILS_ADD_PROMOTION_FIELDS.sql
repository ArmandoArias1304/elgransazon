-- =====================================================
-- Alter Order Details Table - Add Promotion Fields
-- =====================================================
-- Adds fields to track promotional pricing in order details
-- =====================================================

-- Add promotion_applied_price column
-- This stores the discounted price per unit when a promotion is applied
ALTER TABLE order_details
ADD COLUMN promotion_applied_price DECIMAL(10,2) NULL DEFAULT NULL
COMMENT 'Price per unit with promotion applied (NULL if no promotion)';

-- Add applied_promotion_id column
-- This tracks which promotion was applied to this order detail
ALTER TABLE order_details
ADD COLUMN applied_promotion_id BIGINT NULL DEFAULT NULL
COMMENT 'ID of the promotion applied to this item (NULL if no promotion)';

-- Add index for better query performance
ALTER TABLE order_details
ADD INDEX idx_order_details_promotion (applied_promotion_id);

-- Optional: Add foreign key constraint (if you want referential integrity)
-- Note: This will prevent deleting promotions that have been used in orders
-- Comment this out if you want to allow deleting old promotions
ALTER TABLE order_details
ADD CONSTRAINT fk_order_details_promotion 
    FOREIGN KEY (applied_promotion_id) 
    REFERENCES promotions(id_promotion) 
    ON DELETE SET NULL;

-- =====================================================
-- Verification Query
-- =====================================================
-- Run this to verify the columns were added successfully:
-- SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT 
-- FROM INFORMATION_SCHEMA.COLUMNS 
-- WHERE TABLE_SCHEMA = 'elgransazon' 
-- AND TABLE_NAME = 'order_details' 
-- AND COLUMN_NAME IN ('promotion_applied_price', 'applied_promotion_id');
