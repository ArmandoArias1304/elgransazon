-- ============================================
-- SISTEMA DE LICENCIAS - ELGRANSAZON
-- Script de creación de tablas
-- ============================================

-- Tabla principal de licencia del sistema
CREATE TABLE IF NOT EXISTS system_license (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    license_key VARCHAR(255) UNIQUE NOT NULL COMMENT 'Clave única de la licencia',
    
    -- Tipo de paquete y facturación
    package_type VARCHAR(20) NOT NULL COMMENT 'BASIC, WEB, ECOMMERCE',
    billing_cycle VARCHAR(20) NOT NULL COMMENT 'MONTHLY, ANNUAL',
    
    -- Fechas importantes
    purchase_date DATE NOT NULL COMMENT 'Fecha de compra',
    expiration_date DATE NOT NULL COMMENT 'Fecha de vencimiento',
    installation_date DATE NOT NULL COMMENT 'Fecha de instalación',
    
    -- Estado de la licencia
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE, EXPIRED, TRIAL, SUSPENDED',
    
    -- Información del cliente (dueño del restaurante)
    owner_name VARCHAR(255) COMMENT 'Nombre del propietario',
    owner_email VARCHAR(255) COMMENT 'Email del propietario',
    owner_phone VARCHAR(20) COMMENT 'Teléfono del propietario',
    owner_rfc VARCHAR(50) COMMENT 'RFC del propietario',
    restaurant_name VARCHAR(255) COMMENT 'Nombre del restaurante',
    
    -- Límites según paquete
    max_users INT DEFAULT 5 COMMENT 'Número máximo de usuarios permitidos',
    max_branches INT DEFAULT 1 COMMENT 'Número máximo de sucursales',
    
    -- Información técnica
    version VARCHAR(50) DEFAULT '1.0.0' COMMENT 'Versión del sistema instalada',
    last_check_date DATE COMMENT 'Última verificación de la licencia',
    last_notification_sent DATE COMMENT 'Última notificación enviada',
    
    -- Notas internas del programador
    notes TEXT COMMENT 'Notas internas sobre este cliente',
    
    -- Auditoría
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de historial de eventos de la licencia
CREATE TABLE IF NOT EXISTS license_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    license_id BIGINT,
    event_type VARCHAR(50) NOT NULL COMMENT 'CREATED, RENEWED, EXPIRED, SUSPENDED, REACTIVATED, UPDATED, CHECKED, NOTIFICATION_SENT',
    event_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description TEXT COMMENT 'Descripción del evento',
    performed_by VARCHAR(255) COMMENT 'Usuario que realizó la acción',
    
    FOREIGN KEY (license_id) REFERENCES system_license(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de errores del sistema (para monitoreo)
CREATE TABLE IF NOT EXISTS system_errors (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    error_type VARCHAR(100) COMMENT 'Tipo de error',
    error_message TEXT COMMENT 'Mensaje de error',
    stack_trace TEXT COMMENT 'Stack trace completo',
    severity VARCHAR(20) DEFAULT 'MEDIUM' COMMENT 'LOW, MEDIUM, HIGH, CRITICAL',
    resolved BOOLEAN DEFAULT FALSE COMMENT 'Si el error fue resuelto',
    resolved_at TIMESTAMP NULL COMMENT 'Fecha de resolución',
    resolved_by VARCHAR(255) COMMENT 'Quién resolvió el error',
    occurred_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_severity (severity),
    INDEX idx_resolved (resolved),
    INDEX idx_occurred_at (occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índices para mejorar rendimiento
CREATE INDEX idx_license_expiration ON system_license(expiration_date);
CREATE INDEX idx_license_status ON system_license(status);
CREATE INDEX idx_license_key ON system_license(license_key);
CREATE INDEX idx_event_date ON license_events(event_date);

-- ============================================
-- DATOS DE EJEMPLO (OPCIONAL - COMENTAR/DESCOMENTAR SEGÚN NECESITES)
-- ============================================

-- Insertar licencia de ejemplo (Paquete E-Commerce, Mensual, 30 días)
-- INSERT INTO system_license (
--     license_key,
--     package_type,
--     billing_cycle,
--     purchase_date,
--     expiration_date,
--     installation_date,
--     status,
--     owner_name,
--     owner_email,
--     owner_phone,
--     owner_rfc,
--     restaurant_name,
--     max_users,
--     max_branches,
--     version
-- ) VALUES (
--     'ELGS-2026-DEMO-ABC123DEF456',
--     'ECOMMERCE',
--     'MONTHLY',
--     CURDATE(),
--     DATE_ADD(CURDATE(), INTERVAL 30 DAY),
--     CURDATE(),
--     'ACTIVE',
--     'Juan Pérez González',
--     'juan.perez@ejemplo.com',
--     '+52 555-123-4567',
--     'PEGJ850101XXX',
--     'Restaurante Demo',
--     5,
--     1,
--     '1.0.0'
-- );

-- Insertar evento inicial
-- INSERT INTO license_events (license_id, event_type, description, performed_by)
-- VALUES (1, 'CREATED', 'Licencia creada - Paquete ECOMMERCE MENSUAL', 'SYSTEM');

-- ============================================
-- AGREGAR ROL PROGRAMMER A TABLA DE EMPLEADOS
-- ============================================

-- Si usas VARCHAR para roles en la tabla employees
-- ALTER TABLE employees MODIFY COLUMN role VARCHAR(50);

-- Si usas ENUM, necesitas actualizar el enum (MySQL no permite agregarlo directamente en ALTER)
-- Opción 1: Cambiar a VARCHAR (recomendado)
-- ALTER TABLE employees MODIFY COLUMN role VARCHAR(50) NOT NULL;

-- Opción 2: Recrear el ENUM (más complejo, requiere proceso manual)
-- 1. Crear columna temporal
-- ALTER TABLE employees ADD COLUMN role_new VARCHAR(50);
-- 2. Copiar datos
-- UPDATE employees SET role_new = role;
-- 3. Eliminar columna antigua
-- ALTER TABLE employees DROP COLUMN role;
-- 4. Renombrar columna nueva
-- ALTER TABLE employees CHANGE role_new role VARCHAR(50) NOT NULL;

-- ============================================
-- CREAR USUARIO PROGRAMADOR (OPCIONAL)
-- ============================================

-- Insertar usuario programador (ajusta según tu estructura)
-- Contraseña: "programador123" (en BCrypt)
-- INSERT INTO employees (
--     nombre,
--     apellido,
--     username,
--     email,
--     password,
--     telefono,
--     role,
--     enabled,
--     fecha_registro
-- ) VALUES (
--     'Programador',
--     'Sistema',
--     'programador',
--     'tu_email@tudominio.com',
--     '$2a$10$YourBCryptHashedPasswordHere',
--     '555-0000',
--     'PROGRAMMER',
--     TRUE,
--     NOW()
-- );

-- ============================================
-- VERIFICACIÓN DE CREACIÓN
-- ============================================

-- Verificar que las tablas se crearon correctamente
-- SHOW TABLES LIKE 'system_%';
-- SHOW TABLES LIKE 'license_%';

-- Verificar estructura de system_license
-- DESCRIBE system_license;

-- Verificar estructura de license_events
-- DESCRIBE license_events;

-- Verificar estructura de system_errors
-- DESCRIBE system_errors;

-- ============================================
-- FIN DEL SCRIPT
-- ============================================
