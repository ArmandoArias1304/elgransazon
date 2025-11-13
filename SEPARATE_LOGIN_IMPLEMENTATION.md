# Sistema de Login Separado: Empleados vs Clientes

## Resumen de Implementación

Se ha implementado un sistema de autenticación separado para empleados y clientes, con interfaces de login distintas y flujos de autenticación específicos.

## Estructura de Login

### 🔐 Login de Empleados
- **URL**: `/login`
- **Template**: `auth/login.html`
- **Campo de usuario**: Username (nombre de usuario único)
- **Roles**: ADMIN, MANAGER, WAITER, CHEF, CASHIER, DELIVERY
- **Redirección**: Según rol (dashboard específico)

### 👤 Login de Clientes
- **URL**: `/client/login`
- **Template**: `auth/loginClient.html`
- **Campo de usuario**: Email (correo electrónico)
- **Rol**: CLIENT
- **Redirección**: `/client/menu`

## Archivos Modificados

### 1. SecurityConfig.java
```java
// Rutas públicas actualizadas
.requestMatchers("/", "/home", "/login", "/client/login", "/client/register", ...)
    .permitAll()

// Rutas protegidas para clientes
.requestMatchers("/client/**").hasRole("CLIENT")
```

### 2. ClientAuthController.java
**Nuevo método añadido:**
```java
@GetMapping("/login")
public String showLoginForm(...) {
    // Muestra formulario de login para clientes
    // Incluye configuración del sistema para branding
    return "auth/loginClient";
}
```

### 3. ClientController.java
**Error corregido:**
- Se eliminó `@RequiredArgsConstructor`
- Se creó constructor manual con `@Qualifier("customerOrderService")`
- Esto asegura que se inyecte correctamente el servicio específico para clientes

### 4. Templates Actualizados

#### `auth/login.html` (Empleados)
- ✅ Añadido link a login de clientes
- ✅ Sección "¿Eres cliente?" con enlace a `/client/login`
- ✅ Campo: "Usuario" (username)

#### `auth/loginClient.html` (Clientes)
- ✅ Añadido link a login de empleados
- ✅ Sección "¿Eres empleado?" con enlace a `/login`
- ✅ Campo: "Correo Electrónico" (email)
- ✅ Link a registro: "¿No tienes cuenta? Regístrate"
- ✅ Mensajes de error y éxito específicos

#### `auth/registerClient.html`
- ✅ Link a login de clientes: "¿Ya tienes cuenta? Inicia sesión aquí"
- ✅ Link a login de empleados: "¿Eres empleado? Acceso para empleados"
- ✅ Mensaje de éxito añadido
- ✅ Redirección correcta a `/client/login` después del registro

## Flujos de Autenticación

### 📋 Flujo de Login de Empleados

1. Usuario accede a `/login`
2. Ingresa **username** y contraseña
3. Spring Security valida con `CustomUserDetailsService`:
   - Busca en tabla `employees` por username
   - Verifica contraseña BCrypt
   - Asigna roles del empleado (ADMIN, WAITER, etc.)
4. `CustomAuthenticationSuccessHandler` redirige según rol:
   - ADMIN/MANAGER → `/admin/dashboard`
   - WAITER → `/waiter/dashboard`
   - CHEF → `/chef/dashboard`
   - CASHIER → `/cashier/dashboard`
   - DELIVERY → `/delivery/dashboard`

### 📋 Flujo de Login de Clientes

1. Cliente accede a `/client/login`
2. Ingresa **email** y contraseña
3. Spring Security valida con `CustomUserDetailsService`:
   - No encuentra en `employees` (porque usó email, no username)
   - Busca en tabla `customers` por email
   - Verifica contraseña BCrypt
   - Asigna rol `ROLE_CLIENT` automáticamente
4. `CustomAuthenticationSuccessHandler` redirige a `/client/menu`

### 📋 Flujo de Registro de Clientes

1. Cliente accede a `/client/register`
2. Completa formulario con:
   - Nombre completo
   - Email (será su username para login)
   - Teléfono
   - Dirección (opcional)
   - Contraseña (mínimo 6 caracteres)
3. `ClientAuthController` valida:
   - Email no duplicado
   - Campos requeridos
   - Encripta contraseña con BCrypt
4. Guarda en tabla `customers`
5. Redirección a `/client/login` con mensaje de éxito

## Características de Seguridad

### CustomUserDetailsService
```java
@Override
public UserDetails loadUserByUsername(String username) {
    // 1. Primero busca Employee por username
    Employee employee = employeeRepository.findByUsername(username);
    if (employee != null) {
        return buildEmployeeUserDetails(employee);
    }
    
    // 2. Si no es empleado, busca Customer por email
    Customer customer = customerRepository.findByEmailIgnoreCase(username);
    if (customer != null) {
        return buildCustomerUserDetails(customer); // Asigna ROLE_CLIENT
    }
    
    throw new UsernameNotFoundException("Usuario o cliente no encontrado");
}
```

### Diferencias Clave

| Aspecto | Empleados | Clientes |
|---------|-----------|----------|
| **Username** | Campo `username` único | Campo `email` |
| **Login URL** | `/login` | `/client/login` |
| **Formulario** | "Usuario" | "Correo Electrónico" |
| **Roles** | Múltiples (ADMIN, WAITER, etc.) | Solo ROLE_CLIENT |
| **Registro** | Manual por admin | Auto-registro en `/client/register` |
| **Permisos** | CRUD completo de pedidos | Solo crear/ver propios pedidos |

## Navegación Entre Logins

### Desde Login de Empleados → Login de Clientes
```html
<a th:href="@{/client/login}">
    <span class="material-symbols-outlined">person</span>
    <span>Acceso para clientes</span>
</a>
```

### Desde Login de Clientes → Login de Empleados
```html
<a th:href="@{/login}">
    <span class="material-symbols-outlined">badge</span>
    <span>Acceso para empleados</span>
</a>
```

### Desde Registro de Clientes
- **A Login de Clientes**: "¿Ya tienes cuenta? Inicia sesión aquí" → `/client/login`
- **A Login de Empleados**: "¿Eres empleado? Acceso para empleados" → `/login`

## Mensajes de Error

### Login de Empleados
```
URL: /login?error=true
Mensaje: "Usuario o contraseña incorrectos"
```

### Login de Clientes
```
URL: /client/login?error=true
Mensaje: "Usuario o contraseña incorrectos"
```

### Registro de Clientes
```
Email duplicado: "El correo electrónico ya está registrado"
Error general: "Error al registrar el cliente: [mensaje]"
```

## Mensajes de Éxito

### Registro Exitoso
```
Mensaje: "Registro exitoso. Por favor inicia sesión con tu correo electrónico."
Redirección: /client/login
```

### Logout
```
Mensaje: "Has cerrado sesión exitosamente"
Visible en: Ambos formularios de login
```

## Testing Recomendado

### 1. Empleados
```
1. Login con username correcto → Redirige a dashboard según rol
2. Login con username incorrecto → Muestra error
3. Click en "Acceso para clientes" → Va a /client/login
```

### 2. Clientes
```
1. Registro con datos válidos → Crea cuenta y redirige a login
2. Registro con email duplicado → Muestra error
3. Login con email correcto → Redirige a /client/menu
4. Login con email incorrecto → Muestra error
5. Click en "Acceso para empleados" → Va a /login
```

### 3. Seguridad
```
1. Empleado NO puede acceder a /client/menu (sin ROLE_CLIENT)
2. Cliente NO puede acceder a /admin/dashboard (sin ROLE_ADMIN)
3. Usuario no autenticado es redirigido a /login
```

## URLs Importantes

| Descripción | URL | Público |
|-------------|-----|---------|
| Login Empleados | `/login` | ✅ |
| Login Clientes | `/client/login` | ✅ |
| Registro Clientes | `/client/register` | ✅ |
| Logout | `/logout` | ❌ (requiere sesión) |
| Menú Clientes | `/client/menu` | ❌ (requiere ROLE_CLIENT) |
| Dashboard Admin | `/admin/dashboard` | ❌ (requiere ROLE_ADMIN/MANAGER) |

## Próximos Pasos

1. ✅ **Ejecutar script SQL**: `ADD_CUSTOMER_SUPPORT.sql`
2. ✅ **Probar registro de cliente**
3. ✅ **Probar login de cliente con email**
4. ✅ **Probar login de empleado con username**
5. ⏳ **Crear vistas de cliente** (menu, orders, profile)
6. ⏳ **Implementar creación de pedidos desde cliente**
7. ⏳ **Añadir validaciones adicionales**

## Notas Técnicas

- Ambos formularios usan el mismo endpoint `/perform_login`
- `CustomUserDetailsService` determina automáticamente si es empleado o cliente
- El mismo `SecurityFilterChain` maneja ambos tipos de autenticación
- Contraseñas siempre se almacenan con BCrypt (mismo encoder para ambos)
- La redirección post-login se maneja en `CustomAuthenticationSuccessHandler`
