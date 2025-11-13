# Lógica de Estados de Mesas en el Sistema

## 📋 Resumen

Este documento explica la lógica completa de cómo funcionan los estados de las mesas en el sistema, especialmente la diferencia entre el campo `status` y el campo `is_occupied` en la tabla `restaurant_table`.

## 🎯 Conceptos Clave

### Estados de Mesa (TableStatus)

- **AVAILABLE**: Mesa disponible, sin reservaciones ni pedidos
- **RESERVED**: Mesa reservada para un cliente
- **OCCUPIED**: Mesa ocupada por un cliente (sin reservación previa)
- **OUT_OF_SERVICE**: Mesa fuera de servicio

### Campo `is_occupied`

- Indica si **físicamente** hay un cliente en la mesa en este momento
- Solo se usa cuando el `status = RESERVED`
- Permite diferenciar entre:
  - Una mesa reservada pero vacía (`RESERVED` + `is_occupied=false`)
  - Una mesa reservada con cliente presente (`RESERVED` + `is_occupied=true`)

## 📊 Matriz de Estados

| Caso | Status           | is_occupied | Descripción                                           |
| ---- | ---------------- | ----------- | ----------------------------------------------------- |
| 1    | `AVAILABLE`      | `false`     | Mesa libre, nadie la ha reservado ni ocupado          |
| 2    | `RESERVED`       | `false`     | Mesa reservada, cliente aún no llega                  |
| 3    | `RESERVED`       | `true`      | Mesa reservada, cliente ya llegó y está en la mesa    |
| 4    | `OCCUPIED`       | `false`     | Mesa ocupada sin reservación previa (cliente walk-in) |
| 5    | `OUT_OF_SERVICE` | `false`     | Mesa fuera de servicio                                |

**⚠️ IMPORTANTE:** `is_occupied` **solo** se usa cuando `status = RESERVED`. Para mesas `OCCUPIED` (sin reservación), `is_occupied` siempre es `false`.

## 🔄 Flujo de Reservaciones

### 1. **Crear Reservación**

```
Estado inicial: AVAILABLE + is_occupied=false
Acción: Cliente reserva mesa para las 5:00 PM
Estado final: RESERVED + is_occupied=false
```

### 2. **Cliente Llega (Check-in)**

```
Estado inicial: RESERVED + is_occupied=false
Acción: Cliente llega a las 4:50 PM (antes de las 5:00 PM)
Validación: ✅ Tiempo OK (llegó antes de su reservación)
Estado final: RESERVED + is_occupied=true
```

### 3. **Validación de Tiempo de Consumo**

**Configuración del Sistema:**

- Tiempo promedio de consumo: `2 horas` (configurable en System Configuration)

**Regla de Negocio:**
Si una mesa está reservada para las **5:00 PM**, entonces:

- ⏰ **Última hora permitida para ocupar**: `2:59 PM`
- ❌ Si alguien intenta ocuparla a las **3:00 PM o después**, el sistema rechaza la operación

**Ejemplo 1 - PERMITIDO ✅:**

```
Mesa reservada para: 5:00 PM
Cliente walk-in llega a: 2:30 PM
Tiempo de consumo estimado: 2 horas → Terminaría a las 4:30 PM
Validación: 4:30 PM < 5:00 PM ✅
Acción: Permitir ocupar la mesa
```

**Ejemplo 2 - RECHAZADO ❌:**

```
Mesa reservada para: 5:00 PM
Cliente walk-in llega a: 3:30 PM
Tiempo de consumo estimado: 2 horas → Terminaría a las 5:30 PM
Validación: 5:30 PM > 5:00 PM ❌
Acción: Rechazar ocupar la mesa
Error: "No hay tiempo suficiente antes de la próxima reservación"
```

### 4. **Cliente Sale (Check-out)**

```
Estado inicial: RESERVED + is_occupied=true
Acción: Cliente sale a las 3:00 PM o 4:00 PM
Estado final: RESERVED + is_occupied=false
```

**¿Por qué sigue en RESERVED?**

- La mesa tiene una reservación para las 5:00 PM
- Aunque el cliente actual ya salió, la mesa sigue comprometida
- No se puede asignar a nadie más (a menos que valide el tiempo de consumo)

### 5. **Reservación Finalizada**

```
Estado inicial: RESERVED + is_occupied=true
Acción: Cliente con reservación sale después de su turno
Estado final: AVAILABLE + is_occupied=false
```

## 🍽️ Flujo de Pedidos (Orders)

### Escenario 1: Mesa AVAILABLE → Crear Pedido DINE_IN

```java
// Estado inicial
mesa.status = AVAILABLE
mesa.is_occupied = false

// Acción: Crear pedido con tipo DINE_IN (cliente walk-in)
orderService.create(order, orderDetails);

// Estado final
mesa.status = OCCUPIED
mesa.is_occupied = false  // ← NO CAMBIA (solo cambia con RESERVED)
```

### Escenario 2: Mesa RESERVED → Crear Pedido DINE_IN

```java
// Estado inicial
mesa.status = RESERVED
mesa.is_occupied = false

// Acción: Crear pedido con tipo DINE_IN
orderService.create(order, orderDetails);

// Validación interna
restaurantTableService.markAsOccupied(tableId, username);
// ↳ Valida tiempo de consumo vs próxima reservación

// Estado final (si pasa validación)
mesa.status = RESERVED      // ← NO CAMBIA
mesa.is_occupied = true     // ← CAMBIA A TRUE
```

Nota: La vista de creación/edición de pedidos (`/admin/orders/form`) ahora mostrará las mesas que están en estado `RESERVED` cuando la validación de tiempo de consumo permita que sean ocupadas en este momento. Se reutiliza la misma validación que usa el módulo de reservaciones (no se duplica lógica).

### Escenario 3: Cambiar Mesa en Pedido

```java
// Pedido actual: Mesa 5 (OCCUPIED, is_occupied=false)
// Cliente se cambia a: Mesa 6 (AVAILABLE)

// 1. Liberar Mesa 5
if (mesa5.status == RESERVED) {
    mesa5.is_occupied = false;  // Solo cambia el flag
} else if (mesa5.status == OCCUPIED) {
    mesa5.status = AVAILABLE;
    // is_occupied ya está en false, no cambia
}

// 2. Ocupar Mesa 6
if (mesa6.status == RESERVED) {
    // Validar tiempo de consumo
    restaurantTableService.markAsOccupied(6, username);
    mesa6.is_occupied = true;  // Status sigue en RESERVED
} else if (mesa6.status == AVAILABLE) {
    mesa6.status = OCCUPIED;
    // is_occupied se mantiene en false (solo cambia con RESERVED)
}
```

## 🔍 Validaciones Importantes

### 1. **Validación de Tiempo de Consumo** (en `RestaurantTableService.markAsOccupied()`)

```java
// Obtener tiempo de consumo de la configuración
Integer avgConsumptionMinutes = systemConfiguration.getAverageConsumptionTimeMinutes();

// Buscar próxima reservación para esta mesa
Optional<Reservation> nextReservation = findNextReservationForTable(tableId);

if (nextReservation.isPresent()) {
    LocalTime now = LocalTime.now();
    LocalTime nextReservationTime = nextReservation.getReservationTime();
    LocalTime estimatedEndTime = now.plusMinutes(avgConsumptionMinutes);

    // ❌ Si el tiempo estimado de finalización > hora de reservación
    if (estimatedEndTime.isAfter(nextReservationTime)) {
        throw new IllegalStateException(
            "No hay tiempo suficiente antes de la próxima reservación"
        );
    }
}

// ✅ Si pasa la validación
table.setIsOccupied(true);
```

### 2. **Validación de Disponibilidad de Mesa**

```java
// En OrderServiceImpl
private boolean isTableAvailableForOrder(Long tableId) {
    RestaurantTable table = restaurantTableRepository.findById(tableId)
        .orElseThrow();

    // Mesa está disponible si:
    // 1. Estado es AVAILABLE, o
    // 2. Estado es RESERVED pero is_occupied = false (y pasa validación de tiempo)
    return table.getStatus() == TableStatus.AVAILABLE ||
           (table.getStatus() == TableStatus.RESERVED && !table.getIsOccupied());
}
```

## 📝 Métodos Clave en el Código

### En `ReservationService`

- `validateNoOverlappingReservations()`: Valida que no haya conflictos de horarios
- `validateReservationTime()`: Valida que la reservación esté dentro del horario de negocio

### En `RestaurantTableService`

- `markAsOccupied()`: Marca una mesa reservada como ocupada (con validación de tiempo)

### En `OrderServiceImpl`

- `handleTableChange()`: Maneja los cambios de mesa en pedidos
- `isTableAvailableForOrder()`: Valida si una mesa está disponible para un pedido

## 🎓 Ejemplo Completo de Caso de Uso

**Configuración:**

- Tiempo de consumo: 2 horas
- Horario: 12:00 PM - 10:00 PM

**Timeline:**

```
12:00 PM - Mesa 5: AVAILABLE
↓
2:00 PM - Cliente A reserva Mesa 5 para las 5:00 PM
          → Mesa 5: RESERVED + is_occupied=false
↓
2:30 PM - Cliente B (walk-in) quiere Mesa 5
          Sistema valida: 2:30 PM + 2 horas = 4:30 PM < 5:00 PM ✅
          → Cliente B puede sentarse
          → Mesa 5: RESERVED + is_occupied=true
↓
4:00 PM - Cliente B sale
          → Mesa 5: RESERVED + is_occupied=false
          (Mesa sigue reservada para Cliente A)
↓
4:30 PM - Cliente C (walk-in) quiere Mesa 5
          Sistema valida: 4:30 PM + 2 horas = 6:30 PM > 5:00 PM ❌
          → Cliente C NO puede sentarse
          Error: "No hay tiempo suficiente antes de la próxima reservación"
↓
4:50 PM - Cliente A llega (check-in)
          → Mesa 5: RESERVED + is_occupied=true
↓
7:00 PM - Cliente A sale (check-out)
          → Mesa 5: AVAILABLE + is_occupied=false
```

## ✅ Resumen de Reglas

1. **`status = RESERVED`** es controlado por el módulo de Reservaciones
2. **`is_occupied` SOLO se usa cuando `status = RESERVED`** para indicar si el cliente con reservación ya llegó
3. **Una mesa RESERVED con `is_occupied=false`** puede ser ocupada si:
   - El tiempo actual + tiempo de consumo < hora de próxima reservación
4. **Cuando un pedido ocupa una mesa RESERVED**:
   - El `status` NO cambia (sigue en RESERVED)
   - Solo cambia `is_occupied = true`
5. **Cuando un pedido ocupa una mesa AVAILABLE**:
   - El `status` cambia a OCCUPIED
   - `is_occupied` se mantiene en `false` (NO se usa para mesas sin reservación)

## 🔧 Configuración del Sistema

Para cambiar el tiempo de consumo promedio:

1. Ir a: `/admin/system-configuration`
2. Buscar: "Tiempo Promedio de Consumo"
3. Cambiar el valor (en minutos)
4. Ejemplo: `120` minutos = 2 horas

---

**Última actualización:** 23 de octubre de 2025  
**Autor:** Sistema de Gestión de Restaurante
