-- ============================================
-- Script: Crear Rol PROGRAMMER y Usuario Programador
-- Descripción: Crea el rol PROGRAMMER en la tabla roles,
--              crea un usuario programador, y asigna el rol
-- Base de datos: bd_restaurant (o elgransazon)
-- ============================================

USE bd_restaurant;  -- Cambia a 'elgransazon' si ese es el nombre de tu BD

-- ============================================
-- 1. CREAR ROL PROGRAMMER
-- ============================================

-- Verificar si el rol ya existe antes de insertarlo
INSERT INTO roles (nombre_rol) 
SELECT 'ROLE_PROGRAMMER' 
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE nombre_rol = 'ROLE_PROGRAMMER'
);

-- Confirmar que el rol fue creado
SELECT * FROM roles WHERE nombre_rol = 'ROLE_PROGRAMMER';

-- ============================================
-- 2. CREAR USUARIO PROGRAMADOR
-- ============================================

-- Contraseña: "programador123"
-- Hash BCrypt: $2a$10$N9qo8uLOickgx2ZMRZoMyeXyQfQ.vu.xW5iyVMQQ5Xz.cPGVYgPLu

INSERT INTO employee (
    username,
    nombre,
    apellido,
    email,
    contrasenia,
    telefono,
    salario,
    enabled,
    created_at,
    updated_at
) VALUES (
    'programador',                                                -- Username para login
    'Programador',                                                -- Nombre
    'Sistema',                                                    -- Apellido
    'programador@aatechsolutions.com',                           -- Email único
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeXyQfQ.vu.xW5iyVMQQ5Xz.cPGVYgPLu', -- Contraseña: programador123
    '5551234567',                                                 -- Teléfono
    NULL,                                                         -- Sin salario (no es empleado del restaurante)
    TRUE,                                                         -- Habilitado
    NOW(),                                                        -- Fecha de creación
    NOW()                                                         -- Fecha de actualización
)
ON DUPLICATE KEY UPDATE username = username;  -- No hace nada si ya existe

-- Verificar que el usuario fue creado
SELECT id_empleado, username, nombre, apellido, email, enabled 
FROM employee 
WHERE username = 'programador';

-- ============================================
-- 3. ASIGNAR ROL PROGRAMMER AL USUARIO
-- ============================================

-- Asignar el rol PROGRAMMER al usuario programador
INSERT INTO employee_roles (id_empleado, id_rol)
SELECT e.id_empleado, r.id_rol
FROM employee e, roles r
WHERE e.username = 'programador'
  AND r.nombre_rol = 'ROLE_PROGRAMMER'
  AND NOT EXISTS (
      SELECT 1 FROM employee_roles er2
      WHERE er2.id_empleado = e.id_empleado
        AND er2.id_rol = r.id_rol
  );

-- Verificar que el rol fue asignado correctamente
SELECT 
    e.id_empleado,
    e.username,
    e.nombre,
    e.apellido,
    e.email,
    r.nombre_rol
FROM employee e
INNER JOIN employee_roles er ON e.id_empleado = er.id_empleado
INNER JOIN roles r ON er.id_rol = r.id_rol
WHERE e.username = 'programador';

-- ============================================
-- RESUMEN FINAL
-- ============================================

-- Ver todos los roles en el sistema
SELECT id_rol, nombre_rol FROM roles ORDER BY id_rol;

-- Ver el usuario programador con su rol
SELECT 
    e.id_empleado AS 'ID',
    e.username AS 'Usuario',
    e.nombre AS 'Nombre',
    e.apellido AS 'Apellido',
    e.email AS 'Email',
    GROUP_CONCAT(r.nombre_rol SEPARATOR ', ') AS 'Roles',
    e.enabled AS 'Activo'
FROM employee e
LEFT JOIN employee_roles er ON e.id_empleado = er.id_empleado
LEFT JOIN roles r ON er.id_rol = r.id_rol
WHERE e.username = 'programador'
GROUP BY e.id_empleado;

-- ============================================
-- CREDENCIALES DE ACCESO
-- ============================================
-- Usuario: programador
-- Contraseña: programador123
-- Rol: ROLE_PROGRAMMER
-- Acceso: http://localhost:8080/login
-- Dashboard: http://localhost:8080/programmer/dashboard
-- ============================================

-- NOTAS IMPORTANTES:
-- 1. La contraseña "programador123" ya está hasheada con BCrypt
-- 2. Puedes cambiar la contraseña generando un nuevo hash en:
--    https://bcrypt-generator.com/
-- 3. El usuario programador puede acceder a cualquier instalación
--    del sistema donde se ejecute este script
-- 4. Para cambiar el email, actualiza la línea del INSERT
-- 5. El rol PROGRAMMER tiene permisos especiales definidos en SecurityConfig
