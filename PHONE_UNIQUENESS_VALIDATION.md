# 📱 Validación de Unicidad de Teléfono - Implementación Completa

## 🎯 Objetivo

Asegurar que cada número de teléfono esté asociado a un único empleado, evitando duplicados en el sistema.

## 📋 Cambios Realizados

### 1. **Backend - Repository Layer** ✅

#### `EmployeeRepository.java`

Se agregaron dos nuevos métodos para verificar la existencia de teléfonos:

```java
/**
 * Find an employee by their phone number
 */
Optional<Employee> findByTelefono(String telefono);

/**
 * Check if an employee exists by phone number
 */
boolean existsByTelefono(String telefono);
```

### 2. **Backend - Service Layer** ✅

#### `EmployeeService.java`

**En el método `create()`:**

```java
// Check if phone number is already taken (if provided)
if (employee.getTelefono() != null && !employee.getTelefono().isEmpty() &&
    employeeRepository.existsByTelefono(employee.getTelefono())) {
    log.error("Employee with phone {} already exists", employee.getTelefono());
    throw new IllegalArgumentException("El teléfono '" + employee.getTelefono() + "' ya está registrado");
}
```

**En el método `update()`:**

```java
// Check if phone is being changed and if it's already taken (if provided)
if (employeeDetails.getTelefono() != null && !employeeDetails.getTelefono().isEmpty() &&
    !employeeDetails.getTelefono().equals(employee.getTelefono()) &&
    employeeRepository.existsByTelefono(employeeDetails.getTelefono())) {
    log.error("Phone {} already exists", employeeDetails.getTelefono());
    throw new IllegalArgumentException("El teléfono '" + employeeDetails.getTelefono() + "' ya está registrado");
}
```

### 3. **Backend - Controller Layer** ✅

#### `EmployeeController.java`

Se agregó un nuevo endpoint REST para validar la unicidad del teléfono en tiempo real:

```java
/**
 * Check if phone number is already registered
 */
@GetMapping("/check-phone")
@ResponseBody
public Map<String, Object> checkPhoneAvailability(
        @RequestParam String telefono,
        @RequestParam(required = false) Long employeeId) {

    Map<String, Object> response = new HashMap<>();

    try {
        // If phone is empty, it's valid
        if (telefono == null || telefono.trim().isEmpty()) {
            response.put("available", true);
            return response;
        }

        // Check if phone exists
        Optional<Employee> existingEmployee = employeeRepository.findByTelefono(telefono);

        if (existingEmployee.isPresent()) {
            // If it's the same employee being edited, it's valid
            if (employeeId != null && existingEmployee.get().getIdEmpleado().equals(employeeId)) {
                response.put("available", true);
            } else {
                response.put("available", false);
                response.put("message", "El teléfono ya está registrado");
            }
        } else {
            response.put("available", true);
        }

    } catch (Exception e) {
        log.error("Error checking phone availability", e);
        response.put("available", false);
        response.put("message", "Error al verificar el teléfono");
    }

    return response;
}
```

### 4. **Frontend - HTML** ✅

#### `form.html`

**Campo oculto para ID de empleado:**

```html
<!-- Hidden field for employee ID (used in edit mode) -->
<input type="hidden" name="idEmpleado" th:value="${employee.idEmpleado}" />
```

### 5. **Frontend - JavaScript Validation** ✅

#### Validación en tiempo real (AJAX)

```javascript
// Phone validation
const telefonoInput = document.getElementById("telefono");
let phoneCheckTimeout;

telefonoInput.addEventListener("input", function (e) {
  // Only allow digits, max 10
  this.value = this.value.replace(/\D/g, "").substring(0, 10);

  // Clear previous timeout
  clearTimeout(phoneCheckTimeout);

  // Check uniqueness after user stops typing (debounce)
  if (this.value.length === 10) {
    phoneCheckTimeout = setTimeout(() => {
      checkPhoneUniqueness(this.value);
    }, 500);
  }
});

// Function to check phone uniqueness via AJAX
async function checkPhoneUniqueness(telefono) {
  const employeeIdInput = document.querySelector('input[name="idEmpleado"]');
  const employeeId = employeeIdInput ? employeeIdInput.value : null;

  try {
    const params = new URLSearchParams({ telefono });
    if (employeeId) {
      params.append("employeeId", employeeId);
    }

    const response = await fetch(`/admin/employees/check-phone?${params}`);
    const data = await response.json();

    const telefonoInput = document.getElementById("telefono");

    if (!data.available) {
      telefonoInput.classList.add("border-red-500");
      telefonoInput.classList.remove("border-gray-300", "dark:border-gray-600");

      // Show error message below input
      let errorMsg =
        telefonoInput.parentElement.querySelector(".phone-error-msg");
      if (!errorMsg) {
        errorMsg = document.createElement("p");
        errorMsg.className =
          "phone-error-msg mt-2 text-xs text-red-600 dark:text-red-400 flex items-center gap-1";
        errorMsg.innerHTML =
          '<span class="material-symbols-outlined text-sm">error</span> Este teléfono ya está registrado';
        telefonoInput.parentElement.appendChild(errorMsg);
      }
    } else {
      telefonoInput.classList.remove("border-red-500");
      telefonoInput.classList.add("border-gray-300", "dark:border-gray-600");

      // Remove error message if exists
      const errorMsg =
        telefonoInput.parentElement.querySelector(".phone-error-msg");
      if (errorMsg) {
        errorMsg.remove();
      }
    }
  } catch (error) {
    console.error("Error checking phone uniqueness:", error);
  }
}
```

#### Validación antes de enviar el formulario

```javascript
// Check if phone has error (duplicate)
const phoneErrorMsg = form.querySelector(".phone-error-msg");
if (phoneErrorMsg) {
  event.preventDefault();
  Swal.fire({
    title: "Teléfono duplicado",
    html: "El <strong>teléfono</strong> que ingresaste ya está registrado por otro empleado. Por favor usa un número diferente.",
    icon: "error",
    confirmButtonColor: "#38e07b",
    confirmButtonText: "Entendido",
    customClass: {
      popup: "rounded-2xl",
      title: "text-xl font-bold",
      confirmButton: "rounded-xl px-6 py-2.5 font-semibold",
    },
  });
  return false;
}
```

## 🔄 Flujo de Validación

### 1. **Validación en Tiempo Real** (Frontend)

```
Usuario escribe teléfono (10 dígitos)
    ↓
Debounce de 500ms (espera a que termine de escribir)
    ↓
AJAX call a /admin/employees/check-phone
    ↓
Si está duplicado:
  - Borde rojo en input
  - Mensaje de error debajo del campo
Si está disponible:
  - Borde normal
  - Sin mensaje de error
```

### 2. **Validación al Enviar Formulario** (Frontend)

```
Usuario hace submit del formulario
    ↓
JavaScript validateForm() verifica si existe mensaje de error
    ↓
Si hay error:
  - Previene envío (event.preventDefault())
  - Muestra SweetAlert explicando el problema
Si no hay error:
  - Permite envío al backend
```

### 3. **Validación en Backend** (Última línea de defensa)

```
Request llega al Controller
    ↓
Se llama a EmployeeService.create() o update()
    ↓
Service verifica con employeeRepository.existsByTelefono()
    ↓
Si está duplicado:
  - Lanza IllegalArgumentException
  - Se muestra error en la vista
Si está disponible:
  - Se guarda el empleado
```

## ✨ Características Implementadas

### ✅ **Validación Triple Capa**

1. **HTML5**: `pattern`, `minlength`, `maxlength`
2. **JavaScript**: Validación en tiempo real + validación al submit
3. **Backend**: Validación en Service layer

### ✅ **Experiencia de Usuario Optimizada**

- **Feedback instantáneo**: Verifica mientras el usuario escribe
- **Debouncing**: No sobrecarga el servidor con requests
- **Mensajes claros**: Indica exactamente qué está mal
- **Visual feedback**: Borde rojo + ícono de error

### ✅ **Manejo de Casos Especiales**

- **Edición**: Permite mantener el mismo teléfono al editar
- **Teléfono opcional**: No obliga a llenar el campo
- **Validación de formato**: Solo 10 dígitos numéricos
- **Prevención de duplicados**: Verifica unicidad en BD

## 🎨 Elementos Visuales

### Estados del Campo Teléfono:

1. **Normal** (sin interacción)

   - Borde gris
   - Sin mensajes

2. **Válido** (teléfono disponible)

   - Borde verde (opcional)
   - Sin mensajes de error

3. **Inválido - Formato** (no son 10 dígitos)

   - Borde rojo
   - Validación HTML5 nativa

4. **Inválido - Duplicado** (ya existe en BD)
   - Borde rojo
   - Mensaje: "Este teléfono ya está registrado"
   - Ícono de error

## 📊 Endpoint API

### `GET /admin/employees/check-phone`

**Parámetros:**

- `telefono` (String, required): Número de teléfono a verificar
- `employeeId` (Long, optional): ID del empleado en edición

**Respuesta exitosa:**

```json
{
  "available": true
}
```

**Respuesta con duplicado:**

```json
{
  "available": false,
  "message": "El teléfono ya está registrado"
}
```

**Respuesta con error:**

```json
{
  "available": false,
  "message": "Error al verificar el teléfono"
}
```

## 🧪 Casos de Prueba

### Escenario 1: Crear nuevo empleado

1. Ingresar teléfono nuevo → ✅ Permite guardar
2. Ingresar teléfono existente → ❌ Muestra error y previene guardar

### Escenario 2: Editar empleado existente

1. Mantener mismo teléfono → ✅ Permite guardar
2. Cambiar a teléfono nuevo → ✅ Permite guardar
3. Cambiar a teléfono de otro empleado → ❌ Muestra error y previene guardar

### Escenario 3: Teléfono opcional

1. Dejar campo vacío → ✅ Permite guardar
2. Llenar campo y luego borrarlo → ✅ Permite guardar

## 🛡️ Seguridad

- ✅ Validación en múltiples capas (frontend + backend)
- ✅ Protección contra inyección SQL (uso de JPA)
- ✅ Sanitización de entrada (solo dígitos)
- ✅ Autorización en endpoint (Spring Security)

## 📝 Notas Importantes

1. El teléfono sigue siendo **opcional** - no es obligatorio llenar el campo
2. Si se proporciona, debe cumplir:
   - Exactamente 10 dígitos
   - Solo números
   - No estar duplicado
3. La validación en tiempo real usa **debouncing** para no sobrecargar el servidor
4. En modo edición, el empleado puede mantener su mismo teléfono

## 🚀 Próximos Pasos Sugeridos

1. ✅ **Implementado**: Validación de unicidad de teléfono
2. 🔄 **Opcional**: Agregar validación de formato internacional
3. 🔄 **Opcional**: Permitir múltiples números por empleado
4. 🔄 **Opcional**: Validar números con APIs de telefonía

---

**Implementado el:** 8 de Noviembre, 2025
**Sistema:** El Gran Sazón - Restaurant Management System
**Módulo:** Gestión de Empleados
