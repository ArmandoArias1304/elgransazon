-- ==========================================
-- MIGRATION: Replace email with edad in Employee table
-- Description: Remove email field and add edad (age) field
-- Date: 2025
-- ==========================================

-- Step 1: Add edad column (nullable, no default)
ALTER TABLE employee 
ADD COLUMN edad INT NULL 
COMMENT 'Age of the employee (between 18-100 years)';

-- Step 2: Drop email column and its unique constraint
ALTER TABLE employee 
DROP COLUMN email;

-- ==========================================
-- Notes:
-- - All existing employees will have edad = NULL after migration
-- - Edad is optional (nullable) unlike email which was required
-- - Age validation (18-100) is enforced at application level via @Min/@Max annotations
-- - No unique constraint on edad (multiple employees can have same age)
-- - Admin/Manager should update employee ages after migration
-- ==========================================

-- Verification queries (run after migration):
-- SELECT idEmpleado, fullName, username, edad, telefono FROM employee;
-- SELECT COUNT(*) as total_employees, COUNT(edad) as employees_with_edad FROM employee;
