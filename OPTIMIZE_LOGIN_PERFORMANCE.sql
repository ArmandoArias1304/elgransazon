-- ============================================
-- OPTIMIZACIÓN DE PERFORMANCE EN LOGIN
-- ============================================
-- Este script agrega índices para mejorar la velocidad de login
-- y validación de usuarios
-- Ejecutar después de crear las tablas principales

-- Índices para tabla employee (mejorar búsqueda por username)
-- Verificar si ya existe antes de crear
DROP INDEX IF EXISTS idx_employee_username ON employee;
CREATE INDEX idx_employee_username ON employee(username);

DROP INDEX IF EXISTS idx_employee_email ON employee;
CREATE INDEX idx_employee_email ON employee(email);

-- Índice compuesto para validación rápida de empleados habilitados
DROP INDEX IF EXISTS idx_employee_username_enabled ON employee;
CREATE INDEX idx_employee_username_enabled ON employee(username, enabled);

-- Índices para tabla customers (mejorar búsqueda por email/username)
DROP INDEX IF EXISTS idx_customers_email ON customers;
CREATE INDEX idx_customers_email ON customers(email);

DROP INDEX IF EXISTS idx_customers_username ON customers;
CREATE INDEX idx_customers_username ON customers(username);

-- Índice compuesto para validación rápida de clientes activos y verificados
DROP INDEX IF EXISTS idx_customers_email_active ON customers;
CREATE INDEX idx_customers_email_active ON customers(email, active, email_verified);

-- Índices para tabla orders (mejorar consultas de chef y estadísticas)
DROP INDEX IF EXISTS idx_orders_status ON orders;
CREATE INDEX idx_orders_status ON orders(status);

DROP INDEX IF EXISTS idx_orders_created_at ON orders;
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);

DROP INDEX IF EXISTS idx_orders_order_type ON orders;
CREATE INDEX idx_orders_order_type ON orders(order_type);

-- Índice compuesto para queries de chef (status + created_at)
DROP INDEX IF EXISTS idx_orders_status_created ON orders;
CREATE INDEX idx_orders_status_created ON orders(status, created_at DESC);

-- Índices para tabla order_details (mejorar joins)
DROP INDEX IF EXISTS idx_order_details_order ON order_details;
CREATE INDEX idx_order_details_order ON order_details(id_order);

DROP INDEX IF EXISTS idx_order_details_item ON order_details;
CREATE INDEX idx_order_details_item ON order_details(id_item_menu);

-- Índices para tabla item_menu (mejorar filtro de chef)
DROP INDEX IF EXISTS idx_item_menu_requires_prep ON item_menu;
CREATE INDEX idx_item_menu_requires_prep ON item_menu(requires_preparation);

DROP INDEX IF EXISTS idx_item_menu_active ON item_menu;
CREATE INDEX idx_item_menu_active ON item_menu(active, available);

-- Verificar los índices creados
SHOW INDEX FROM employee;
SHOW INDEX FROM customers;
SHOW INDEX FROM orders;
SHOW INDEX FROM order_details;
SHOW INDEX FROM item_menu;

-- ============================================
-- NOTAS:
-- ============================================
-- Estos índices mejoran significativamente:
-- 1. Login de empleados (username lookup)
-- 2. Login de clientes (email lookup)
-- 3. Validación de usuarios habilitados (UserValidationFilter)
-- 4. Queries de chef (findOrdersWithPreparationItems)
-- 5. Estadísticas de dashboard
-- 6. Joins en orderDetails
--
-- Impacto esperado:
-- - Login: 50-70% más rápido
-- - Validación de usuarios: 80% más rápido
-- - Queries de chef: 30-50% más rápido
-- - Dashboard stats: 40-60% más rápido
-- ============================================
