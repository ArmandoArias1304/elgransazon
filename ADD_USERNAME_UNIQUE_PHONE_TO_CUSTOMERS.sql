-- Migration: Add username field and unique constraint for phone to customers table
-- Date: 2024-11-11
-- Description: Adds username field (unique, not null) and makes phone unique in customers table

-- Step 1: Add username column (nullable at first)
ALTER TABLE customers 
ADD COLUMN username VARCHAR(50) NULL AFTER id_customer;

-- Step 2: Generate temporary usernames for existing customers
-- Format: customer_<id>
UPDATE customers 
SET username = CONCAT('customer_', id_customer)
WHERE username IS NULL;

-- Step 3: Make username NOT NULL and add UNIQUE constraint
ALTER TABLE customers 
MODIFY COLUMN username VARCHAR(50) NOT NULL,
ADD CONSTRAINT uk_customers_username UNIQUE (username);

-- Step 4: Add UNIQUE constraint to phone
ALTER TABLE customers 
ADD CONSTRAINT uk_customers_phone UNIQUE (phone);

-- Verify changes
DESCRIBE customers;

-- Show sample data
SELECT id_customer, username, full_name, email, phone, active 
FROM customers 
LIMIT 5;
