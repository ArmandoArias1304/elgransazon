# DTO Solution - Password Corruption Fix

## Problema Original

Al actualizar el perfil del cliente, la contraseña se corrompe y el usuario no puede volver a iniciar sesión.

### Root Cause (Causa Raíz)
- El formulario de perfil usaba `th:object="${customer}"` que vincula el **entity completo** JPA
- Esto incluye TODOS los campos del entity, incluyendo el `password` con hash de la base de datos
- Aunque el formulario no tenía input de contraseña visible, Spring MVC incluía el campo password del objeto Customer
- El servicio intentaba detectar si el password era nuevo o existente comparando strings
- Esta lógica fallaba y re-hasheaba el hash, corrompiéndolo

---

## Solución Implementada: DTOs (Data Transfer Objects)

### 1. UpdateProfileDTO.java
**Ubicación:** `src/main/java/com/aatechsolutions/elgransazon/presentation/dto/UpdateProfileDTO.java`

```java
@Data
public class UpdateProfileDTO {
    
    @NotBlank(message = "El nombre completo es requerido")
    private String fullName;
    
    @NotBlank(message = "El nombre de usuario es requerido")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Solo letras, números y guión bajo")
    private String username;
    
    @NotBlank(message = "El teléfono es requerido")
    @Pattern(regexp = "^[+]?[0-9\\-\\s()]{7,20}$", message = "Formato de teléfono inválido")
    private String phone;
    
    private String address; // Optional
}
```

**Características:**
- ❌ **NO contiene campo `password`** - Imposible enviar password desde este formulario
- ✅ Solo incluye campos editables del perfil
- ✅ Validaciones propias para cada campo
- ✅ Evita exponer contraseña hasheada de la base de datos

---

### 2. ChangePasswordDTO.java
**Ubicación:** `src/main/java/com/aatechsolutions/elgransazon/presentation/dto/ChangePasswordDTO.java`

```java
@Data
public class ChangePasswordDTO {
    
    @NotBlank(message = "La nueva contraseña es requerida")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String newPassword;
    
    @NotBlank(message = "Debe confirmar la contraseña")
    private String confirmPassword;
    
    public boolean passwordsMatch() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
```

**Características:**
- ✅ **Solo para cambio de contraseña**
- ✅ Incluye confirmación de contraseña
- ✅ Método helper `passwordsMatch()` para validar que coincidan
- ✅ Validaciones específicas de contraseña

---

## 3. ClientController.java - Cambios

### A. Imports Añadidos
```java
import com.aatechsolutions.elgransazon.presentation.dto.ChangePasswordDTO;
import com.aatechsolutions.elgransazon.presentation.dto.UpdateProfileDTO;
import org.springframework.security.crypto.password.PasswordEncoder;
```

### B. PasswordEncoder Inyectado
```java
private final PasswordEncoder passwordEncoder;

public ClientController(..., PasswordEncoder passwordEncoder) {
    // ...
    this.passwordEncoder = passwordEncoder;
}
```

### C. Método showProfile() - Actualizado
```java
@GetMapping("/profile")
public String showProfile(Model model, Authentication authentication) {
    Customer customer = customerService.findByUsernameOrEmail(authentication.getName())
            .orElseThrow(() -> new IllegalStateException("Cliente no encontrado"));
    
    // Crear DTO para formulario de perfil (sin password)
    UpdateProfileDTO profileDTO = new UpdateProfileDTO();
    profileDTO.setFullName(customer.getFullName());
    profileDTO.setUsername(customer.getUsername());
    profileDTO.setPhone(customer.getPhone());
    profileDTO.setAddress(customer.getAddress());
    
    model.addAttribute("customer", customer); // Para mostrar email (read-only)
    model.addAttribute("profileDTO", profileDTO); // Para form binding
    model.addAttribute("passwordDTO", new ChangePasswordDTO()); // Para form de password
    return "client/profile";
}
```

**Cambios:**
- ✅ Crea `UpdateProfileDTO` poblado con datos del Customer
- ✅ Crea `ChangePasswordDTO` vacío
- ✅ Ambos DTOs disponibles en el modelo para los formularios

### D. Método updateProfile() - Refactorizado
```java
@PostMapping("/profile/update")
public String updateProfile(
        @Valid @ModelAttribute UpdateProfileDTO profileDTO, // ✅ USA DTO
        BindingResult bindingResult,
        Authentication authentication,
        Model model,
        RedirectAttributes redirectAttributes) {
    
    // Validaciones de formulario
    if (bindingResult.hasErrors()) {
        // ...
        return "client/profile";
    }
    
    Customer existing = customerService.findByUsernameOrEmail(authentication.getName())
            .orElseThrow();
    
    // Validar username y phone únicos
    if (!existing.getUsername().equalsIgnoreCase(profileDTO.getUsername()) && 
        customerService.existsByUsername(profileDTO.getUsername())) {
        bindingResult.rejectValue("username", "error.customer", "El nombre de usuario ya está en uso");
        return "client/profile";
    }
    
    // ✅ ACTUALIZAR SOLO CAMPOS DEL DTO (SIN PASSWORD)
    existing.setFullName(profileDTO.getFullName());
    existing.setUsername(profileDTO.getUsername());
    existing.setPhone(profileDTO.getPhone());
    existing.setAddress(profileDTO.getAddress());
    
    customerService.update(existing.getIdCustomer(), existing);
    
    redirectAttributes.addFlashAttribute("successMessage", "Perfil actualizado exitosamente");
    return "redirect:/client/profile";
}
```

**Cambios:**
- ❌ **NO usa más** `@ModelAttribute("customer") Customer customer`
- ✅ **USA** `@ModelAttribute UpdateProfileDTO profileDTO`
- ✅ Solo actualiza campos del DTO (fullName, username, phone, address)
- ✅ **Password NUNCA se toca** en este método

### E. Método changePassword() - Refactorizado
```java
@PostMapping("/profile/change-password")
public String changePassword(
        @Valid @ModelAttribute ChangePasswordDTO passwordDTO, // ✅ USA DTO
        BindingResult bindingResult,
        Authentication authentication,
        RedirectAttributes redirectAttributes) {
    
    // Validar errores de formulario
    if (bindingResult.hasErrors()) {
        redirectAttributes.addFlashAttribute("errorMessage", "Por favor corrige los errores");
        return "redirect:/client/profile";
    }
    
    // Validar que las contraseñas coincidan
    if (!passwordDTO.passwordsMatch()) {
        redirectAttributes.addFlashAttribute("errorMessage", "Las contraseñas no coinciden");
        return "redirect:/client/profile";
    }
    
    Customer existing = customerService.findByUsernameOrEmail(authentication.getName())
            .orElseThrow();
    
    // ✅ ENCODE PASSWORD EN CONTROLLER (no en service)
    String encodedPassword = passwordEncoder.encode(passwordDTO.getNewPassword());
    existing.setPassword(encodedPassword);
    customerService.update(existing.getIdCustomer(), existing);
    
    redirectAttributes.addFlashAttribute("successMessage", "Contraseña actualizada exitosamente");
    return "redirect:/client/profile";
}
```

**Cambios:**
- ❌ **NO usa más** `@RequestParam String newPassword, @RequestParam String confirmPassword`
- ✅ **USA** `@ModelAttribute ChangePasswordDTO passwordDTO`
- ✅ Valida usando método helper `passwordsMatch()`
- ✅ **CODIFICA password en el controller** con `passwordEncoder.encode()`
- ✅ Pasa password ya codificado al servicio

---

## 4. CustomerServiceImpl.java - Simplificado

### ANTES (Lógica Compleja y FALLIDA)
```java
public Customer update(Long id, Customer customer) {
    Customer existing = customerRepository.findById(id).orElseThrow();
    
    String originalPassword = existing.getPassword();
    
    existing.setFullName(customer.getFullName());
    existing.setUsername(customer.getUsername());
    existing.setPhone(customer.getPhone());
    existing.setAddress(customer.getAddress());
    
    // ❌ LÓGICA COMPLEJA QUE FALLABA
    if (customer.getPassword() != null && 
        !customer.getPassword().trim().isEmpty() && 
        !customer.getPassword().equals(originalPassword)) {
        existing.setPassword(passwordEncoder.encode(customer.getPassword()));
    } else {
        existing.setPassword(originalPassword);
    }
    
    return customerRepository.save(existing);
}
```

### DESPUÉS (Lógica Simple)
```java
public Customer update(Long id, Customer customer) {
    Customer existing = customerRepository.findById(id).orElseThrow();
    
    // Update all fields from customer object
    // Controller is responsible for what fields to include
    existing.setFullName(customer.getFullName());
    existing.setUsername(customer.getUsername());
    existing.setPhone(customer.getPhone());
    existing.setAddress(customer.getAddress());
    
    // ✅ SI PASSWORD ESTÁ PRESENTE, YA VIENE CODIFICADO DEL CONTROLLER
    if (customer.getPassword() != null && !customer.getPassword().trim().isEmpty()) {
        existing.setPassword(customer.getPassword());
    }
    // Otherwise, password remains unchanged
    
    return customerRepository.save(existing);
}
```

**Cambios:**
- ❌ **Elimina** lógica compleja de detección de hash vs plaintext
- ❌ **Elimina** `passwordEncoder` del servicio
- ✅ **Asume** que password viene ya codificado desde el controller
- ✅ Si password no viene (null o empty), no se modifica

**Responsabilidad clara:**
- **Controller:** Decide QUÉ actualizar y codifica password si es necesario
- **Service:** Solo persiste lo que le pasan

---

## 5. profile.html - Vista Actualizada

### Formulario de Perfil
**ANTES:**
```html
<form th:action="@{/client/profile/update}" th:object="${customer}" method="post">
  <input th:field="*{fullName}" />
  <input th:field="*{username}" />
  <!-- ... otros campos ... -->
</form>
```

**DESPUÉS:**
```html
<form th:action="@{/client/profile/update}" th:object="${profileDTO}" method="post">
  <input th:field="*{fullName}" />
  <input th:field="*{username}" />
  <input th:field="*{phone}" />
  <textarea th:field="*{address}"></textarea>
  <!-- Email es read-only y usa ${customer.email} directamente -->
</form>
```

**Cambios:**
- ❌ **NO usa más** `th:object="${customer}"`
- ✅ **USA** `th:object="${profileDTO}"`
- ✅ Imposible que incluya campo password

### Formulario de Contraseña
**ANTES:**
```html
<form th:action="@{/client/profile/change-password}" method="post">
  <input type="password" name="newPassword" />
  <input type="password" name="confirmPassword" />
</form>
```

**DESPUÉS:**
```html
<form th:action="@{/client/profile/change-password}" th:object="${passwordDTO}" method="post">
  <input type="password" th:field="*{newPassword}" />
  <div th:if="${#fields.hasErrors('newPassword')}" th:errors="*{newPassword}"></div>
  
  <input type="password" th:field="*{confirmPassword}" />
  <div th:if="${#fields.hasErrors('confirmPassword')}" th:errors="*{confirmPassword}"></div>
</form>
```

**Cambios:**
- ✅ **USA** `th:object="${passwordDTO}"` para binding
- ✅ **USA** `th:field` en lugar de `name` para validaciones de Spring
- ✅ Muestra errores de validación con `th:errors`

---

## Beneficios de la Solución DTO

### 1. Seguridad ✅
- **NO expone** password hasheado de la base de datos
- **Previene** que password se incluya accidentalmente en actualizaciones de perfil
- **Separación clara** entre datos de perfil y datos sensibles (password)

### 2. Validación ✅
- **Validaciones específicas** por DTO
- **Mensajes de error** claros y contextuales
- **Binding de formulario** con Spring Validation (@Valid)

### 3. Mantenibilidad ✅
- **Código más limpio** y fácil de entender
- **Responsabilidades claras:**
  - DTOs: Transferencia de datos y validación
  - Controller: Lógica de presentación y codificación de password
  - Service: Lógica de negocio (simple, sin password encoding)
  - Entity: Modelo de dominio

### 4. Evita Bugs ✅
- **Imposible** corromper password desde formulario de perfil
- **No más lógica compleja** de detección de hash vs plaintext
- **Controller controla** qué campos se actualizan

---

## Testing del Fix

### Pasos para Probar:

1. **Resetear Password Corrupto (si es necesario):**
   ```sql
   -- Ejecutar FIX_CORRUPTED_PASSWORD.sql
   UPDATE customers 
   SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhka'
   WHERE email = 'pablo@gmail.com';
   -- Credenciales: pablo@gmail.com / password123
   ```

2. **Login:**
   - Iniciar sesión con `pablo@gmail.com` / `password123`

3. **Actualizar Perfil (SIN cambiar password):**
   - Ir a "Mi Perfil"
   - Cambiar nombre, username, teléfono, dirección
   - Click "Guardar Información"
   - ✅ **Verificar mensaje de éxito**

4. **Verificar Password NO se corrompió:**
   - Cerrar sesión
   - Iniciar sesión con mismo email/username y password
   - ✅ **Debe poder entrar SIN problemas**

5. **Cambiar Password:**
   - Ir a "Mi Perfil"
   - En sección "Cambiar Contraseña"
   - Nueva contraseña: `nuevaPassword456`
   - Confirmar: `nuevaPassword456`
   - Click "Cambiar Contraseña"
   - ✅ **Verificar mensaje de éxito**

6. **Verificar Nuevo Password Funciona:**
   - Cerrar sesión
   - Iniciar sesión con email/username y `nuevaPassword456`
   - ✅ **Debe poder entrar con nueva contraseña**

---

## Archivos Modificados

### Nuevos Archivos Creados:
1. ✅ `UpdateProfileDTO.java`
2. ✅ `ChangePasswordDTO.java`
3. ✅ `DTO_SOLUTION_PASSWORD_FIX.md` (este documento)

### Archivos Modificados:
1. ✅ `ClientController.java`
   - Imports para DTOs y PasswordEncoder
   - Constructor con PasswordEncoder
   - Método `showProfile()` - Crea y pasa DTOs al modelo
   - Método `updateProfile()` - USA UpdateProfileDTO
   - Método `changePassword()` - USA ChangePasswordDTO y codifica password

2. ✅ `CustomerServiceImpl.java`
   - Método `update()` - Simplificado, SIN lógica de password encoding

3. ✅ `profile.html`
   - Form de perfil: `th:object="${profileDTO}"`
   - Form de password: `th:object="${passwordDTO}"`
   - Campos con `th:field` y validaciones con `th:errors`

---

## Conclusión

✅ **El problema de corrupción de password está RESUELTO**

La solución DTO es:
- **Más segura:** Password nunca se expone en formulario de perfil
- **Más mantenible:** Código más simple y responsabilidades claras
- **Best Practice:** Usar DTOs es el estándar en Spring Boot para forms
- **Escalable:** Fácil agregar más campos o validaciones específicas

**No más password corrupto al actualizar perfil! 🎉**
