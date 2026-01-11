-- ============================================
-- ALTERNATIVA: Recrear columna event_type si es ENUM
-- ============================================

-- Si la columna se creó como ENUM en lugar de VARCHAR,
-- este script la convierte a VARCHAR para compatibilidad con JPA @Enumerated(EnumType.STRING)

USE elgransazon;

-- Primero, verificar el tipo actual
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    DATA_TYPE,
    CHARACTER_MAXIMUM_LENGTH
FROM 
    INFORMATION_SCHEMA.COLUMNS
WHERE 
    TABLE_SCHEMA = 'elgransazon' 
    AND TABLE_NAME = 'license_events' 
    AND COLUMN_NAME = 'event_type';

-- Si el resultado muestra que es ENUM, ejecutar este ALTER:
-- (Si es VARCHAR, no es necesario)

ALTER TABLE license_events 
MODIFY COLUMN event_type VARCHAR(50) NOT NULL;

-- Verificar que ahora es VARCHAR
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    DATA_TYPE,
    CHARACTER_MAXIMUM_LENGTH
FROM 
    INFORMATION_SCHEMA.COLUMNS
WHERE 
    TABLE_SCHEMA = 'elgransazon' 
    AND TABLE_NAME = 'license_events' 
    AND COLUMN_NAME = 'event_type';
