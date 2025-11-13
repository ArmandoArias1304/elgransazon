# 🎭 Sistema de Roles - Restaurant POS

## ✅ Actualización Completada

Se ha implementado un sistema completo de roles con las siguientes características:

### 🎯 Roles Implementados

1. **ROLE_ADMIN** (Administrador)
   - Gestión completa del sistema
   - Dashboard: `/admin/dashboard`
   - Color: Morado (Purple gradient)

2. **ROLE_WAITER** (Mesero)
   - Gestión de órdenes y mesas
   - Dashboard: `/waiter/dashboard`
   - Color: Rosa (Pink gradient)

3. **ROLE_CHEF** (Cocinero)
   - Gestión de cocina y preparación
   - Dashboard: `/chef/dashboard`
   - Color: Naranja-Amarillo (Orange-Yellow gradient)

## 🗄️ Estructura de Base de Datos

### Tablas Creadas

#### 1. `employee` (Empleados)
```sql
id_empleado (PK)
nombre (username único)
apellido
contrasenia (BCrypt)
enabled (activo/inactivo)
```

#### 2. `roles` (Roles)
```sql
id_rol (PK)
nombre_rol (único: ROLE_ADMIN, ROLE_WAITER, ROLE_CHEF)
```

#### 3. `employee_roles` (Relación Many-to-Many)
```sql
id_empleado (FK → employee)
id_rol (FK → roles)
PRIMARY KEY (id_empleado, id_rol)
```

### Diagrama de Relación

```
┌─────────────┐         ┌──────────────────┐         ┌─────────────┐
│  employee   │         │  employee_roles  │         │    roles    │
├─────────────┤         ├──────────────────┤         ├─────────────┤
│ id_empleado │────────>│  id_empleado (FK)│         │   id_rol    │
│ nombre      │         │  id_rol (FK)     │<────────│ nombre_rol  │
│ apellido    │         └──────────────────┘         └─────────────┘
│ contrasenia │
│ enabled     │
└─────────────┘
```

## 👤 Usuarios de Prueba

| Usuario | Contraseña | Rol | Dashboard |
|---------|------------|-----|-----------|
| admin | password123 | Administrador | `/admin/dashboard` |
| juan | password123 | Mesero | `/waiter/dashboard` |
| maria | password123 | Mesero | `/waiter/dashboard` |
| carlos | password123 | Cocinero | `/chef/dashboard` |
| ana | password123 | Cocinero | `/chef/dashboard` |

## 🔐 Configuración de Seguridad

### Protección de Rutas por Rol

```java
/admin/**   → Solo ROLE_ADMIN
/waiter/**  → Solo ROLE_WAITER
/chef/**    → Solo ROLE_CHEF
```

### Redirección Automática

Después del login exitoso, cada usuario es redirigido automáticamente a su dashboard correspondiente según su rol principal:

1. Si tiene `ROLE_ADMIN` → `/admin/dashboard`
2. Si tiene `ROLE_CHEF` → `/chef/dashboard`
3. Si tiene `ROLE_WAITER` → `/waiter/dashboard`
4. Si no tiene rol específico → `/home`

## 📁 Archivos Creados/Actualizados

### Entidades
- ✅ `Role.java` - Nueva entidad de roles
- ✅ `Employee.java` - Actualizada con relación Many-to-Many

### Repositorios
- ✅ `RoleRepository.java` - Repositorio para roles

### Servicios
- ✅ `CustomUserDetailsService.java` - Actualizado para cargar roles desde BD

### Configuración
- ✅ `CustomAuthenticationSuccessHandler.java` - Redirección basada en roles
- ✅ `SecurityConfig.java` - Protección de rutas por rol

### Controladores
- ✅ `AdminController.java` - Controlador para administradores
- ✅ `WaiterController.java` - Controlador para meseros
- ✅ `ChefController.java` - Controlador para cocineros

### Vistas (Thymeleaf)
- ✅ `templates/admin/dashboard.html` - Dashboard de administrador
- ✅ `templates/waiter/dashboard.html` - Dashboard de mesero
- ✅ `templates/chef/dashboard.html` - Dashboard de cocinero

### Base de Datos
- ✅ `database/init_employee_with_roles.sql` - Script SQL completo con roles

## 🚀 Instrucciones de Instalación

### 1. Actualizar Base de Datos

Ejecuta el nuevo script SQL:

```bash
mysql -u root -p < database/init_employee_with_roles.sql
```

O desde MySQL Workbench/phpMyAdmin ejecuta:

```sql
SOURCE database/init_employee_with_roles.sql;
```

### 2. Verificar Tablas Creadas

```sql
USE bd_restaurant;
SHOW TABLES;
-- Deberías ver: employee, roles, employee_roles

SELECT * FROM roles;
SELECT * FROM employee;
SELECT * FROM employee_roles;
```

### 3. Compilar el Proyecto

```powershell
.\mvnw.cmd clean install
```

### 4. Ejecutar la Aplicación

```powershell
.\mvnw.cmd spring-boot:run
```

## 🧪 Pruebas de Funcionalidad

### Prueba 1: Login como Admin
1. Ir a `http://localhost:8080/login`
2. Usuario: `admin` / Contraseña: `password123`
3. ✅ Debe redirigir a `/admin/dashboard`
4. ✅ Debe mostrar panel de administrador con color morado

### Prueba 2: Login como Mesero
1. Cerrar sesión
2. Usuario: `juan` / Contraseña: `password123`
3. ✅ Debe redirigir a `/waiter/dashboard`
4. ✅ Debe mostrar panel de mesero con color rosa

### Prueba 3: Login como Cocinero
1. Cerrar sesión
2. Usuario: `carlos` / Contraseña: `password123`
3. ✅ Debe redirigir a `/chef/dashboard`
4. ✅ Debe mostrar panel de cocinero con color naranja-amarillo

### Prueba 4: Control de Acceso
1. Loguearse como mesero (`juan`)
2. Intentar acceder a `http://localhost:8080/admin/dashboard`
3. ✅ Debe mostrar "403 Forbidden" (acceso denegado)

## 🎨 Características de las Vistas

### Dashboard de Administrador
- 👥 Gestión de empleados
- 📊 Reportes de ventas
- 📦 Control de inventario
- ⚙️ Configuración del sistema
- 🏪 Gestión de mesas
- 📱 Órdenes en vivo

### Dashboard de Mesero
- ➕ Nueva orden
- 🪑 Mis mesas
- 📋 Órdenes activas
- 📖 Menú
- 💳 Procesar pago
- 📊 Mis ventas

### Dashboard de Cocinero
- 🔔 Órdenes pendientes
- 🍳 En progreso
- ✅ Listas para servir
- 📦 Inventario de cocina
- 📖 Recetas
- 📊 Estadísticas de cocina

## 🔧 Consultas SQL Útiles

### Ver todos los empleados con sus roles
```sql
SELECT 
    e.nombre,
    e.apellido,
    GROUP_CONCAT(r.nombre_rol) AS roles
FROM employee e
LEFT JOIN employee_roles er ON e.id_empleado = er.id_empleado
LEFT JOIN roles r ON er.id_rol = r.id_rol
GROUP BY e.id_empleado;
```

### Agregar un nuevo empleado con rol
```sql
-- 1. Crear empleado
INSERT INTO employee (nombre, apellido, contrasenia, enabled)
VALUES ('nuevo', 'Empleado', '$2a$10$...hash_bcrypt...', TRUE);

-- 2. Asignar rol
INSERT INTO employee_roles (id_empleado, id_rol)
SELECT e.id_empleado, r.id_rol
FROM employee e, roles r
WHERE e.nombre = 'nuevo' AND r.nombre_rol = 'ROLE_WAITER';
```

### Cambiar rol de un empleado
```sql
-- Eliminar roles actuales
DELETE FROM employee_roles WHERE id_empleado = 1;

-- Asignar nuevo rol
INSERT INTO employee_roles (id_empleado, id_rol)
VALUES (1, (SELECT id_rol FROM roles WHERE nombre_rol = 'ROLE_ADMIN'));
```

## 🛠️ Uso Programático de Roles

### Verificar rol en código Java
```java
// En un controlador
@GetMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public String manageUsers() {
    return "admin/users";
}

// En una vista Thymeleaf
<div sec:authorize="hasRole('ADMIN')">
    Solo visible para administradores
</div>
```

### Obtener rol del usuario actual
```java
// En Employee entity
public String getPrimaryRole() {
    return roles.stream()
        .findFirst()
        .map(Role::getNombreRol)
        .orElse("ROLE_EMPLOYEE");
}

// Verificar si tiene un rol específico
public boolean hasRole(String roleName) {
    return roles.stream()
        .anyMatch(role -> role.getNombreRol().equals(roleName));
}
```

## 📈 Próximos Pasos Recomendados

1. **Agregar más roles específicos**
   - ROLE_CASHIER (Cajero)
   - ROLE_MANAGER (Gerente)
   - ROLE_SUPERVISOR (Supervisor)

2. **Implementar permisos granulares**
   - Tabla de permisos
   - Asignación de permisos a roles
   - Control de acceso a nivel de funcionalidad

3. **Mejorar gestión de empleados**
   - CRUD completo de empleados (crear, editar, eliminar)
   - Asignación dinámica de roles desde interfaz
   - Historial de cambios de roles

4. **Auditoría**
   - Registrar logins por rol
   - Tracking de acciones por usuario
   - Reportes de actividad por rol

## 🐛 Troubleshooting

### Error: "No role found for user"
**Solución**: Verificar que el empleado tenga al menos un rol asignado:
```sql
SELECT * FROM employee_roles WHERE id_empleado = ?;
```

### Error: "403 Forbidden" al acceder a dashboard
**Solución**: Verificar que el usuario tenga el rol correcto:
```sql
SELECT r.nombre_rol 
FROM employee e
JOIN employee_roles er ON e.id_empleado = er.id_empleado
JOIN roles r ON er.id_rol = r.id_rol
WHERE e.nombre = 'usuario';
```

### Usuario redirigido a /home en vez de su dashboard
**Solución**: Verificar que los roles en BD empiecen con "ROLE_":
```sql
UPDATE roles SET nombre_rol = CONCAT('ROLE_', nombre_rol) 
WHERE nombre_rol NOT LIKE 'ROLE_%';
```

## ✅ Checklist de Verificación

- [ ] Tabla `roles` creada con 3 roles
- [ ] Tabla `employee_roles` creada con relaciones
- [ ] Empleados tienen roles asignados
- [ ] Login como admin redirige a `/admin/dashboard`
- [ ] Login como waiter redirige a `/waiter/dashboard`
- [ ] Login como chef redirige a `/chef/dashboard`
- [ ] Control de acceso funciona (403 en rutas no autorizadas)
- [ ] Cada dashboard muestra información del rol correcto

---

**¡Sistema de roles completamente funcional!** 🎉
