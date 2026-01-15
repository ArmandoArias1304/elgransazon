-- Migration: Create customer_addresses table for multiple delivery addresses with map coordinates
-- Date: 2026-01-14

CREATE TABLE IF NOT EXISTS customer_addresses (
    id_address BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_customer BIGINT NOT NULL,
    label VARCHAR(100) NOT NULL COMMENT 'Name for the address (Casa, Trabajo, etc.)',
    address VARCHAR(500) NOT NULL COMMENT 'Full text address',
    reference VARCHAR(300) NULL COMMENT 'Additional reference (Portón azul, etc.)',
    latitude DECIMAL(10, 8) NOT NULL COMMENT 'GPS latitude coordinate',
    longitude DECIMAL(11, 8) NOT NULL COMMENT 'GPS longitude coordinate',
    is_default BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Whether this is the default delivery address',
    active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Soft delete flag',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_customer_address_customer 
        FOREIGN KEY (id_customer) REFERENCES customers(id_customer)
        ON DELETE CASCADE ON UPDATE CASCADE,
    
    INDEX idx_customer_addresses_customer (id_customer),
    INDEX idx_customer_addresses_default (id_customer, is_default, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Migrate existing addresses from customers table to customer_addresses
-- This will create an entry for customers who already have an address
INSERT INTO customer_addresses (id_customer, label, address, latitude, longitude, is_default, active, created_at)
SELECT 
    id_customer,
    'Principal',
    address,
    0.0,  -- Default latitude (will need to be updated by user)
    0.0,  -- Default longitude (will need to be updated by user)
    TRUE,
    TRUE,
    COALESCE(created_at, NOW())
FROM customers 
WHERE address IS NOT NULL AND address != '';

-- Note: The 'address' column in customers table can be kept for backwards compatibility
-- or removed in a future migration after confirming all systems use customer_addresses
