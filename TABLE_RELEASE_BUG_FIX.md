# Test de Liberación de Mesa al Pagar

## Problema Identificado

La mesa se estaba liberando cuando el estado de la orden era DELIVERED en lugar de PAID.

## Solución Aplicada

### 1. Actualización en `OrderServiceImpl.java`

- Cambiada la condición para liberar mesa solo cuando `newStatus == OrderStatus.PAID`
- Eliminada la verificación de `DELIVERED`

### 2. Actualización en `PaymentController.java`

- Agregado `OrderRepository` como dependencia
- Guardado de la orden con `tip` y `paymentMethod` ANTES de cambiar el estado
- Esto asegura que los cambios se persistan correctamente

## Código Actualizado

### PaymentController.processPayment()

```java
// Set tip and payment method
order.setTip(tip);
order.setPaymentMethod(paymentMethod);
order.setUpdatedBy(username);

// Save order first with tip and payment method
orderRepository.save(order);
log.info("Order {} updated with tip: {} and payment method: {}",
         order.getOrderNumber(), tip, paymentMethod);

// Change status to PAID
// NOTE: The OrderService.changeStatus() method will automatically free the table
// when status changes to PAID, so we don't need to do it manually here
orderService.changeStatus(orderId, OrderStatus.PAID, username);
```

### OrderServiceImpl.changeStatus()

```java
// If order is marked as PAID, free the table
// NOTE: Table is NOT freed when DELIVERED - only when PAID
if (newStatus == OrderStatus.PAID && orderType == OrderType.DINE_IN) {
    RestaurantTable table = order.getTable();
    if (table != null) {
        if (table.getStatus() == TableStatus.RESERVED) {
            table.setIsOccupied(false);
            log.info("Reserved table #{} is_occupied set to false after order payment",
                     table.getTableNumber());
        } else if (table.getStatus() == TableStatus.OCCUPIED) {
            table.setStatus(TableStatus.AVAILABLE);
            table.setIsOccupied(false);
            log.info("Table #{} freed and marked as AVAILABLE after order payment",
                     table.getTableNumber());
        }
        table.setUpdatedBy(updatedBy);
        restaurantTableRepository.save(table);
    }
}
```

## Flujo Correcto Ahora

### Escenario 1: Mesa OCCUPIED

1. Orden PENDING → Mesa OCCUPIED ✅
2. Orden IN_PREPARATION → Mesa OCCUPIED ✅
3. Orden READY → Mesa OCCUPIED ✅
4. Orden DELIVERED → Mesa OCCUPIED ✅ (**CORRECTO - NO SE LIBERA**)
5. Procesar Pago → Orden PAID → Mesa AVAILABLE ✅

### Escenario 2: Mesa RESERVED

1. Orden PENDING → Mesa RESERVED, isOccupied=true ✅
2. Orden IN_PREPARATION → Mesa RESERVED, isOccupied=true ✅
3. Orden READY → Mesa RESERVED, isOccupied=true ✅
4. Orden DELIVERED → Mesa RESERVED, isOccupied=true ✅ (**CORRECTO - NO SE LIBERA**)
5. Procesar Pago → Orden PAID → Mesa RESERVED, isOccupied=false ✅

## Logs Esperados

### Al cambiar a DELIVERED:

```
Order status changed: READY -> DELIVERED
```

**(NO debe aparecer ningún log de liberación de mesa)**

### Al procesar pago (PAID):

```
Order <numero> updated with tip: <monto> and payment method: <metodo>
Payment processed successfully for order: <numero>

-- Si mesa es OCCUPIED:
Table #<numero> freed and marked as AVAILABLE after order payment

-- Si mesa es RESERVED:
Reserved table #<numero> is_occupied set to false after order payment

Order status changed: DELIVERED -> PAID
```

## Prueba Manual

### Paso 1: Crear orden DINE_IN

```
1. Ir a /admin/orders/select-table
2. Seleccionar una mesa AVAILABLE
3. Crear orden con items
4. Verificar que mesa está OCCUPIED
```

### Paso 2: Cambiar estados hasta DELIVERED

```
1. Cambiar a IN_PREPARATION
2. Verificar que mesa sigue OCCUPIED ✅
3. Cambiar a READY
4. Verificar que mesa sigue OCCUPIED ✅
5. Cambiar a DELIVERED
6. Verificar que mesa sigue OCCUPIED ✅ (IMPORTANTE)
```

### Paso 3: Procesar pago

```
1. Click en botón $ (verde)
2. Seleccionar método de pago
3. Ingresar propina (opcional)
4. Confirmar pago
5. Verificar que orden está en PAID ✅
6. Verificar que mesa está AVAILABLE ✅
```

## Verificación en Base de Datos

### Antes del pago (DELIVERED):

```sql
SELECT o.order_number, o.status, t.table_number, t.status as table_status, t.is_occupied
FROM orders o
LEFT JOIN tables t ON o.id_table = t.id
WHERE o.order_number = 'ORD-XXXXXXXX-XXX';

-- Resultado esperado:
-- status = DELIVERED
-- table_status = OCCUPIED (o RESERVED)
-- is_occupied = true (si es RESERVED) o NULL (si es OCCUPIED)
```

### Después del pago (PAID):

```sql
SELECT o.order_number, o.status, o.tip, t.table_number, t.status as table_status, t.is_occupied
FROM orders o
LEFT JOIN tables t ON o.id_table = t.id
WHERE o.order_number = 'ORD-XXXXXXXX-XXX';

-- Resultado esperado:
-- status = PAID
-- tip = <monto ingresado>
-- table_status = AVAILABLE (si era OCCUPIED) o RESERVED (si era RESERVED)
-- is_occupied = false (si era RESERVED) o NULL (si era OCCUPIED->AVAILABLE)
```

## Archivos Modificados

1. ✅ `PaymentController.java` - Agregado guardado de orden antes de cambiar estado
2. ✅ `OrderServiceImpl.java` - Liberación de mesa solo en PAID (ya estaba hecho)

## Confirmación

El problema estaba en que no se guardaban los cambios de `tip` y `paymentMethod` antes de cambiar el estado. Ahora:

1. ✅ Se guarda la orden con tip y paymentMethod
2. ✅ Se cambia el estado a PAID
3. ✅ La mesa se libera automáticamente cuando el estado es PAID
4. ✅ La mesa NO se libera cuando el estado es DELIVERED

**Estado: RESUELTO** ✅
