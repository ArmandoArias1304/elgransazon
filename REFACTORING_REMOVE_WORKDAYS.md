# Refactorización: Eliminación de system_work_days

## 📋 Resumen de Cambios

Se implementó la **Opción A** (Diseño Simple) eliminando la tabla `system_work_days` y usando `business_hours` como única fuente de verdad para determinar los días laborales.

---

## 🎯 Objetivo

Eliminar la duplicación de información entre `system_work_days` y `business_hours`, manteniendo una única fuente de verdad.

### Antes ❌
```
SystemConfiguration
├── Set<DayOfWeek> workDays → tabla system_work_days
└── List<BusinessHours> businessHours → tabla business_hours

Problema: Dos fuentes de verdad que podían desincronizarse
```

### Después ✅
```
SystemConfiguration
└── List<BusinessHours> businessHours → tabla business_hours
    └── is_closed = FALSE → día de trabajo
    └── is_closed = TRUE  → día cerrado

Solución: Una sola fuente de verdad
```

---

## 🔧 Archivos Modificados

### 1. **Entidades**

#### `SystemConfiguration.java`
**Eliminado:**
- Campo `Set<DayOfWeek> workDays`
- Anotaciones `@ElementCollection` para workDays
- Tabla `system_work_days`

**Modificado:**
```java
// Método actualizado - ahora consulta businessHours
public boolean isWorkDay(DayOfWeek day) {
    return businessHours.stream()
            .anyMatch(hours -> hours.getDayOfWeek().equals(day) && !hours.getIsClosed());
}

// Método actualizado - retorna días con is_closed = false
public List<DayOfWeek> getSortedWorkDays() {
    return businessHours.stream()
            .filter(hours -> !hours.getIsClosed())
            .map(BusinessHours::getDayOfWeek)
            .sorted(Comparator.comparingInt(Enum::ordinal))
            .toList();
}
```

---

### 2. **Servicios**

#### `SystemConfigurationService.java`
**Eliminado:**
- Método `updateWorkDays(Set<DayOfWeek> workDays)`
- Import `java.util.Set`

#### `SystemConfigurationServiceImpl.java`
**Eliminado:**
- Implementación de `updateWorkDays()`
- Referencia a `workDays` en `updateConfiguration()`
- `.workDays(defaultWorkDays)` en el builder

**Modificado:**
```java
// Método createDefaultConfiguration() actualizado
// Ya no usa workDays en el builder
SystemConfiguration defaultConfig = SystemConfiguration.builder()
        .restaurantName("Mi Restaurante")
        // ... otros campos ...
        .paymentMethods(defaultPaymentMethods)
        .build(); // Sin .workDays()

// Después de guardar, crea BusinessHours para los 7 días
for (DayOfWeek day : DayOfWeek.values()) {
    boolean isWorkDay = defaultWorkDays.contains(day);
    BusinessHours hours = BusinessHours.builder()
            .dayOfWeek(day)
            .openTime(LocalTime.of(8, 0))
            .closeTime(LocalTime.of(22, 0))
            .isClosed(!isWorkDay) // Lunes-Sábado abierto, Domingo cerrado
            .systemConfiguration(saved)
            .build();
    saved.addBusinessHours(hours);
}
```

---

### 3. **Controladores**

#### `SystemConfigurationController.java`
**Eliminado:**
- Parámetro `@RequestParam(value = "workDays", required = false) List<String> workDays`
- Lógica para procesar workDays del formulario
- Lógica para setear workDays en la configuración

**Modificado:**
```java
// Método updateBusinessHours() actualizado
// Ahora procesa TODOS los días (DayOfWeek.values())
// en lugar de solo config.getWorkDays()
for (DayOfWeek day : DayOfWeek.values()) {
    // ... procesa cada día ...
}
```

---

### 4. **Templates**

#### `form.html` (System Configuration)
**Eliminado:**
- Sección completa de "Días Laborales" con checkboxes
- Ya no se muestran checkboxes para seleccionar días

**Modificado:**
```html
<!-- Sección de Horarios de Negocio -->
<!-- Ahora muestra TODOS los días (allDays) en lugar de solo sortedWorkDays -->
<tr th:each="day : ${allDays}">
  <!-- ... formulario de horarios ... -->
  <!-- El checkbox "Cerrado" determina si es día de trabajo -->
</tr>
```

**Comentario agregado:**
```html
<!-- Note: Work days are now managed through Business Hours section (open/closed status) -->
```

---

### 5. **Scripts SQL**

#### `migration_remove_system_work_days.sql`
Script completo de migración que:
1. Verifica datos actuales
2. Sincroniza BusinessHours con system_work_days
3. Crea registros faltantes en business_hours
4. Proporciona comando para eliminar system_work_days
5. Incluye plan de rollback

---

## 🔄 Flujo de Datos Actualizado

### Determinar si un día es laboral:

**Antes:**
```java
// Consultaba Set<DayOfWeek> workDays
boolean isWorkDay = configuration.getWorkDays().contains(DayOfWeek.MONDAY);
```

**Después:**
```java
// Consulta BusinessHours.isClosed
boolean isWorkDay = configuration.isWorkDay(DayOfWeek.MONDAY);
// Internamente: businessHours.stream().anyMatch(h -> h.getDayOfWeek() == MONDAY && !h.isClosed)
```

### Obtener días laborales:

**Antes:**
```java
// Retornaba Set<DayOfWeek> workDays
Set<DayOfWeek> workDays = configuration.getWorkDays();
```

**Después:**
```java
// Retorna días con is_closed = false
List<DayOfWeek> workDays = configuration.getSortedWorkDays();
// Internamente: businessHours.stream().filter(h -> !h.isClosed).map(...)
```

---

## ✅ Compatibilidad con Reservaciones

### ReservationService no requiere cambios

El método de validación sigue funcionando sin modificaciones:

```java
// En ReservationService.validateReservationTime()
if (!config.isWorkDay(dayOfWeek)) {
    throw new IllegalArgumentException(
        "No se pueden hacer reservaciones en días que el restaurante está cerrado"
    );
}
```

**¿Por qué funciona sin cambios?**
- Usa el método `isWorkDay()` de SystemConfiguration
- Este método fue actualizado internamente para consultar BusinessHours
- La lógica de negocio permanece idéntica
- Las validaciones siguen siendo las mismas

---

## 📊 Estructura de Datos Final

### Tabla: `business_hours`
```sql
CREATE TABLE business_hours (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    system_configuration_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    open_time TIME NOT NULL,
    close_time TIME NOT NULL,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (system_configuration_id, day_of_week),
    FOREIGN KEY (system_configuration_id) REFERENCES system_configuration(id)
);
```

**Reglas:**
- Debe existir 1 registro por cada día de la semana (7 registros por configuración)
- `is_closed = FALSE` → Día de trabajo
- `is_closed = TRUE` → Día cerrado (restaurante no opera)

### Ejemplo de datos:
```
id | system_config_id | day_of_week | open_time | close_time | is_closed
---|------------------|-------------|-----------|------------|----------
1  | 1                | MONDAY      | 08:00     | 22:00      | FALSE  ← Día de trabajo
2  | 1                | TUESDAY     | 08:00     | 22:00      | FALSE  ← Día de trabajo
3  | 1                | WEDNESDAY   | 08:00     | 22:00      | FALSE  ← Día de trabajo
4  | 1                | THURSDAY    | 08:00     | 22:00      | FALSE  ← Día de trabajo
5  | 1                | FRIDAY      | 08:00     | 22:00      | FALSE  ← Día de trabajo
6  | 1                | SATURDAY    | 08:00     | 22:00      | FALSE  ← Día de trabajo
7  | 1                | SUNDAY      | NULL      | NULL       | TRUE   ← Cerrado (no es día de trabajo)
```

---

## 🚀 Proceso de Migración

### Paso 1: Actualizar código (✅ Completado)
Todos los archivos Java y templates ya fueron actualizados.

### Paso 2: Ejecutar migración de base de datos

```sql
-- 1. Ejecutar script de migración
source database/migration_remove_system_work_days.sql;

-- 2. Verificar que todos los días tienen BusinessHours
SELECT system_configuration_id, COUNT(*) as total_days
FROM business_hours
GROUP BY system_configuration_id;
-- Debe retornar 7 días por cada configuración

-- 3. Comparar work days
SELECT day_of_week FROM business_hours WHERE is_closed = FALSE;
-- Debe coincidir con los días que estaban en system_work_days

-- 4. Eliminar tabla (después de verificar)
DROP TABLE IF EXISTS system_work_days;
```

### Paso 3: Reiniciar aplicación
```bash
# Detener la aplicación
# Iniciar la aplicación
# Verificar logs - no debe haber errores relacionados con workDays
```

### Paso 4: Verificar funcionalidad

**UI - Configuración del Sistema:**
1. Ir a `/admin/system-configuration`
2. Verificar que se muestren los 7 días en "Horarios de Negocio"
3. Cambiar el estado "Cerrado" de un día
4. Guardar y verificar que se actualiza correctamente

**UI - Reservaciones:**
1. Intentar crear una reservación en un día cerrado
2. Debe mostrar error: "No se pueden hacer reservaciones en días que el restaurante está cerrado"
3. Crear reservación en día abierto
4. Debe funcionar normalmente

---

## 📦 Beneficios de la Refactorización

### ✅ Ventajas

1. **Una sola fuente de verdad**
   - No hay duplicación de información
   - No hay inconsistencias entre tablas

2. **Más simple de mantener**
   - Menos código
   - Menos tablas
   - Lógica más clara

3. **Mejor experiencia de usuario**
   - Todo se maneja en una sola pantalla (Horarios de Negocio)
   - Más intuitivo: "cerrado" significa "no es día de trabajo"

4. **Menos bugs potenciales**
   - Eliminamos la posibilidad de que workDays y businessHours estén desincronizados

### 🔄 Cambios para el Usuario

**Antes:**
- Checkboxes para seleccionar "Días Laborales"
- Formulario separado para "Horarios de Negocio"
- Dos lugares para gestionar días

**Después:**
- Solo formulario de "Horarios de Negocio"
- Checkbox "Cerrado" determina si es día de trabajo
- Un solo lugar para gestionar todo

---

## ⚠️ Notas Importantes

### Para Desarrolladores

1. **Nunca usar `setWorkDays()`** - El método ya no existe
2. **Usar `isWorkDay(day)`** - Para verificar si un día es laboral
3. **Usar `getSortedWorkDays()`** - Para obtener lista de días laborales
4. **Asegurar 7 días en BusinessHours** - Siempre debe haber un registro por cada día

### Para Base de Datos

1. **Constraint recomendado:**
   ```sql
   -- Asegurar que cada configuración tenga exactamente 7 días
   -- (Implementar en código o mediante trigger)
   ```

2. **Migración de datos existentes:**
   - El script sincroniza automáticamente
   - Días en system_work_days → business_hours.is_closed = FALSE
   - Días NO en system_work_days → business_hours.is_closed = TRUE

---

## 🧪 Testing

### Test Cases para Verificar

```java
// 1. Verificar que isWorkDay() funciona correctamente
assertTrue(config.isWorkDay(DayOfWeek.MONDAY)); // Si MONDAY está abierto
assertFalse(config.isWorkDay(DayOfWeek.SUNDAY)); // Si SUNDAY está cerrado

// 2. Verificar que getSortedWorkDays() retorna solo días abiertos
List<DayOfWeek> workDays = config.getSortedWorkDays();
assertEquals(6, workDays.size()); // Si Domingo está cerrado
assertFalse(workDays.contains(DayOfWeek.SUNDAY));

// 3. Verificar validación en reservaciones
// Intentar reservar en día cerrado debe lanzar excepción
assertThrows(IllegalArgumentException.class, () -> {
    reservationService.create(reservationOnSunday, "admin");
});
```

---

## 📚 Documentos Relacionados

- `SYSTEM_CONFIGURATION_ARCHITECTURE.md` - Arquitectura completa
- `SYSTEM_CONFIGURATION_DIAGRAM.md` - Diagramas visuales
- `database/migration_remove_system_work_days.sql` - Script de migración
- `database/init_system_configuration_complete.sql` - Inicialización completa

---

## ✨ Conclusión

La refactorización elimina exitosamente la duplicación de información manteniendo toda la funcionalidad existente. El sistema ahora es más simple, más robusto y más fácil de mantener.

**Próximos pasos:**
1. ✅ Código actualizado
2. ⏳ Ejecutar migración SQL
3. ⏳ Verificar funcionalidad
4. ⏳ Deploy a producción
