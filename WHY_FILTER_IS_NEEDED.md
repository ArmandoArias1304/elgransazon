# Por Qué el UserValidationFilter es Necesario

## Fecha: 16 de Octubre de 2025

## El Problema Original

### ❌ Comportamiento Observado
Cuando el admin deshabilitaba un usuario (enabled = false):
- ✅ El método `setEnabled()` **SÍ llamaba a `invalidateUserSessions()`**
- ✅ La sesión **SÍ se marcaba como expirada** con `session.expireNow()`
- ❌ **PERO** al recargar la página, el usuario **NO era redirigido al login**
- ✅ En cambio, cuando se cambiaba username o password, **SÍ redirigía al login**

### ❓ La Pregunta Clave
**"¿Por qué cuando cambio el username o contraseña sí me lleva al login, pero cuando desactivo el usuario no?"**

---

## La Causa Raíz

### Cómo Funciona Spring Security con Sesiones

1. **Al hacer login:**
   - `CustomUserDetailsService.loadUserByUsername()` consulta la BD
   - Crea un objeto `UserDetails` con el estado actual:
     ```java
     UserDetails userDetails = User.builder()
         .username(employee.getUsername())
         .password(employee.getContrasenia())
         .disabled(!employee.getEnabled())  // ← Se captura EN EL LOGIN
         .authorities(getAuthorities(employee))
         .build();
     ```
   - Este `UserDetails` se guarda en la **sesión HTTP**
   - Spring Security **NO vuelve a consultar la BD** en cada request

2. **En cada request posterior:**
   - Spring Security **lee el `UserDetails` de la sesión**
   - **NO consulta la base de datos nuevamente**
   - Usa el valor de `disabled` que se guardó al hacer login

### Por Qué Username/Password SÍ Funcionan

Cuando cambias **username** o **password**:

```java
// En EmployeeService.update()
if (usernameChanged) {
    invalidateUserSessions(oldUsername);  // ← Expira la sesión
}
```

**Flujo:**
1. Se llama a `session.expireNow()`
2. La sesión se marca como expirada en `SessionRegistry`
3. Usuario recarga la página
4. Spring Security detecta: **"Esta sesión está expirada en el registry"**
5. Redirige a login (gracias a `expiredUrl("/login")`)

### Por Qué Enabled NO Funcionaba

Cuando cambias **enabled = false**:

```java
// En EmployeeService.setEnabled()
if (!enabled) {
    invalidateUserSessions(employee.getUsername());  // ← Expira la sesión
}
```

**Flujo:**
1. Se llama a `session.expireNow()`
2. La sesión se marca como expirada en `SessionRegistry`
3. Usuario recarga la página
4. Spring Security detecta: **"Esta sesión está expirada"**
5. **PERO** el `UserDetails` en la sesión todavía tiene `disabled=false` (valor del login)
6. Spring Security **NO re-valida contra la BD**
7. El usuario permanece logueado ❌

**El problema:** El valor de `disabled` en `UserDetails` **no se actualiza automáticamente**. Es una instantánea tomada al hacer login.

---

## La Solución: UserValidationFilter

### ¿Qué Hace el Filtro?

```java
@Component
public class UserValidationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            
            // ← CONSULTA LA BD EN CADA REQUEST
            Optional<Employee> employeeOpt = employeeRepository.findByUsername(username);
            
            // Si el usuario está deshabilitado o no existe
            if (employeeOpt.isEmpty() || !employeeOpt.get().getEnabled()) {
                // Desloguear inmediatamente
                new SecurityContextLogoutHandler().logout(request, response, authentication);
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

### Flujo Completo con el Filtro

```
1. Admin deshabilita usuario (enabled = false)
   ↓
2. EmployeeService.setEnabled() guarda en BD
   ↓
3. EmployeeService.setEnabled() llama a invalidateUserSessions()
   ↓
4. SessionRegistry marca la sesión como expirada
   ↓
5. Usuario recarga la página
   ↓
6. UserValidationFilter se ejecuta ANTES de todo
   ↓
7. Filtro consulta la BD: ¿employee.getEnabled()?
   ↓
8. BD responde: enabled = false
   ↓
9. Filtro desloguea y redirige a /login ✅
```

---

## Por Qué el Filtro es Necesario (Resumen Técnico)

| Aspecto | Username/Password | Enabled |
|---------|------------------|---------|
| **Qué cambia** | Credenciales de autenticación | Estado de habilitación |
| **Dónde se valida** | SessionRegistry (en memoria) | UserDetails.disabled (en sesión) |
| **Spring detecta cambio** | ✅ Sí (sesión expirada) | ❌ No (UserDetails obsoleto) |
| **Necesita filtro** | ❌ No | ✅ Sí |
| **Re-consulta BD** | ❌ No necesita | ✅ Sí necesita |

**Conclusión:**
- `session.expireNow()` funciona para invalidar sesiones por username/password
- **NO** funciona para el campo `enabled` porque Spring no re-valida ese campo
- El filtro es necesario para **consultar la BD en cada request** y verificar `enabled`

---

## Optimización del Filtro

### Rutas Excluidas
El filtro **NO** se ejecuta en:
- `/login` - Página de login
- `/perform_login` - Procesamiento de login
- `/logout` - Cierre de sesión
- `/css/**`, `/js/**`, `/images/**` - Recursos estáticos

### Performance
- **Query adicional por request**: `SELECT * FROM employee WHERE username = ?`
- **Con índice en `username`**: ~1-2ms por query
- **Overhead aceptable**: Para 100 usuarios con 10 req/min = 1,000 queries/min

### Posibles Optimizaciones Futuras (si necesario)
1. **Cache con TTL corto** (5-30 segundos):
   ```java
   @Cacheable(value = "employeeStatus", key = "#username")
   public Boolean isEmployeeEnabled(String username) { ... }
   ```

2. **Event-driven con WebSockets**:
   - Cuando admin deshabilita usuario, publicar evento
   - Frontend escucha evento y cierra sesión automáticamente

---

## Archivos Modificados

### Creados
- ✅ `UserValidationFilter.java` - Valida `enabled` en cada request

### Modificados
- ✅ `SecurityConfig.java` - Agregado filtro antes de `UsernamePasswordAuthenticationFilter`

---

## Testing

### Caso de Prueba
1. **Navegador A**: Login como `empleado1`
2. **Navegador B**: Login como `admin`
3. **En B**: Cambiar estado de `empleado1` a **INACTIVO**
4. **En A**: Presionar F5 o hacer cualquier acción
5. **Resultado esperado**: Redirige a `/login` inmediatamente ✅

### Diferencia con el Comportamiento Anterior
- **Antes**: Usuario seguía logueado después de F5
- **Ahora**: Usuario es deslogueado inmediatamente al recargar

---

## Conclusión Final

El filtro `UserValidationFilter` **SÍ es necesario** porque:

1. ✅ Spring Security **no re-valida** el campo `enabled` en cada request
2. ✅ El `UserDetails.disabled` es una **instantánea** del momento del login
3. ✅ `session.expireNow()` funciona para username/password pero **no para enabled**
4. ✅ El filtro **fuerza la validación en tiempo real** contra la base de datos
5. ✅ El overhead de performance es **aceptable** y **optimizable** si es necesario

**Lección aprendida:** No todos los cambios en la entidad `Employee` se reflejan automáticamente en la sesión activa. El campo `enabled` requiere validación explícita en cada request.

---

**Estado:** ✅ Implementado y funcionando correctamente  
**Versión:** 3.0 (Final con explicación completa)
