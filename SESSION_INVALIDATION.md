# Session Invalidation Implementation

## Overview
Este documento explica la implementación de invalidación automática de sesiones cuando se modifican credenciales críticas de empleados.

## Problema Original
Cuando un administrador cambiaba el **username**, **password** o **deshabilitaba** a un empleado que tenía una sesión activa:
- ❌ El usuario seguía logueado con las credenciales antiguas
- ❌ El username/password antiguo ya no funcionaba para futuros logins
- ❌ El usuario podía seguir usando el sistema hasta que cerrara sesión o expirara la sesión
- ❌ Aparecía mensaje confuso: "This session has been expired (possibly due to...)"
- ❌ Riesgo de seguridad: credenciales obsoletas pero sesión válida

## Solución Implementada

### 1. SessionRegistry en SecurityConfig
**Archivo:** `SecurityConfig.java`

```java
@Bean
public SessionRegistry sessionRegistry() {
    return new SessionRegistryImpl();
}
```

- **SessionRegistry**: Rastrea todas las sesiones activas en la aplicación
- **SessionRegistryImpl**: Implementación por defecto de Spring Security
- Se configura en `sessionManagement` para que Spring lo use automáticamente

**Configuración de Session Management:**
```java
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
    .maximumSessions(1)
    .maxSessionsPreventsLogin(false)
    .expiredUrl("/login")  // ← CLAVE: Redirige a login sin mostrar mensaje de error
    .sessionRegistry(sessionRegistry())
)
```

**¿Por qué `expiredUrl("/login")`?**
- Sin esta configuración: Spring muestra mensaje "This session has been expired (possibly due to...)"
- Con esta configuración: Redirige directamente a `/login` sin mensaje
- Mejora la experiencia del usuario

### 2. Método de Invalidación en EmployeeService
**Archivo:** `EmployeeService.java`

```java
private void invalidateUserSessions(String username) {
    log.debug("Invalidating sessions for user: {}", username);
    
    try {
        List<Object> principals = sessionRegistry.getAllPrincipals();
        
        for (Object principal : principals) {
            if (principal instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) principal;
                
                if (userDetails.getUsername().equals(username)) {
                    List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
                    
                    for (SessionInformation session : sessions) {
                        session.expireNow();
                        log.info("Expired session {} for user {}", session.getSessionId(), username);
                    }
                }
            }
        }
    } catch (Exception e) {
        log.error("Error invalidating sessions for user {}: {}", username, e.getMessage(), e);
    }
}
```

**Funcionamiento:**
1. Obtiene todos los principals (usuarios autenticados) del registry
2. Busca el principal que coincide con el username
3. Obtiene todas las sesiones activas de ese usuario
4. Llama a `session.expireNow()` para invalidar cada sesión
5. Maneja excepciones sin interrumpir la operación principal

### 3. Casos de Uso - Cuándo se Invalidan Sesiones

#### a) Cambio de Username
**Método:** `update(Long id, Employee employeeDetails, String updatedBy)`

```java
// Track if username is changing
boolean usernameChanged = !employee.getUsername().equals(employeeDetails.getUsername());
String oldUsername = employee.getUsername();

// ... actualizaciones ...

// Invalidate sessions if username was changed
if (usernameChanged) {
    invalidateUserSessions(oldUsername);
    log.info("Invalidated sessions for user {} due to username change", oldUsername);
}
```

**Flujo:**
1. Admin cambia el username de `juan.perez` a `jperez`
2. Se guarda el cambio en la BD
3. Se invalidan todas las sesiones del username antiguo (`juan.perez`)
4. Usuario es deslogueado automáticamente
5. Debe iniciar sesión con el nuevo username (`jperez`)

#### b) Cambio de Password
**Método:** `update(Long id, Employee employeeDetails, String updatedBy)`

```java
// Track if password is being changed
boolean passwordChanged = false;

// Only update password if it's provided and different
if (employeeDetails.getContrasenia() != null && 
    !employeeDetails.getContrasenia().isEmpty() &&
    !employee.getContrasenia().equals(employeeDetails.getContrasenia())) {
    String encodedPassword = passwordEncoder.encode(employeeDetails.getContrasenia());
    employee.setContrasenia(encodedPassword);
    passwordChanged = true;
}

// ... save ...

// Invalidate sessions if password was changed
if (passwordChanged) {
    invalidateUserSessions(updatedEmployee.getUsername());
    log.info("Invalidated sessions for user {} due to password change", updatedEmployee.getUsername());
}
```

**También en:** `changePassword(Long id, String newPassword)`

**Flujo:**
1. Admin cambia el password de un empleado
2. Password se encripta y guarda
3. Se invalidan todas las sesiones activas de ese usuario
4. Usuario es deslogueado automáticamente
5. Debe iniciar sesión con el nuevo password

#### c) Deshabilitación de Usuario
**Método:** `setEnabled(Long id, boolean enabled, String updatedBy)`

```java
employee.setEnabled(enabled);
employee.setUpdatedBy(updatedBy);
employeeRepository.save(employee);

log.info("Employee enabled status updated: {}", id);

// Invalidate sessions if employee was disabled
if (!enabled) {
    invalidateUserSessions(employee.getUsername());
    log.info("Invalidated sessions for user {} due to account being disabled", employee.getUsername());
}
```

**Flujo:**
1. Admin deshabilita un empleado (enabled = false)
2. Se guarda el cambio en la BD
3. Se invalidan todas las sesiones activas de ese usuario
4. Usuario es deslogueado automáticamente
5. No puede volver a iniciar sesión (cuenta deshabilitada)

**Nota:** Si el empleado es **habilitado** (enabled = true), NO se invalidan sesiones.

## Comportamiento del Usuario

### Escenario 1: Cambio de Username
1. **Usuario logueado como:** `juan.perez`
2. **Admin cambia username a:** `jperez`
3. **Usuario ve:** Página de login (sesión expirada)
4. **Usuario debe:** Iniciar sesión con `jperez`

### Escenario 2: Cambio de Password
1. **Usuario logueado** y trabajando
2. **Admin cambia su password**
3. **Usuario ve:** Página de login (sesión expirada)
4. **Usuario debe:** Iniciar sesión con el nuevo password

### Escenario 3: Usuario Deshabilitado
1. **Usuario logueado** y trabajando
2. **Admin deshabilita la cuenta** (toggle OFF)
3. **EmployeeService** llama a `invalidateUserSessions(username)`
4. **SessionRegistry** expira todas las sesiones de ese usuario
5. **Usuario intenta navegar o refrescar**
6. **Usuario ve:** Página de login (sin mensaje de error gracias a `expiredUrl`)
7. **Usuario intenta login:** Error "Usuario deshabilitado"

**Nota:** La sesión se invalida inmediatamente cuando el admin deshabilita la cuenta. En la siguiente petición HTTP del usuario, será redirigido al login.

## Ventajas de Seguridad

✅ **Inmediatez**: Los cambios de credenciales se aplican inmediatamente  
✅ **Seguridad**: No hay ventana de vulnerabilidad con credenciales obsoletas  
✅ **Auditoría**: Logs detallados de cada invalidación de sesión  
✅ **Transparencia**: El usuario sabe que debe re-autenticarse  
✅ **Prevención**: Evita acceso no autorizado con credenciales viejas  
✅ **Sin mensajes confusos**: `expiredUrl("/login")` redirige directamente sin mostrar "session expired"  
✅ **Simplicidad**: Usa funcionalidad nativa de Spring Security, sin filtros adicionales  

## Logs Generados

### Cambio de Username
```
INFO  - Employee updated successfully: 5
INFO  - Invalidated sessions for user juan.perez due to username change
INFO  - Expired session ABC123XYZ for user juan.perez
```

### Cambio de Password
```
INFO  - Password changed successfully for employee: 5
INFO  - Invalidated sessions for user jperez due to password change
INFO  - Expired session DEF456UVW for user jperez
```

### Usuario Deshabilitado
```
INFO  - Employee enabled status updated: 5
INFO  - Invalidated sessions for user jperez due to account being disabled
INFO  - Expired session GHI789RST for user jperez
```

## Testing Manual

### Test 1: Cambio de Username
1. Abrir navegador A: Login como `empleado1`
2. Abrir navegador B: Login como `admin`
3. En navegador B: Cambiar username de `empleado1` a `empleado1_nuevo`
4. En navegador A: Intentar navegar o refrescar
5. **Resultado esperado:** Redirige a login, debe usar `empleado1_nuevo`

### Test 2: Cambio de Password
1. Abrir navegador A: Login como `empleado2`
2. Abrir navegador B: Login como `admin`
3. En navegador B: Cambiar password de `empleado2`
4. En navegador A: Intentar navegar o refrescar
5. **Resultado esperado:** Redirige a login, debe usar nuevo password

### Test 3: Deshabilitar Usuario
1. Abrir navegador A: Login como `empleado3`
2. Abrir navegador B: Login como `admin`
3. En navegador B: Toggle enabled OFF para `empleado3`
4. En navegador A: Refrescar página o intentar navegar
5. **Resultado esperado:** 
   - Redirige a login (sin mensaje "This session has been expired")
   - Login falla (usuario deshabilitado)
   - Experiencia limpia sin mensajes confusos

## Consideraciones Técnicas

- **maximumSessions(1)**: Solo permite 1 sesión simultánea por usuario
- **maxSessionsPreventsLogin(false)**: Nueva sesión desloguea la sesión anterior
- **expiredUrl("/login")**: ⭐ **CLAVE** - Redirige a login sin mensaje cuando la sesión expira
- **SessionRegistry**: Mantiene mapa en memoria de principals → sesiones activas
- **Thread-safe**: SessionRegistryImpl es thread-safe
- **Manejo de errores**: Excepciones en `invalidateUserSessions()` no interrumpen la operación principal
- **Sin filtros adicionales**: Usa funcionalidad nativa de Spring Security
- **Performance**: No hay overhead adicional, solo las llamadas a `session.expireNow()` cuando es necesario

## Próximos Pasos (Opcional)

- [ ] Añadir notificación al usuario cuando su sesión es invalidada
- [ ] Dashboard admin para ver sesiones activas de todos los usuarios
- [ ] Invalidación manual desde el admin panel
- [ ] Historial de invalidaciones en tabla de auditoría
- [ ] Websockets para notificar cambios en tiempo real

---

**Fecha de implementación:** {{ fecha actual }}  
**Autor:** {{ tu nombre }}  
**Versión:** 1.0
