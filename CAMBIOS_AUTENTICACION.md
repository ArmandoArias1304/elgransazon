# Cambios Realizados - Sistema de Autenticación

## Resumen de Modificaciones

Se ha actualizado el sistema de autenticación para separar el **username** (nombre de usuario para login) del **nombre** (nombre real del empleado), y se agregó el campo **email** como campo único requerido.

## Cambios en el Código

### 1. Entidad Employee (`Employee.java`)

**Campos Nuevos Agregados:**
```java
@Column(name = "username", nullable = false, unique = true, length = 50)
private String username;

@Column(name = "email", nullable = false, unique = true, length = 100)
private String email;
```

**Antes:**
- Los empleados iniciaban sesión con su `nombre` (nombre real)
- No había campo de email

**Ahora:**
- Los empleados inician sesión con `username` (nombre de usuario único)
- El `nombre` sigue existiendo pero es solo el nombre real
- Nuevo campo `email` único y obligatorio

### 2. EmployeeRepository (`EmployeeRepository.java`)

**Métodos Actualizados:**

**Antes:**
```java
Optional<Employee> findByNombre(String nombre);
boolean existsByNombre(String nombre);
```

**Ahora:**
```java
Optional<Employee> findByUsername(String username);
Optional<Employee> findByEmail(String email);
boolean existsByUsername(String username);
boolean existsByEmail(String email);
```

### 3. CustomUserDetailsService (`CustomUserDetailsService.java`)

**Cambios en Autenticación:**

**Antes:**
```java
Employee employee = employeeRepository.findByNombre(username)...
UserDetails userDetails = User.builder()
    .username(employee.getNombre())...
```

**Ahora:**
```java
Employee employee = employeeRepository.findByUsername(username)...
UserDetails userDetails = User.builder()
    .username(employee.getUsername())...
```

### 4. EmployeeService (`EmployeeService.java`)

**Nuevos Métodos:**
- `findByUsername(String username)` - Buscar empleado por username
- `findByEmail(String email)` - Buscar empleado por email

**Validaciones Agregadas en `create()`:**
```java
if (employeeRepository.existsByUsername(employee.getUsername())) {
    throw new IllegalArgumentException("Username already exists");
}
if (employeeRepository.existsByEmail(employee.getEmail())) {
    throw new IllegalArgumentException("Email already exists");
}
```

**Validaciones Agregadas en `update()`:**
- Verifica que el nuevo username no esté en uso por otro empleado
- Verifica que el nuevo email no esté en uso por otro empleado

### 5. Templates HTML

**Vistas Actualizadas con Logout Funcional:**

Todas las vistas en `templates/admin/`, `templates/waiter/` y `templates/chef/` ahora tienen formularios de logout funcionales:

```html
<form action="/logout" method="post" style="display: inline; width: 100%;">
  <button type="submit" class="btn-logout">
    <span class="material-symbols-outlined text-lg">logout</span>
    <span>Cerrar Sesión</span>
  </button>
</form>
```

**Archivos Modificados:**
- ✅ `templates/admin/dashboard.html` (ya tenía logout)
- ✅ `templates/admin/tables.html` (actualizado)
- ✅ `templates/admin/reservations.html` (actualizado)
- ✅ `templates/admin/inventory.html` (actualizado)
- ✅ `templates/waiter/dashboard.html` (ya tenía logout)
- ✅ `templates/chef/dashboard.html` (ya tenía logout)

## Cambios en la Base de Datos

### Nueva Estructura de la Tabla `employee`

```sql
CREATE TABLE employee (
    id_empleado BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,      -- NUEVO
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,        -- NUEVO
    contrasenia VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uk_employee_username (username),
    UNIQUE KEY uk_employee_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Datos de Prueba Actualizados

| ID | Username | Nombre | Apellido | Email | Rol |
|----|----------|--------|----------|-------|-----|
| 1 | admin_user | admin | Administrator | admin@restaurant.com | ROLE_ADMIN |
| 2 | juan_mesero | juan | Perez | juan.perez@restaurant.com | ROLE_WAITER |
| 3 | maria_mesera | maria | Garcia | maria.garcia@restaurant.com | ROLE_WAITER |
| 4 | carlos_chef | carlos | Rodriguez | carlos.rodriguez@restaurant.com | ROLE_CHEF |
| 5 | ana_chef | ana | Martinez | ana.martinez@restaurant.com | ROLE_CHEF |

**Contraseña para todos:** `password123`

## Scripts de Base de Datos

### 1. Script de Inicialización Completa

**Archivo:** `database/init_employee_with_roles.sql`

Usa este script para crear la base de datos desde cero con la nueva estructura.

```bash
mysql -u root -p < database/init_employee_with_roles.sql
```

### 2. Script de Migración

**Archivo:** `database/migrate_add_username_email.sql`

Usa este script si ya tienes una base de datos existente y quieres agregar los campos `username` y `email`.

```bash
mysql -u root -p < database/migrate_add_username_email.sql
```

**¿Qué hace el script de migración?**
1. Agrega columna `username` si no existe
2. Agrega columna `email` si no existe
3. Genera valores por defecto basados en el `nombre` actual
4. Agrega las restricciones UNIQUE y NOT NULL
5. Elimina la restricción antigua de `nombre` único

## Cómo Iniciar Sesión Ahora

### Antes (Incorrecto ahora):
- **Usuario:** `admin` (nombre real)
- **Contraseña:** `password123`

### Ahora (Correcto):
- **Usuario:** `admin_user` (username)
- **Contraseña:** `password123`

## Ejemplos de Uso

### Crear un Nuevo Empleado

```java
Employee newEmployee = Employee.builder()
    .username("pedro_mesero")           // NUEVO: username único
    .nombre("Pedro")                    // Nombre real
    .apellido("Sanchez")
    .email("pedro.sanchez@restaurant.com")  // NUEVO: email único
    .contrasenia("password123")         // Se encriptará automáticamente
    .enabled(true)
    .build();

employeeService.create(newEmployee);
```

### Buscar Empleado por Username

```java
Optional<Employee> employee = employeeService.findByUsername("admin_user");
```

### Buscar Empleado por Email

```java
Optional<Employee> employee = employeeService.findByEmail("admin@restaurant.com");
```

### Actualizar Empleado

```java
Employee employee = employeeService.findById(1L).orElseThrow();
employee.setEmail("newemail@restaurant.com");
employee.setUsername("new_username");
employeeService.update(1L, employee);
```

## Validaciones Implementadas

### En el Servicio (EmployeeService):

1. **Al Crear:**
   - ✅ Username debe ser único
   - ✅ Email debe ser único
   - ✅ Contraseña se encripta con BCrypt

2. **Al Actualizar:**
   - ✅ Si se cambia el username, verifica que no esté en uso
   - ✅ Si se cambia el email, verifica que no esté en uso
   - ✅ Solo actualiza contraseña si se proporciona una nueva

### En la Base de Datos:

1. ✅ `username` es NOT NULL y UNIQUE
2. ✅ `email` es NOT NULL y UNIQUE
3. ✅ Índices únicos: `uk_employee_username`, `uk_employee_email`

## Testing

### 1. Probar Login con Username

1. Ejecuta el script `init_employee_with_roles.sql`
2. Inicia la aplicación
3. Ve a `http://localhost:8080/login`
4. Ingresa:
   - **Usuario:** `admin_user`
   - **Contraseña:** `password123`
5. Deberías ser redirigido a `/admin/dashboard`

### 2. Probar Logout

1. Después de iniciar sesión, ve a cualquier dashboard
2. Haz clic en el botón "Cerrar Sesión" / "Logout"
3. Deberías ser redirigido a `/login?logout`
4. La sesión debe ser destruida

### 3. Probar Roles

**Admin:**
- Usuario: `admin_user`
- Redirect: `/admin/dashboard`

**Mesero:**
- Usuario: `juan_mesero` o `maria_mesera`
- Redirect: `/waiter/dashboard`

**Chef:**
- Usuario: `carlos_chef` o `ana_chef`
- Redirect: `/chef/dashboard`

## Troubleshooting

### Error: "Column 'username' cannot be null"

**Causa:** Intentaste crear un empleado sin username.

**Solución:** Siempre proporciona un username único:
```java
employee.setUsername("unique_username");
```

### Error: "Duplicate entry for key 'uk_employee_username'"

**Causa:** El username ya existe en la base de datos.

**Solución:** Usa un username diferente o busca el empleado existente.

### Error: "Employee not found with username: XXX"

**Causa:** El username no existe en la base de datos o estás usando el nombre real en vez del username.

**Solución:** 
- Verifica que el usuario exista en la BD
- Usa el `username`, no el `nombre`

### La base de datos tiene la estructura antigua

**Solución:** 
1. Ejecuta el script de migración: `database/migrate_add_username_email.sql`
2. O recrea la base de datos: `database/init_employee_with_roles.sql`

## Próximos Pasos Recomendados

1. **Validación de Email en Frontend:**
   - Agregar validación de formato de email en formularios HTML

2. **Recuperación de Contraseña:**
   - Implementar funcionalidad "Olvidé mi contraseña" usando email

3. **Perfil de Usuario:**
   - Crear vista para que empleados actualicen su email y contraseña

4. **Auditoría:**
   - Agregar campos `created_at` y `updated_at` a la tabla employee

5. **Testing Automatizado:**
   - Crear tests unitarios para EmployeeService
   - Crear tests de integración para autenticación

## Resumen de Archivos Modificados

### Código Java:
- ✅ `Employee.java` - Agregados campos username y email
- ✅ `EmployeeRepository.java` - Nuevos métodos de búsqueda
- ✅ `CustomUserDetailsService.java` - Usar username en vez de nombre
- ✅ `EmployeeService.java` - Validaciones y métodos nuevos

### Templates HTML:
- ✅ `admin/tables.html` - Logout funcional
- ✅ `admin/reservations.html` - Logout funcional
- ✅ `admin/inventory.html` - Logout funcional
- ✅ `waiter/dashboard.html` - Ya tenía logout
- ✅ `chef/dashboard.html` - Ya tenía logout

### Base de Datos:
- ✅ `init_employee_with_roles.sql` - Script actualizado
- ✅ `migrate_add_username_email.sql` - Nuevo script de migración

### Documentación:
- ✅ `CAMBIOS_AUTENTICACION.md` - Este archivo

## Conclusión

El sistema ahora tiene una separación clara entre:
- **Username**: Credencial de login (único, corto)
- **Nombre**: Nombre real del empleado (puede repetirse)
- **Email**: Comunicación y posible recuperación de cuenta (único)

Todos los dashboards ahora tienen funcionalidad de logout completa y funcional.

---

**Fecha de Actualización:** 2024
**Versión:** 1.1.0
