-- =====================================================
-- ADD BARISTA ROLE SUPPORT
-- =====================================================
-- This migration adds support for the BARISTA role
-- Created: 2025-12-23
-- =====================================================

-- 1. Add new column to item_menu table for barista preparation requirement
ALTER TABLE item_menu 
ADD COLUMN requires_barista_preparation BOOLEAN NOT NULL DEFAULT FALSE
COMMENT 'Indicates if this item requires preparation by a barista (e.g., coffee, espresso, smoothies)';

-- 2. Add new column to orders table for barista who prepared items
ALTER TABLE orders 
ADD COLUMN id_prepared_by_barista BIGINT NULL
COMMENT 'Employee (barista) who prepared beverages/coffee items in this order';

-- 3. Add foreign key constraint
ALTER TABLE orders 
ADD CONSTRAINT fk_orders_prepared_by_barista 
FOREIGN KEY (id_prepared_by_barista) REFERENCES employees(id_employee)
ON DELETE SET NULL;

-- 4. Create index for better query performance
CREATE INDEX idx_orders_prepared_by_barista ON orders(id_prepared_by_barista);
CREATE INDEX idx_item_menu_requires_barista_prep ON item_menu(requires_barista_preparation);

-- 5. Insert BARISTA role if not exists
INSERT INTO roles (nombre_rol) 
SELECT 'ROLE_BARISTA' 
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE nombre_rol = 'ROLE_BARISTA'
);

-- =====================================================
-- NOTES:
-- - Items can require both chef AND barista preparation
-- - Example: A combo with burger + coffee requires both
-- - Baristas will only see orders with requiresBaristaPreparation items
-- - The BARISTA role shares the same interface as CHEF
-- =====================================================
