-- =====================================================
-- ADD DELIVERY PAYMENT METHODS - Separate from Restaurant Payment Methods
-- =====================================================
-- Purpose: Add separate payment method configuration for delivery orders
-- This allows the restaurant to disable payment methods ONLY for delivery
-- without affecting the restaurant's in-house payment options
-- =====================================================

-- Create table for delivery payment methods (separate from restaurant payment methods)
CREATE TABLE IF NOT EXISTS system_delivery_payment_methods (
    system_configuration_id BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    payment_method_type VARCHAR(50) NOT NULL,
    PRIMARY KEY (system_configuration_id, payment_method_type),
    CONSTRAINT fk_delivery_payment_system_config 
        FOREIGN KEY (system_configuration_id) 
        REFERENCES system_configuration(id)
        ON DELETE CASCADE
);

-- Insert default delivery payment methods (all enabled by default)
-- Only CASH will be typically enabled for delivery, but we enable all by default
INSERT INTO system_delivery_payment_methods (system_configuration_id, enabled, payment_method_type)
SELECT id, TRUE, 'CASH' FROM system_configuration 
WHERE NOT EXISTS (
    SELECT 1 FROM system_delivery_payment_methods 
    WHERE system_configuration_id = system_configuration.id 
    AND payment_method_type = 'CASH'
);

INSERT INTO system_delivery_payment_methods (system_configuration_id, enabled, payment_method_type)
SELECT id, FALSE, 'CREDIT_CARD' FROM system_configuration 
WHERE NOT EXISTS (
    SELECT 1 FROM system_delivery_payment_methods 
    WHERE system_configuration_id = system_configuration.id 
    AND payment_method_type = 'CREDIT_CARD'
);

INSERT INTO system_delivery_payment_methods (system_configuration_id, enabled, payment_method_type)
SELECT id, FALSE, 'DEBIT_CARD' FROM system_configuration 
WHERE NOT EXISTS (
    SELECT 1 FROM system_delivery_payment_methods 
    WHERE system_configuration_id = system_configuration.id 
    AND payment_method_type = 'DEBIT_CARD'
);

INSERT INTO system_delivery_payment_methods (system_configuration_id, enabled, payment_method_type)
SELECT id, FALSE, 'TRANSFER' FROM system_configuration 
WHERE NOT EXISTS (
    SELECT 1 FROM system_delivery_payment_methods 
    WHERE system_configuration_id = system_configuration.id 
    AND payment_method_type = 'TRANSFER'
);

-- =====================================================
-- Verification queries
-- =====================================================

-- Check delivery payment methods
SELECT * FROM system_delivery_payment_methods;

-- Compare with restaurant payment methods
SELECT 'Restaurant' as type, payment_method_type, enabled 
FROM system_payment_methods 
UNION ALL
SELECT 'Delivery' as type, payment_method_type, enabled 
FROM system_delivery_payment_methods;
