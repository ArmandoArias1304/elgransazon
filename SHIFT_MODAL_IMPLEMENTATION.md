# Gestión de Turnos en Edición de Empleados

## Fecha: 16 de Octubre de 2025

## Requerimiento

Cuando se edita un empleado, en lugar del select de turnos, mostrar un **botón "Agregar turnos"** que al hacer clic abra una ventana modal con las siguientes características:

### Características del Modal
1. ✅ Mostrar **todos los turnos disponibles**
2. ✅ Los turnos **actualmente asignados** deben aparecer **marcados (checked)**
3. ✅ Los turnos ya asignados **NO se pueden deseleccionar** (checkbox disabled)
4. ✅ Solo se pueden **agregar nuevos turnos**, no remover
5. ✅ Para remover turnos, el usuario debe ir al **módulo de Turnos**
6. ✅ Usar el endpoint `POST /admin/shifts/{id}/assign` para asignar

---

## Implementación

### 1. Modificación del Formulario (form.html)

#### Modo Crear (New Employee)
Cuando `employee.idEmpleado == null`:
- Se muestra el **select tradicional**
- Se puede seleccionar **solo un turno**
- Comportamiento original sin cambios

```html
<div th:if="${employee.idEmpleado == null}">
    <select id="shift" name="shiftId" ...>
        <option value="">Sin turno asignado...</option>
        <option th:each="shift : ${shifts}" ...>
        </option>
    </select>
    <p>Solo se puede asignar un turno en la creación</p>
</div>
```

#### Modo Editar (Edit Employee)
Cuando `employee.idEmpleado != null`:
- Se muestra un **botón "Gestionar Turnos"**
- Al hacer clic, abre un **modal**
- Muestra los turnos **actualmente asignados**

```html
<div th:if="${employee.idEmpleado != null}">
    <button type="button" onclick="openShiftModal()" ...>
        <span>
            <svg>...</svg>
            Gestionar Turnos
        </span>
    </button>
    <p>Actualmente asignado(s): {{ turnos actuales }}</p>
    <p class="text-red-600">Nota: Solo se pueden agregar turnos aquí...</p>
</div>
```

### 2. Modal de Gestión de Turnos

#### Estructura del Modal
```html
<div id="shiftModal" class="hidden fixed inset-0...">
    <div class="relative top-20 mx-auto...">
        <!-- Header -->
        <div class="flex justify-between...">
            <h3>Gestionar Turnos</h3>
            <button onclick="closeShiftModal()">X</button>
        </div>

        <!-- Body -->
        <div class="mt-4">
            <!-- Info Message -->
            <div class="bg-blue-50...">
                <p><strong>Nota:</strong> Los turnos ya asignados no se pueden deseleccionar...</p>
            </div>

            <!-- Shifts List -->
            <form id="shiftAssignmentForm">
                <input type="hidden" name="employeeId" value="{{id}}" />
                
                <div th:each="shift : ${shifts}">
                    <input type="checkbox" 
                           th:checked="${employee.shifts.contains(shift)}"
                           th:disabled="${employee.shifts.contains(shift)}"
                           name="shiftIds" />
                    <label>
                        <div>{{ shift.name }}</div>
                        <div>{{ shift.timeRange }}</div>
                        <div>Días: {{ shift.daysNames }}</div>
                    </label>
                    <span th:if="asignado">Asignado</span>
                </div>
            </form>
        </div>

        <!-- Footer -->
        <div class="flex justify-end...">
            <button onclick="closeShiftModal()">Cancelar</button>
            <button onclick="assignShifts()">Asignar Turnos Seleccionados</button>
        </div>
    </div>
</div>
```

### 3. Lógica JavaScript

#### Funciones del Modal
```javascript
// Abrir modal
function openShiftModal() {
    document.getElementById('shiftModal').classList.remove('hidden');
}

// Cerrar modal
function closeShiftModal() {
    document.getElementById('shiftModal').classList.add('hidden');
}

// Cerrar al hacer click fuera del modal
document.getElementById('shiftModal')?.addEventListener('click', function(e) {
    if (e.target === this) {
        closeShiftModal();
    }
});
```

#### Función de Asignación
```javascript
function assignShifts() {
    const form = document.getElementById('shiftAssignmentForm');
    const employeeId = form.querySelector('input[name="employeeId"]').value;
    
    // Obtener SOLO los checkboxes seleccionados que NO están deshabilitados
    const checkboxes = form.querySelectorAll('input[name="shiftIds"]:checked:not(:disabled)');
    const selectedShiftIds = Array.from(checkboxes).map(cb => cb.value);
    
    if (selectedShiftIds.length === 0) {
        alert('No hay nuevos turnos seleccionados para asignar.');
        return;
    }

    // Mostrar estado de carga
    button.textContent = 'Asignando...';
    button.disabled = true;

    // Asignar cada turno usando el endpoint POST /admin/shifts/{id}/assign
    const assignmentPromises = selectedShiftIds.map(shiftId => {
        return fetch(`/admin/shifts/${shiftId}/assign`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: `employeeIds=${employeeId}`
        });
    });

    // Ejecutar todas las asignaciones en paralelo
    Promise.all(assignmentPromises)
        .then(responses => {
            const allSuccessful = responses.every(r => r.ok);
            if (allSuccessful) {
                alert(`${selectedShiftIds.length} turno(s) asignado(s) exitosamente.`);
                window.location.reload(); // Recargar para ver los cambios
            } else {
                throw new Error('Algunos turnos no pudieron ser asignados');
            }
        })
        .catch(error => {
            alert('Error al asignar los turnos. Por favor, intente nuevamente.');
            button.textContent = originalText;
            button.disabled = false;
        });
}
```

---

## Flujo Completo

### Escenario: Agregar Turnos a un Empleado Existente

```
1. Admin abre formulario de edición de empleado
   ↓
2. En la sección "Turnos", ve botón "Gestionar Turnos"
   ↓
3. Admin hace clic en el botón
   ↓
4. Se abre modal con lista de todos los turnos
   ↓
5. Turnos ya asignados aparecen:
   - Checkbox: ✓ (marcado)
   - Checkbox: disabled (no se puede deseleccionar)
   - Badge: "Asignado" (verde)
   ↓
6. Admin marca nuevos turnos adicionales
   ↓
7. Admin hace clic en "Asignar Turnos Seleccionados"
   ↓
8. JavaScript filtra solo checkboxes :checked:not(:disabled)
   ↓
9. Para cada turno nuevo:
   - POST /admin/shifts/{shiftId}/assign
   - Body: employeeIds={employeeId}
   ↓
10. Si todos exitosos: Recarga página
   ↓
11. Empleado ahora tiene los turnos adicionales asignados
```

---

## Características de Seguridad

### Prevención de Remoción Accidental
```html
<!-- Checkbox deshabilitado para turnos ya asignados -->
<input type="checkbox" 
       th:checked="${employee.shifts.contains(shift)}"
       th:disabled="${employee.shifts.contains(shift)}"  ← CLAVE
       ... />
```

### Filtrado en JavaScript
```javascript
// Solo obtiene checkboxes que:
// 1. Están marcados (:checked)
// 2. NO están deshabilitados (:not(:disabled))
const checkboxes = form.querySelectorAll('input[name="shiftIds"]:checked:not(:disabled)');
```

**Resultado:** Imposible enviar turnos ya asignados en la petición.

---

## Ventajas de la Implementación

| Aspecto | Beneficio |
|---------|-----------|
| **UX** | Interfaz clara que diferencia turnos asignados vs disponibles |
| **Seguridad** | Imposible remover turnos accidentalmente |
| **Separación de Responsabilidades** | Asignación en empleados, remoción en módulo de turnos |
| **Visual Feedback** | Badge "Asignado" + checkbox disabled |
| **Validación** | Alerta si no hay turnos nuevos seleccionados |
| **Reutilización** | Usa endpoint existente `/admin/shifts/{id}/assign` |
| **Estado de Carga** | Botón muestra "Asignando..." durante proceso |
| **Manejo de Errores** | Catch y alert si falla la asignación |

---

## Diferencias: Crear vs Editar

### Modo Crear
- ✅ Select simple (un solo turno)
- ✅ No hay turnos pre-asignados
- ✅ Comportamiento tradicional

### Modo Editar
- ✅ Botón "Gestionar Turnos"
- ✅ Modal con todos los turnos
- ✅ Turnos asignados: marcados + disabled
- ✅ Turnos nuevos: disponibles para marcar
- ✅ Solo permite agregar, no remover

---

## Endpoint Utilizado

### POST /admin/shifts/{id}/assign

**Parámetros:**
- `employeeIds` (form data): ID(s) del empleado a asignar

**Respuesta:**
- Redirect con mensaje de éxito/error

**En este caso:**
- Se llama **múltiples veces** (una por cada turno nuevo)
- Se usa `Promise.all()` para ejecutar en paralelo
- Si alguno falla, se muestra error

---

## Testing Manual

### Caso de Prueba 1: Agregar Turnos
1. Editar empleado que tiene 1 turno asignado
2. Click en "Gestionar Turnos"
3. Ver turno actual marcado y deshabilitado
4. Marcar 2 turnos nuevos
5. Click en "Asignar Turnos Seleccionados"
6. **Resultado esperado:** Empleado ahora tiene 3 turnos

### Caso de Prueba 2: Intentar Deseleccionar
1. Editar empleado con turnos asignados
2. Click en "Gestionar Turnos"
3. Intentar des-marcar turno asignado
4. **Resultado esperado:** No se puede (checkbox disabled)

### Caso de Prueba 3: Sin Turnos Nuevos
1. Editar empleado
2. Click en "Gestionar Turnos"
3. No marcar ningún turno nuevo
4. Click en "Asignar"
5. **Resultado esperado:** Alert "No hay nuevos turnos seleccionados"

---

## Archivos Modificados

### ✅ Modificados
- `src/main/resources/templates/admin/employees/form.html`
  - Líneas 198-242: Reemplazado select por botón (en modo editar)
  - Líneas 290-430: Agregado modal y JavaScript

### ❌ No Modificados
- `EmployeeController.java` - No requiere cambios
- `ShiftController.java` - Endpoint ya existía
- `EmployeeService.java` - No requiere cambios

---

## Mejoras Futuras (Opcionales)

- [ ] Agregar filtro de búsqueda en el modal
- [ ] Mostrar número de empleados por turno
- [ ] Agregar animación al abrir/cerrar modal
- [ ] Implementar drag & drop para asignar
- [ ] Mostrar conflictos de horario en tiempo real
- [ ] Toast notification en lugar de alert()

---

**Estado:** ✅ Implementado y listo para testing  
**Fecha:** 16 de Octubre de 2025  
**Versión:** 1.0
