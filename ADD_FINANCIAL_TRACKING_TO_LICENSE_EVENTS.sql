-- ============================================
-- ADD FINANCIAL TRACKING TO LICENSE EVENTS
-- Agrega columnas para rastrear montos y meses en eventos de licencia
-- ============================================

-- Agregar columna para el monto de la renovación
ALTER TABLE license_events 
ADD COLUMN IF NOT EXISTS amount DECIMAL(10,2) COMMENT 'Monto de la renovación en MXN';

-- Agregar columna para los meses de la renovación (puede ser negativo)
ALTER TABLE license_events 
ADD COLUMN IF NOT EXISTS months INT COMMENT 'Meses agregados o restados (positivo o negativo)';

-- Crear índice para consultas de suma de montos
CREATE INDEX IF NOT EXISTS idx_license_events_amount 
ON license_events(license_id, event_type, amount);

SELECT 'Columnas amount y months agregadas exitosamente a license_events' AS resultado;
