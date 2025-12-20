# Separación de Contraseña en Formulario de Empleados

## 📋 Problema Resuelto

**Síntoma**: Al editar un empleado (incluyendo el propio usuario con sesión iniciada), la sesión se cerraba automáticamente después de guardar los cambios.

**Causa raíz**: El campo de contraseña estaba incluido en el mismo formulario que los datos del perfil. Spring Security invalida la sesión cuando se cambia la contraseña, lo que provocaba el logout automático.

## ✅ Solución Implementada

Se separó el cambio de contraseña en un formulario independiente, siguiendo el mismo patrón usado en la vista de clientes (`client/profile.html`).

### Cambios en `admin/employees/form.html`

#### 1. Campo de Contraseña en Formulario Principal

- **Antes**: Contraseña opcional en edición, incluida en el formulario principal
- **Después**: Contraseña solo aparece para nuevos empleados (obligatoria)

```html
<!-- Password (Only for new employees) -->
<div th:if="${employee.idEmpleado == null}">
  <label for="password">
    <span class="material-symbols-outlined">lock</span>
    Contraseña
    <span class="text-red-500">*</span>
  </label>
  <input
    type="password"
    name="password"
    id="password"
    required
    minlength="6"
    placeholder="••••••••"
  />
  <p>Mínimo 6 caracteres</p>
</div>
```

#### 2. Nueva Sección de Cambio de Contraseña

- **Ubicación**: Después del formulario principal, antes del botón de eliminar
- **Visibilidad**: Solo en modo edición (`th:if="${employee.idEmpleado != null}"`)
- **Estilo**: Tema naranja/ámbar (color de seguridad)

```html
<form
  th:if="${employee.idEmpleado != null}"
  th:action="@{/admin/employees/{id}/change-password(id=${employee.idEmpleado})}"
  method="post"
  id="changePasswordForm"
>
  <div>
    <h2>Cambiar Contraseña</h2>
    <p>Actualiza la contraseña del empleado</p>

    <!-- Nueva Contraseña -->
    <input
      type="password"
      name="newPassword"
      id="newPassword"
      required
      minlength="6"
    />

    <!-- Confirmar Contraseña -->
    <input
      type="password"
      name="confirmPassword"
      id="confirmPassword"
      required
      minlength="6"
    />

    <button type="submit">Cambiar Contraseña</button>

    <div class="alert-info">
      <strong>Importante:</strong> El empleado deberá usar la nueva contraseña
      en su próximo inicio de sesión.
    </div>
  </div>
</form>
```

#### 3. Validación JavaScript Actualizada

**Formulario Principal** (`validateForm`):

- Eliminada validación de contraseña en modo edición
- Solo valida contraseña para nuevos empleados

```javascript
// Validate password (only for new employees now)
if (!isEdit && (!password || password.length < 6)) {
  // Show error
  return false;
}
```

**Nuevo: Validación de Cambio de Contraseña**:

```javascript
const changePasswordForm = document.getElementById("changePasswordForm");
if (changePasswordForm) {
  changePasswordForm.addEventListener("submit", function (event) {
    const newPassword = document.getElementById("newPassword").value;
    const confirmPassword = document.getElementById("confirmPassword").value;

    // Validate minimum length
    if (newPassword.length < 6) {
      // Show error
      return false;
    }

    // Validate passwords match
    if (newPassword !== confirmPassword) {
      // Show error
      return false;
    }
  });

  // Real-time password match validation
  function validatePasswordMatch() {
    // Visual feedback if passwords don't match
  }
}
```

### Cambios en `EmployeeController.java`

#### 1. Método `updateEmployee()` Actualizado

- **Eliminado**: Parámetro `@RequestParam password`
- **Eliminado**: Lógica de actualización de contraseña
- **Agregado**: Preservación de contraseña existente

```java
@PostMapping("/{id}")
public String updateEmployee(
        @PathVariable Long id,
        @Valid @ModelAttribute("employee") Employee employee,
        @RequestParam(value = "roleId", required = false) Long roleId,
        @RequestParam(value = "supervisorId", required = false) Long supervisorId,
        // ... otros parámetros
) {
    // ... validaciones

    // Preserve existing password (no password change in this endpoint)
    employee.setContrasenia(existingEmployee.getContrasenia());

    // ... resto de la lógica
}
```

#### 2. Nuevo Método `changeEmployeePassword()`

- **Ruta**: `POST /admin/employees/{id}/change-password`
- **Seguridad**: `@PreAuthorize("hasRole('ROLE_ADMIN')")`
- **Validaciones**:
  - Contraseñas coinciden
  - Longitud mínima de 6 caracteres
- **Comportamiento**: Solo actualiza la contraseña, no afecta otros datos

```java
@PreAuthorize("hasRole('ROLE_ADMIN')")
@PostMapping("/{id}/change-password")
public String changeEmployeePassword(
        @PathVariable Long id,
        @RequestParam("newPassword") String newPassword,
        @RequestParam("confirmPassword") String confirmPassword,
        Authentication authentication,
        RedirectAttributes redirectAttributes) {

    // Validate passwords match
    if (!newPassword.equals(confirmPassword)) {
        redirectAttributes.addFlashAttribute("errorMessage",
                "Las contraseñas no coinciden");
        return "redirect:/admin/employees/" + id + "/edit";
    }

    // Validate minimum length
    if (newPassword.length() < 6) {
        redirectAttributes.addFlashAttribute("errorMessage",
                "La contraseña debe tener al menos 6 caracteres");
        return "redirect:/admin/employees/" + id + "/edit";
    }

    // Get employee and update password
    Employee employee = employeeService.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado"));

    employee.setContrasenia(newPassword);
    String currentUsername = authentication.getName();
    Employee updated = employeeService.update(id, employee, currentUsername);

    redirectAttributes.addFlashAttribute("successMessage",
            "Contraseña actualizada exitosamente para " + updated.getFullName());
    return "redirect:/admin/employees/" + id + "/edit";
}
```

## 🎯 Resultados

### Antes

- ❌ Editar datos del empleado cerraba la sesión
- ❌ Cambio de contraseña accidental al actualizar perfil
- ❌ Usuario logueado perdía su sesión al editar su propio perfil

### Después

- ✅ Editar datos del empleado mantiene la sesión activa
- ✅ Contraseña solo se cambia cuando se usa el formulario específico
- ✅ Usuario logueado puede editar su perfil sin perder sesión
- ✅ Separación clara entre actualización de perfil y cambio de contraseña
- ✅ Mayor seguridad: cambio de contraseña requiere confirmación

## 🔒 Seguridad

### Restricciones

- Solo usuarios con rol `ROLE_ADMIN` pueden cambiar contraseñas
- El cambio de contraseña requiere confirmación (doble entrada)
- Longitud mínima: 6 caracteres
- Validación en frontend y backend

### Preservación de ADMIN

- Los empleados con rol ADMIN mantienen su rol protegido
- El cambio de contraseña no afecta la protección del rol ADMIN

## 📝 Flujo de Uso

### Editar Datos de Empleado (Sin cambiar contraseña)

1. Admin accede a `/admin/employees/{id}/edit`
2. Modifica nombre, apellido, teléfono, salario, etc.
3. Hace clic en "Actualizar Empleado"
4. ✅ Datos actualizados, sesión activa, contraseña sin cambios

### Cambiar Contraseña de Empleado

1. Admin accede a `/admin/employees/{id}/edit`
2. Desplaza hacia abajo a la sección "Cambiar Contraseña"
3. Ingresa nueva contraseña
4. Confirma nueva contraseña
5. Hace clic en "Cambiar Contraseña"
6. ✅ Solo la contraseña se actualiza, el empleado debe usar la nueva contraseña

## 🎨 Características de UI

### Sección de Cambio de Contraseña

- **Color distintivo**: Gradiente naranja/ámbar (indica operación de seguridad)
- **Icono**: `lock_reset` (candado con flecha de renovación)
- **Mensaje informativo**: Alerta azul explicando que el empleado debe usar la nueva contraseña
- **Validación en tiempo real**: El campo de confirmación muestra borde rojo si no coincide
- **Diseño responsivo**: Funciona en dispositivos móviles

### Mensajes de Éxito/Error

- **Éxito**: "Contraseña actualizada exitosamente para [Nombre Completo]"
- **Error - No coinciden**: "Las contraseñas no coinciden"
- **Error - Muy corta**: "La contraseña debe tener al menos 6 caracteres"

## 🔄 Compatibilidad

### Creación de Nuevos Empleados

- ✅ La contraseña sigue siendo obligatoria
- ✅ Campo de contraseña visible solo en modo creación
- ✅ Validación de longitud mínima (6 caracteres)
- ✅ No hay cambios en el flujo de creación

### Empleados con Rol ADMIN

- ✅ Pueden cambiar su contraseña
- ✅ Su rol permanece protegido (no puede cambiarse)
- ✅ La protección de rol es independiente del cambio de contraseña

## 📚 Patrón de Referencia

Esta implementación sigue el mismo patrón usado en `client/profile.html`:

| Archivo                     | Formulario 1                                  | Formulario 2                                                 |
| --------------------------- | --------------------------------------------- | ------------------------------------------------------------ |
| `client/profile.html`       | Actualizar Perfil (`/client/profile/update`)  | Cambiar Contraseña (`/client/profile/change-password`)       |
| `admin/employees/form.html` | Actualizar Empleado (`/admin/employees/{id}`) | Cambiar Contraseña (`/admin/employees/{id}/change-password`) |

## ✨ Mejoras Futuras Opcionales

1. **Verificación de contraseña actual**: Requerir contraseña actual del admin para cambios
2. **Historial de cambios**: Registrar auditoría de cambios de contraseña
3. **Requisitos de complejidad**: Validar mayúsculas, números, caracteres especiales
4. **Expiración de contraseñas**: Sistema de cambio periódico obligatorio
5. **Notificación por email**: Avisar al empleado cuando su contraseña es cambiada

## 🐛 Solución de Problemas

### La sesión sigue cerrándose

- Verificar que el formulario principal NO tenga campo `password`
- Confirmar que `updateEmployee()` preserve la contraseña existente
- Revisar que el formulario de cambio envíe a `/change-password`

### No aparece la sección de cambio de contraseña

- Verificar que estés en modo edición (no creación)
- Confirmar que `employee.idEmpleado != null`
- Revisar que no haya errores JavaScript en consola

### Validación no funciona

- Verificar que `changePasswordForm` exista en el DOM
- Confirmar que los IDs `newPassword` y `confirmPassword` sean correctos
- Revisar consola del navegador para errores JavaScript

---

**Fecha de implementación**: 2024
**Desarrollador**: Sistema de Gestión El Gran Sazón
**Relacionado con**: ADMIN Role Protection, Customer Profile Management
