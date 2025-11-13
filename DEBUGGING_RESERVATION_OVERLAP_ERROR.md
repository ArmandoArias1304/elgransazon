# 🔍 Debugging: Error de Solapamiento de Reservaciones

## 📋 Problema Reportado

Al intentar crear una reservación a las **18:00** cuando ya existe una a las **16:00**, el sistema arroja error:

```
java.lang.IllegalArgumentException: Ya existe una reservación para esta mesa en el horario solicitado. 
Debe haber al menos 1h 59min entre reservaciones.
```

**Configuración:**
- `average_consumption_time_minutes = 119` (1h 59min)
- Reservación existente: **16:00**
- Nueva reservación: **18:00**
- Diferencia: **2 horas** (120 minutos)

**Expectativa:** Debería permitir la reservación porque 120 min > 119 min

---

## 🐛 Análisis de la Query

### Query SQL Ejecutada

```sql
SELECT COUNT(r.id_reservation)
FROM reservations r
WHERE r.id_table = ?
  AND r.reservation_date = ?
  AND r.status IN ('RESERVED', 'OCCUPIED')
  AND (? IS NULL OR r.id_reservation != ?)
  AND r.reservation_time < ?
  AND ADDTIME(r.reservation_time, SEC_TO_TIME(?)) > ?
```

### Parámetros

```java
:tableId = <mesa seleccionada>
:date = <fecha seleccionada>
:startTime = 18:00:00  (nueva reservación)
:endTime = 19:59:00    (18:00 + 119 minutos)
:avgConsumptionSeconds = 7140  (119 * 60)
:reservationId = NULL  (es nueva)
```

### Lógica de Solapamiento

La query busca reservaciones que se **solapen** con la nueva:

```sql
-- Condición 1: Reservación existente empieza ANTES de que termine la nueva
r.reservation_time < :endTime

-- Condición 2: Reservación existente TERMINA DESPUÉS de que empiece la nueva
ADDTIME(r.reservation_time, SEC_TO_TIME(:avgConsumptionSeconds)) > :startTime
```

---

## 🧮 Cálculo del Escenario

### Reservación Existente: 16:00

```sql
-- Condición 1: ¿16:00 < 19:59?
r.reservation_time < :endTime
16:00 < 19:59  --> TRUE ✅

-- Condición 2: ¿(16:00 + 119 min) > 18:00?
ADDTIME(16:00, SEC_TO_TIME(7140)) > 18:00
ADDTIME(16:00, 01:59:00) > 18:00
17:59 > 18:00  --> FALSE ❌
```

**Resultado:** La reservación existente **NO debería** detectarse como conflicto porque 17:59 NO es mayor que 18:00.

---

## 🎯 Posibles Causas

### Hipótesis 1: Valor de `average_consumption_time_minutes` no actualizado

**Problema:** El cambio a `119` no se reflejó en la BD

**Verificar:**
```sql
SELECT id, average_consumption_time_minutes, updated_at
FROM system_configuration
ORDER BY id DESC
LIMIT 1;
```

**Esperado:** `average_consumption_time_minutes = 119`

**Si es diferente:** El cambio no se guardó (problema de `logoUrl` que ya resolvimos)

---

### Hipótesis 2: Hora de fin calculada incorrectamente

**Problema:** `endTime` se calcula mal en el código Java

**Verificar en ReservationService.java línea 422:**
```java
// Calculate end time
LocalTime endTime = startTime.plusMinutes(avgConsumption);
```

**Si `startTime = 18:00` y `avgConsumption = 119`:**
```
endTime = 18:00 + 119 min = 19:59 ✅ Correcto
```

---

### Hipótesis 3: Reservación existente tiene hora diferente

**Problema:** La reservación a las "16:00" en realidad está a otra hora

**Verificar:**
```sql
SELECT id_reservation, reservation_time, reservation_date, status
FROM reservations
WHERE id_table = <id_mesa>
  AND reservation_date = '<fecha>'
  AND status IN ('RESERVED', 'OCCUPIED')
ORDER BY reservation_time;
```

**Esperado:** `reservation_time = 16:00:00`

---

### Hipótesis 4: Bug en la query con comparación de tiempos

**Problema:** MySQL/MariaDB tiene comportamiento inesperado con `ADDTIME` o `SEC_TO_TIME`

**Verificar ejecutando query directamente:**
```sql
-- Test de la lógica
SELECT 
    '16:00:00' AS start_time,
    ADDTIME('16:00:00', SEC_TO_TIME(7140)) AS end_time,
    ADDTIME('16:00:00', SEC_TO_TIME(7140)) > '18:00:00' AS overlaps;
```

**Esperado:**
```
start_time: 16:00:00
end_time: 17:59:00
overlaps: 0 (FALSE)
```

**Si `overlaps = 1`:** Bug en la BD o timezone

---

## 🔧 Soluciones Potenciales

### Solución 1: Verificar y forzar actualización de configuración

```sql
-- Ver valor actual
SELECT average_consumption_time_minutes FROM system_configuration;

-- Si no es 119, actualizar manualmente
UPDATE system_configuration
SET average_consumption_time_minutes = 119,
    updated_at = NOW()
WHERE id = 1;
```

---

### Solución 2: Agregar logging detallado

Modificar `ReservationService.java` para ver valores exactos:

```java
private void validateNoOverlappingReservations(Long tableId, LocalDate date, 
                                               LocalTime startTime, Long excludeId) {
    SystemConfiguration config = systemConfigurationService.getConfiguration();
    Integer avgConsumption = config.getAverageConsumptionTimeMinutes();

    log.debug("=== Validating overlapping reservations ===");
    log.debug("Table ID: {}", tableId);
    log.debug("Date: {}", date);
    log.debug("Start time: {}", startTime);
    log.debug("Avg consumption: {} minutes", avgConsumption);

    // Calculate end time
    LocalTime endTime = startTime.plusMinutes(avgConsumption);
    log.debug("Calculated end time: {}", endTime);

    // Convert average consumption to seconds for the native query
    Integer avgConsumptionSeconds = avgConsumption * 60;
    log.debug("Avg consumption seconds: {}", avgConsumptionSeconds);

    Long overlapCount = reservationRepository.countOverlappingReservations(
            tableId, date, startTime, endTime, avgConsumptionSeconds, excludeId);

    log.debug("Overlap count: {}", overlapCount);

    if (overlapCount > 0) {
        throw new IllegalArgumentException(
                "Ya existe una reservación para esta mesa en el horario solicitado. " +
                "Debe haber al menos " + config.getAverageConsumptionTimeDisplay() + 
                " entre reservaciones.");
    }
    
    log.debug("=== Validation passed ===");
}
```

---

### Solución 3: Cambiar lógica de comparación

Si el problema persiste, cambiar a usar `>=` en lugar de `>`:

```sql
-- Cambiar de:
AND ADDTIME(r.reservation_time, SEC_TO_TIME(:avgConsumptionSeconds)) > :startTime

-- A:
AND ADDTIME(r.reservation_time, SEC_TO_TIME(:avgConsumptionSeconds)) >= :startTime
```

Pero esto haría la validación **más estricta**.

---

## 📋 Pasos para Depurar

### Paso 1: Verificar valor en BD

```sql
SELECT 
    id, 
    average_consumption_time_minutes,
    updated_at
FROM system_configuration
ORDER BY id DESC
LIMIT 1;
```

### Paso 2: Ver reservaciones existentes

```sql
SELECT 
    id_reservation,
    customer_name,
    reservation_date,
    reservation_time,
    ADDTIME(reservation_time, SEC_TO_TIME(119 * 60)) AS end_time_calculated,
    status,
    id_table
FROM reservations
WHERE reservation_date = '<tu_fecha>'
  AND id_table = <tu_mesa>
  AND status IN ('RESERVED', 'OCCUPIED')
ORDER BY reservation_time;
```

### Paso 3: Probar query directamente

```sql
-- Simular la query de validación
SELECT COUNT(*) AS overlap_count
FROM reservations r
WHERE r.id_table = <tu_mesa>
  AND r.reservation_date = '<tu_fecha>'
  AND r.status IN ('RESERVED', 'OCCUPIED')
  AND r.reservation_time < '19:59:00'  -- endTime (18:00 + 119 min)
  AND ADDTIME(r.reservation_time, SEC_TO_TIME(7140)) > '18:00:00';  -- startTime

-- Si retorna > 0, significa que SÍ detecta conflicto
-- Ver cuál reservación específica causa el conflicto:
SELECT 
    r.id_reservation,
    r.reservation_time,
    ADDTIME(r.reservation_time, SEC_TO_TIME(7140)) AS calculated_end_time,
    (ADDTIME(r.reservation_time, SEC_TO_TIME(7140)) > '18:00:00') AS causes_overlap
FROM reservations r
WHERE r.id_table = <tu_mesa>
  AND r.reservation_date = '<tu_fecha>'
  AND r.status IN ('RESERVED', 'OCCUPIED')
  AND r.reservation_time < '19:59:00';
```

### Paso 4: Agregar logging y reintentar

1. Agregar el logging propuesto en Solución 2
2. Reiniciar la aplicación
3. Intentar crear la reservación de nuevo
4. Revisar los logs para ver los valores exactos

---

## ✅ Checklist de Verificación

- [ ] Valor de `average_consumption_time_minutes` en BD es 119
- [ ] No hay otra reservación entre 16:00 y 18:00
- [ ] La reservación a las 16:00 tiene status 'RESERVED' o 'OCCUPIED'
- [ ] La query manual retorna `overlap_count = 0`
- [ ] El logging muestra `avgConsumption = 119`
- [ ] La aplicación se reinició después de cambiar la configuración

---

## 🎯 Resultado Esperado

Con `average_consumption_time_minutes = 119`:

| Hora Reservación 1 | Hora Reservación 2 | Separación | ¿Permitido? |
|--------------------|-------------------|------------|-------------|
| 16:00 | 17:59 | 119 min | ✅ Exactamente el límite |
| 16:00 | 18:00 | 120 min | ✅ Más que el límite |
| 16:00 | 17:58 | 118 min | ❌ Menos que el límite |

---

## 📚 Archivos Relacionados

- `ReservationService.java` - Línea 417-435 (validateNoOverlappingReservations)
- `ReservationRepository.java` - Línea 100-117 (countOverlappingReservations query)
- `SystemConfiguration.java` - Campo `averageConsumptionTimeMinutes`
- `SystemConfigurationServiceImpl.java` - getConfiguration()

---

## 🚀 Próxima Acción

**Por favor ejecuta estas queries SQL y comparte los resultados:**

```sql
-- 1. Ver configuración actual
SELECT average_consumption_time_minutes FROM system_configuration;

-- 2. Ver reservaciones del día
SELECT 
    id_reservation,
    reservation_time,
    ADDTIME(reservation_time, SEC_TO_TIME(119 * 60)) AS end_time,
    status
FROM reservations
WHERE reservation_date = CURDATE()  -- o tu fecha específica
  AND status IN ('RESERVED', 'OCCUPIED')
ORDER BY reservation_time;

-- 3. Probar la query de overlap
SELECT COUNT(*) AS overlap_count
FROM reservations r
WHERE r.reservation_time < '19:59:00'
  AND ADDTIME(r.reservation_time, SEC_TO_TIME(7140)) > '18:00:00'
  AND r.status IN ('RESERVED', 'OCCUPIED')
  AND r.reservation_date = CURDATE();  -- o tu fecha específica
```

Con estos resultados podré identificar la causa exacta. 🔍
