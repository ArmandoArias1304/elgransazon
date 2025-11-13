-- =====================================================
-- SQL Script: Add Customer Support to Orders System
-- =====================================================
-- This script adds customer management functionality
-- allowing customers to create TAKEOUT and DELIVERY orders
-- =====================================================

-- Step 1: Create customers table
CREATE TABLE IF NOT EXISTS customers (
    id_customer BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(200) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    address VARCHAR(500),
    password VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    last_access TIMESTAMP NULL,
    INDEX idx_customer_email (email),
    INDEX idx_customer_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 2: Add customer relationship to orders table
ALTER TABLE orders
ADD COLUMN id_customer BIGINT NULL AFTER id_employee,
ADD CONSTRAINT fk_orders_customer 
    FOREIGN KEY (id_customer) REFERENCES customers(id_customer)
    ON DELETE SET NULL
    ON UPDATE CASCADE;

-- Step 3: Make id_employee nullable (for customer-created orders)
ALTER TABLE orders
MODIFY COLUMN id_employee BIGINT NULL;

-- Step 4: Add index for customer orders lookup
CREATE INDEX idx_orders_customer ON orders(id_customer);

-- Step 5: Insert ROLE_CLIENT into roles table if not exists
INSERT INTO roles (nombre_rol) 
SELECT 'ROLE_CLIENT'
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE nombre_rol = 'ROLE_CLIENT'
);

-- =====================================================
-- Verification Queries
-- =====================================================

-- Verify customers table structure
DESCRIBE customers;

-- Verify orders table has customer column
DESCRIBE orders;

-- Verify ROLE_CLIENT exists
SELECT * FROM roles WHERE nombre_rol = 'ROLE_CLIENT';

-- =====================================================
-- Sample Customer Data (Optional - for testing)
-- =====================================================

-- Insert sample customer (password is BCrypt hash of "password123")
-- INSERT INTO customers (full_name, email, phone, address, password, active)
-- VALUES (
--     'Juan Pérez',
--     'juan.perez@email.com',
--     '555-0123',
--     'Calle Principal 123, Ciudad',
--     '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8v3.z5z5z5z5z5z5z5z5z5z5z5z5z5',
--     TRUE
-- );

-- =====================================================
-- Rollback Script (if needed)
-- =====================================================

-- To rollback these changes, execute:
-- ALTER TABLE orders DROP FOREIGN KEY fk_orders_customer;
-- ALTER TABLE orders DROP COLUMN id_customer;
-- ALTER TABLE orders MODIFY COLUMN id_employee BIGINT NOT NULL;
-- DROP TABLE customers;
-- DELETE FROM roles WHERE nombre_rol = 'ROLE_CLIENT';
