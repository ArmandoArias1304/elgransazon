-- ============================================
-- FIX: Actualizar columna event_type 
-- Problema: Data truncated for column 'event_type'
-- ============================================

-- Este script corrige el problema con la columna event_type
-- El error ocurre porque la columna puede tener restricciones o tipo incorrecto

USE elgransazon;

-- PASO 1: Ver la estructura actual de la tabla
DESCRIBE license_events;

-- PASO 2: Ver los valores actuales en la columna
SELECT DISTINCT event_type, LENGTH(event_type) as longitud 
FROM license_events 
ORDER BY event_type;

-- PASO 3: Modificar la columna para asegurar compatibilidad con todos los valores del enum
-- Cambiamos a VARCHAR(50) que es suficiente para 'NOTIFICATION_SENT' (18 caracteres)
ALTER TABLE license_events 
MODIFY COLUMN event_type VARCHAR(50) NOT NULL 
COMMENT 'CREATED, RENEWED, EXPIRED, SUSPENDED, REACTIVATED, UPDATED, CHECKED, NOTIFICATION_SENT';

-- PASO 4: Verificar que el cambio se aplicó correctamente
DESCRIBE license_events;

-- PASO 5: Intentar insertar un registro de prueba con UPDATED
-- (esto fallará si aún hay problemas, pero ayuda a diagnosticar)
-- INSERT INTO license_events (license_id, event_type, description, performed_by) 
-- VALUES (1, 'UPDATED', 'Prueba de evento UPDATED', 'test');

-- PASO 6: Ver todos los tipos de eventos registrados
SELECT event_type, COUNT(*) as cantidad 
FROM license_events 
GROUP BY event_type 
ORDER BY event_type;
