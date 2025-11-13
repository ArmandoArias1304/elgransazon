# Impacto en Módulo de Turnos (Shifts) - Refactorización workDays

## 📋 Resumen

La eliminación de `system_work_days` afecta la validación de turnos en `ShiftServiceImpl`. Se actualizó el método `validateShiftDays()` para usar `BusinessHours` como fuente de verdad.

---

## 🔍 Análisis del Impacto

### Entidades Relacionadas

#### `Shift.java` - ✅ NO REQUIERE CAMBIOS
```java
@Entity
@Table(name = "shifts")
public class Shift {
    // ...
    
    // Esta propiedad es de SHIFT, no de SystemConfiguration
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "shift_work_days", joinColumns = @JoinColumn(name = "shift_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private Set<DayOfWeek> workDays = new HashSet<>();
    
    // ... métodos que usan shift.workDays
}
```

**⚠️ IMPORTANTE:** 
- `Shift` tiene su PROPIO campo `workDays` (tabla `shift_work_days`)
- Esto es diferente de `SystemConfiguration.workDays` (tabla `system_work_days`)
- Los turnos SIEMPRE tendrán workDays porque es parte de su definición
- Solo se eliminó `SystemConfiguration.workDays`, NO `Shift.workDays`

---

## 🔧 Cambios Realizados

### `ShiftServiceImpl.java`

#### Método Actualizado: `validateShiftDays()`

**Antes:**
```java
@Override
public void validateShiftDays(Set<DayOfWeek> shiftDays) {
    log.debug("Validating shift days");

    if (shiftDays == null || shiftDays.isEmpty()) {
        throw new IllegalArgumentException("Debe seleccionar al menos un día para el turno");
    }

    SystemConfiguration config = configurationService.getConfiguration();
    Set<DayOfWeek> workDays = config.getWorkDays(); // ❌ Ya no existe

    if (workDays == null || workDays.isEmpty()) {
        throw new IllegalStateException(
                "No hay días laborales configurados en el sistema. Configure los días laborales primero."
        );
    }

    for (DayOfWeek day : shiftDays) {
        if (!workDays.contains(day)) { // ❌ Consultaba Set directamente
            throw new IllegalArgumentException(
                    "El día " + day.getDisplayName() + " no es un día laboral del restaurante"
            );
        }
    }

    log.debug("Shift days validation passed");
}
```

**Después:**
```java
@Override
public void validateShiftDays(Set<DayOfWeek> shiftDays) {
    log.debug("Validating shift days");

    if (shiftDays == null || shiftDays.isEmpty()) {
        throw new IllegalArgumentException("Debe seleccionar al menos un día para el turno");
    }

    SystemConfiguration config = configurationService.getConfiguration();
    
    // ✅ Ahora obtiene work days desde BusinessHours
    List<DayOfWeek> workDays = config.getSortedWorkDays();

    if (workDays == null || workDays.isEmpty()) {
        throw new IllegalStateException(
                "No hay días laborales configurados en el sistema. Configure los horarios de negocio primero."
        );
    }

    // ✅ Valida usando isWorkDay() que consulta BusinessHours
    for (DayOfWeek day : shiftDays) {
        if (!config.isWorkDay(day)) {
            throw new IllegalArgumentException(
                    "El día " + day.getDisplayName() + " no es un día laboral del restaurante. " +
                    "El restaurante está cerrado este día."
            );
        }
    }

    log.debug("Shift days validation passed");
}
```

---

## 📊 Flujo de Validación Actualizado

### Escenario: Crear un turno "Mañana" para Lunes a Viernes

```
Usuario intenta crear turno con:
└── workDays = [MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY]

ShiftServiceImpl.createShift()
└── validateShift(shift)
    └── validateShiftDays(shift.getWorkDays())
        └── SystemConfiguration.getSortedWorkDays()
            └── businessHours.stream()
                .filter(h -> !h.isClosed)
                .map(BusinessHours::getDayOfWeek)
                
        ✅ Validación:
        Para cada día del turno (MONDAY-FRIDAY):
            └── config.isWorkDay(day)
                └── businessHours.stream()
                    .anyMatch(h -> h.getDayOfWeek == day && !h.isClosed)
                    
        Si todos pasan → ✅ Turno válido
        Si alguno falla → ❌ Error: "El día X no es día laboral"
```

---

## 🔄 Comparación de Comportamiento

### Caso 1: Crear turno en días abiertos

**Configuración BusinessHours:**
```
MONDAY    - 08:00-22:00 - is_closed=FALSE  ← Día laboral
TUESDAY   - 08:00-22:00 - is_closed=FALSE  ← Día laboral
WEDNESDAY - 08:00-22:00 - is_closed=FALSE  ← Día laboral
THURSDAY  - 08:00-22:00 - is_closed=FALSE  ← Día laboral
FRIDAY    - 08:00-22:00 - is_closed=FALSE  ← Día laboral
SATURDAY  - 08:00-22:00 - is_closed=FALSE  ← Día laboral
SUNDAY    - NULL-NULL   - is_closed=TRUE   ← Cerrado
```

**Crear turno Lunes-Viernes:**
```java
Shift shift = new Shift();
shift.setWorkDays(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY));

// Validación
validateShiftDays(shift.getWorkDays());
// ✅ Pasa - Todos los días están abiertos (is_closed=false)
```

---

### Caso 2: Crear turno incluyendo día cerrado

**Intento de crear turno Lunes-Domingo:**
```java
Shift shift = new Shift();
shift.setWorkDays(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY));

// Validación
validateShiftDays(shift.getWorkDays());
// ❌ Falla en SUNDAY
// Mensaje: "El día Domingo no es un día laboral del restaurante. El restaurante está cerrado este día."
```

---

### Caso 3: Sin días laborales configurados

**BusinessHours con todos los días cerrados:**
```
MONDAY-SUNDAY: is_closed=TRUE
```

**Intento de crear cualquier turno:**
```java
validateShiftDays(shift.getWorkDays());
// ❌ Falla inmediatamente
// Mensaje: "No hay días laborales configurados en el sistema. Configure los horarios de negocio primero."
```

---

## ✅ Funcionalidades que NO Cambian

### 1. Gestión de Turnos
```java
// ✅ Crear turno - Funciona igual
shiftService.createShift(shift);

// ✅ Actualizar turno - Funciona igual
shiftService.updateShift(id, shift);

// ✅ Eliminar turno - Funciona igual
shiftService.deleteShift(id);

// ✅ Activar/Desactivar - Funciona igual
shiftService.activateShift(id);
shiftService.deactivateShift(id);
```

### 2. Asignación de Empleados
```java
// ✅ Asignar empleados - Funciona igual
shiftService.assignEmployeesToShift(shiftId, employeeIds, actionById);

// ✅ Remover empleados - Funciona igual
shiftService.removeEmployeesFromShift(shiftId, employeeIds, actionById, reason);
```

### 3. Consultas
```java
// ✅ Todas las consultas funcionan igual
shiftService.getAllShifts();
shiftService.getAllActiveShifts();
shiftService.getShiftById(id);
shiftService.getEmployeesByShift(shiftId);
```

### 4. Validación de Horarios
```java
// ✅ validateShiftHours() - Funciona igual
// Ya usaba BusinessHoursService, no afectado
validateShiftHours(shiftDays, startTime, endTime);
```

---

## 🎯 Diferencias Clave

### Tabla de Comparación

| Aspecto | Antes | Después |
|---------|-------|---------|
| **Fuente de días laborales** | `SystemConfiguration.workDays` (tabla `system_work_days`) | `BusinessHours.is_closed = false` |
| **Método de validación** | `workDays.contains(day)` | `config.isWorkDay(day)` |
| **Mensaje de error** | "Configure los días laborales primero" | "Configure los horarios de negocio primero" |
| **Lógica de validación** | Compara con Set<DayOfWeek> | Consulta BusinessHours |
| **Tabla shift_work_days** | ✅ Existe (propiedad de Shift) | ✅ Sigue existiendo (sin cambios) |
| **Funcionalidad general** | ✅ Funciona | ✅ Funciona igual |

---

## 📝 Notas Importantes

### ⚠️ Distinción Crítica

```
ANTES:
├── system_work_days    → Días laborales del RESTAURANTE (eliminado)
└── shift_work_days     → Días de trabajo del TURNO (mantiene)

DESPUÉS:
├── business_hours      → Días laborales del RESTAURANTE (is_closed=false)
└── shift_work_days     → Días de trabajo del TURNO (sin cambios)
```

### 🔍 Relación entre Entidades

```
SystemConfiguration
└── List<BusinessHours> businessHours
    └── is_closed = FALSE → Días que el restaurante opera
    
Shift
└── Set<DayOfWeek> workDays (shift_work_days)
    └── Días específicos que aplica este turno
    └── DEBE ser subconjunto de días laborales del restaurante
```

**Ejemplo:**
```
Restaurante opera: Lunes-Sábado (BusinessHours con is_closed=false)
├── Turno "Mañana":   Lunes-Viernes   ✅ Válido (subconjunto)
├── Turno "Tarde":    Lunes-Sábado    ✅ Válido (mismo conjunto)
└── Turno "Domingo":  Domingo         ❌ Inválido (Domingo cerrado)
```

---

## 🧪 Casos de Prueba

### Test 1: Validar turno en días laborales
```java
@Test
public void testValidateShiftDays_ValidWorkDays_Success() {
    // Arrange
    Set<DayOfWeek> shiftDays = Set.of(MONDAY, TUESDAY, WEDNESDAY);
    // BusinessHours configurado con MONDAY-SATURDAY abiertos
    
    // Act & Assert - No debe lanzar excepción
    assertDoesNotThrow(() -> shiftService.validateShiftDays(shiftDays));
}
```

### Test 2: Validar turno con día cerrado
```java
@Test
public void testValidateShiftDays_ClosedDay_ThrowsException() {
    // Arrange
    Set<DayOfWeek> shiftDays = Set.of(MONDAY, SUNDAY);
    // BusinessHours: SUNDAY con is_closed=true
    
    // Act & Assert
    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> shiftService.validateShiftDays(shiftDays)
    );
    
    assertTrue(ex.getMessage().contains("no es un día laboral"));
    assertTrue(ex.getMessage().contains("Domingo"));
}
```

### Test 3: Sin días laborales configurados
```java
@Test
public void testValidateShiftDays_NoWorkDays_ThrowsException() {
    // Arrange
    Set<DayOfWeek> shiftDays = Set.of(MONDAY);
    // BusinessHours: Todos los días con is_closed=true
    
    // Act & Assert
    IllegalStateException ex = assertThrows(
        IllegalStateException.class,
        () -> shiftService.validateShiftDays(shiftDays)
    );
    
    assertTrue(ex.getMessage().contains("No hay días laborales configurados"));
}
```

---

## ✨ Beneficios de la Actualización

### 1. Consistencia
✅ Los turnos ahora validan contra la misma fuente de verdad que las reservaciones

### 2. Simplicidad
✅ Un solo lugar para definir días laborales (BusinessHours)

### 3. Mejor UX
✅ Mensajes de error más claros que mencionan "horarios de negocio"

### 4. Mantenibilidad
✅ Cambiar días laborales en BusinessHours afecta automáticamente a Shifts

---

## 🚀 Proceso de Verificación

### Paso 1: Configurar Días Laborales
1. Ir a `/admin/system-configuration`
2. En sección "Horarios de Negocio"
3. Marcar días como abiertos/cerrados
4. Guardar cambios

### Paso 2: Crear Turno
1. Ir a `/admin/shifts/new`
2. Seleccionar días para el turno
3. Intentar incluir un día cerrado
4. Verificar que muestra error apropiado

### Paso 3: Validar Comportamiento
```
✅ Turnos en días abiertos → Se crean exitosamente
❌ Turnos en días cerrados → Error claro
✅ Empleados se asignan → Funciona igual
✅ Historial de turnos → Funciona igual
```

---

## 📚 Archivos Relacionados

- `ShiftServiceImpl.java` - Servicio actualizado
- `Shift.java` - Entidad SIN cambios
- `ShiftController.java` - Controlador SIN cambios
- `REFACTORING_REMOVE_WORKDAYS.md` - Documento principal

---

## ✅ Conclusión

La refactorización de `system_work_days` tiene un **impacto mínimo en Shifts**:

- ✅ Solo 1 método actualizado: `validateShiftDays()`
- ✅ Funcionalidad completamente preservada
- ✅ Mejor consistencia con el resto del sistema
- ✅ Sin cambios en UI, controladores o entidades
- ✅ Validaciones más robustas usando BusinessHours

**El módulo de Shifts mantiene 100% de su funcionalidad** con una validación más consistente y robusta. 🎉
