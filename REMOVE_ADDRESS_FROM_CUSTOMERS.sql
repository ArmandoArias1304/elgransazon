-- ==============================================
-- Script: REMOVE_ADDRESS_FROM_CUSTOMERS.sql
-- Description: Elimina la columna address de la tabla customers
-- Date: 2026-01-14
-- ==============================================

-- Eliminar la columna address de la tabla customers
ALTER TABLE customers DROP COLUMN address;

-- Verificar que la columna fue eliminada
DESCRIBE customers;
