-- Migration: Create employee_monthly_stats table for tracking monthly performance
-- Purpose: Track waiter sales and chef orders per month for "Employee of the Month" feature
-- Author: System
-- Date: 2025-12-20

CREATE TABLE IF NOT EXISTS employee_monthly_stats (
    id_stat BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    month INT NOT NULL COMMENT 'Month number (1-12)',
    year INT NOT NULL COMMENT 'Year (e.g., 2025)',
    total_sales DECIMAL(10, 2) DEFAULT 0.00 COMMENT 'Total sales for waiters (sum of order totals without tip)',
    total_orders INT DEFAULT 0 COMMENT 'Total orders completed for chefs',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key to employee table
    CONSTRAINT fk_employee_monthly_stats_employee 
        FOREIGN KEY (employee_id) 
        REFERENCES employee(id_empleado) 
        ON DELETE CASCADE,
    
    -- Unique constraint: one record per employee per month/year
    CONSTRAINT uk_employee_month_year 
        UNIQUE (employee_id, month, year),
    
    -- Check constraints for valid month and year
    CONSTRAINT chk_valid_month CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT chk_valid_year CHECK (year >= 2020 AND year <= 2100),
    CONSTRAINT chk_non_negative_sales CHECK (total_sales >= 0),
    CONSTRAINT chk_non_negative_orders CHECK (total_orders >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Tracks monthly statistics for employees (waiter sales and chef orders)';

-- Create indexes for better query performance
CREATE INDEX idx_employee_monthly_stats_month_year 
    ON employee_monthly_stats(month, year);

CREATE INDEX idx_employee_monthly_stats_employee 
    ON employee_monthly_stats(employee_id);

CREATE INDEX idx_employee_monthly_stats_total_sales 
    ON employee_monthly_stats(total_sales DESC);

CREATE INDEX idx_employee_monthly_stats_total_orders 
    ON employee_monthly_stats(total_orders DESC);

-- Insert initial records for current month for all active employees
INSERT INTO employee_monthly_stats (employee_id, month, year, total_sales, total_orders)
SELECT 
    e.id_empleado,
    MONTH(CURRENT_DATE) as month,
    YEAR(CURRENT_DATE) as year,
    0.00 as total_sales,
    0 as total_orders
FROM employee e
WHERE e.enabled = TRUE
ON DUPLICATE KEY UPDATE 
    employee_id = employee_id; -- No-op to avoid errors if records already exist
