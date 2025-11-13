-- Script para resetear la contraseña del cliente con el nuevo username
-- Ejecutar este script para restaurar el acceso a la cuenta

-- Opción 1: Resetear la contraseña a "password123" (BCrypt hash)
-- Hash BCrypt de "password123"
UPDATE customers 
SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhka'
WHERE email = 'pablo@gmail.com';

-- Verificar el update
SELECT id_customer, username, email, full_name, active 
FROM customers 
WHERE email = 'pablo@gmail.com';

-- Ahora puedes iniciar sesión con:
-- Email: pablo@gmail.com o Username: tu_nuevo_username
-- Contraseña: password123
-- 
-- ⚠️ IMPORTANTE: Cambia tu contraseña inmediatamente después de iniciar sesión
