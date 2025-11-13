-- =====================================================
-- Script para agregar funcionalidad de verificación de email
-- y restablecimiento de contraseña
-- Fecha: 2024
-- =====================================================

-- 1. Agregar columna email_verified a la tabla customers (si no existe)
ALTER TABLE customers 
ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE
COMMENT 'Indica si el cliente ha verificado su correo electrónico';

-- 2. Crear tabla para tokens de verificación de email
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id_token BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    token VARCHAR(100) NOT NULL UNIQUE,
    expiration DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_email_verification_customer 
        FOREIGN KEY (customer_id) 
        REFERENCES customers(id_customer) 
        ON DELETE CASCADE,
    
    INDEX idx_token (token),
    INDEX idx_customer (customer_id),
    INDEX idx_expiration (expiration)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Tokens para verificación de correo electrónico de clientes';

-- 3. Crear tabla para tokens de restablecimiento de contraseña
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id_token BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    token VARCHAR(100) NOT NULL UNIQUE,
    expiration DATETIME NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_password_reset_customer 
        FOREIGN KEY (customer_id) 
        REFERENCES customers(id_customer) 
        ON DELETE CASCADE,
    
    INDEX idx_token (token),
    INDEX idx_customer (customer_id),
    INDEX idx_expiration (expiration),
    INDEX idx_used (used)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Tokens para restablecimiento de contraseña de clientes';

-- 4. Actualizar clientes existentes (OPCIONAL - solo si quieres que los clientes existentes estén verificados)
-- DESCOMENTAR LA SIGUIENTE LÍNEA SI QUIERES QUE LOS CLIENTES EXISTENTES ESTÉN AUTOMÁTICAMENTE VERIFICADOS
-- UPDATE customers SET email_verified = TRUE WHERE email_verified = FALSE;

-- =====================================================
-- Verificación de la estructura
-- =====================================================

-- Verificar que la columna se agregó correctamente
SELECT 
    COLUMN_NAME, 
    DATA_TYPE, 
    COLUMN_DEFAULT, 
    IS_NULLABLE,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'bd_restaurant' 
    AND TABLE_NAME = 'customers' 
    AND COLUMN_NAME = 'email_verified';

-- Verificar estructura de tabla de tokens de verificación
DESCRIBE email_verification_tokens;

-- Verificar estructura de tabla de tokens de reset
DESCRIBE password_reset_tokens;

-- Contar clientes verificados vs no verificados
SELECT 
    email_verified,
    COUNT(*) as total
FROM customers
GROUP BY email_verified;

-- =====================================================
-- FIN DEL SCRIPT
-- =====================================================
