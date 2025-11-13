# Refactorización de la Entidad Order - Seguimiento de Empleados

## 📋 Resumen de Cambios

Se refactorizó la entidad `Order` para clarificar y mejorar el seguimiento de los empleados involucrados en cada etapa del proceso de pedido.

## 🎯 Problema Identificado

Anteriormente, la entidad `Order` tenía campos ambiguos:
- `employee` - No estaba claro si era quien creó o cobró la orden
- `createdBy` - String que duplicaba información
- No había campo para rastrear quién preparó la orden

## ✅ Solución Implementada

### Nuevos Campos en la Entidad Order

Se agregaron dos nuevos campos tipo `Employee` a la entidad:

1. **`employee`** (existente, clarificado)
   - **Propósito**: Empleado que creó/tomó el pedido
   - **Rol típico**: Mesero (Waiter)
   - **Cuándo se establece**: Al crear la orden

2. **`preparedBy`** (NUEVO)
   - **Propósito**: Empleado que preparó/cocinó la orden
   - **Rol típico**: Chef
   - **Cuándo se establece**: Cuando el chef acepta la orden (cambia estado a `IN_PREPARATION`)
   - **Campo en BD**: `id_prepared_by`

3. **`paidBy`** (NUEVO)
   - **Propósito**: Empleado que cobró la orden
   - **Rol típico**: Cajero o Mesero (según método de pago)
   - **Cuándo se establece**: Cuando la orden se marca como `PAID`
   - **Campo en BD**: `id_paid_by`

## 💳 Reglas de Negocio para Cobro

### Mesero (Waiter)
- ✅ Puede cobrar con: Tarjeta de Crédito, Tarjeta de Débito, Transferencia
- ❌ NO puede cobrar con: Efectivo

### Cajero (Cashier/Admin)
- ✅ Puede cobrar con cualquier método de pago, incluyendo Efectivo

### Lógica de Validación
La validación se realiza en el `OrderController` en el método `changeStatus`:
```java
if ("waiter".equalsIgnoreCase(role) && order.getPaymentMethod() == PaymentMethodType.CASH) {
    response.put("success", false);
    response.put("message", "Los meseros no pueden cobrar órdenes en efectivo. Solo el cajero puede hacerlo.");
    return response;
}
```

## 📝 Archivos Modificados

### 1. Entidad y Base de Datos

#### `Order.java`
- Agregados campos `preparedBy` y `paidBy`
- Actualizado `@ToString` para excluir nuevos campos
- Agregados JavaDocs explicativos

#### `database/add_prepared_paid_by_columns.sql` (NUEVO)
Script SQL para agregar las columnas a la base de datos:
```sql
ALTER TABLE orders
ADD COLUMN id_prepared_by BIGINT NULL AFTER id_employee;

ALTER TABLE orders
ADD COLUMN id_paid_by BIGINT NULL AFTER id_prepared_by;

-- Foreign keys
ALTER TABLE orders
ADD CONSTRAINT fk_orders_prepared_by
FOREIGN KEY (id_prepared_by) REFERENCES employee(id_empleado)
ON DELETE SET NULL;

ALTER TABLE orders
ADD CONSTRAINT fk_orders_paid_by
FOREIGN KEY (id_paid_by) REFERENCES employee(id_empleado)
ON DELETE SET NULL;
```

**⚠️ IMPORTANTE**: Ejecutar este script SQL antes de iniciar la aplicación.

### 2. Servicios

#### `OrderServiceImpl.java`
Actualizado método `changeStatus`:
- Establece `preparedBy` cuando el estado cambia a `READY`
- Establece `paidBy` cuando el estado cambia a `PAID`

#### `ChefOrderServiceImpl.java`
Actualizado método `changeStatus`:
- Establece `preparedBy` cuando el chef acepta la orden (`PENDING` → `IN_PREPARATION`)

#### `WaiterOrderServiceImpl.java`
Simplificado método `validateStatusChangeForWaiter`:
- Removida validación de efectivo (ahora está en el controller)

### 3. Controladores

#### `OrderController.java`
Actualizado método `changeStatus`:
- Establece `preparedBy` al buscar el employee actual
- Establece `paidBy` al buscar el employee actual
- Valida restricción de efectivo para meseros
- Retorna error descriptivo si mesero intenta cobrar en efectivo

Actualizado método `buildOrderDTO`:
- Incluye información de `createdBy`, `preparedBy` y `paidBy`

#### `ChefController.java`
Actualizado método `myOrders`:
- Filtra órdenes para mostrar solo las preparadas por el chef autenticado
- Compara `order.getPreparedBy().getUsername()` con el username del chef

### 4. Repositorio

#### `OrderRepository.java`
Actualizado `findByIdWithDetails`:
- Incluye `LEFT JOIN FETCH o.preparedBy`
- Incluye `LEFT JOIN FETCH o.paidBy`

### 5. Vistas HTML

#### `chef/orders/my-orders.html`
- Muestra "Levantado por" con el nombre del mesero que creó la orden
- Muestra "Preparado por" con el nombre del chef que cocinó la orden
- Muestra "Cobrado por" (solo si está pagada) con el nombre de quien cobró

## 🔄 Flujo de Trabajo Actualizado

### Creación de Orden
1. **Mesero** crea la orden
   - `employee` = Mesero
   - `preparedBy` = null
   - `paidBy` = null
   - Estado: `PENDING`

### Preparación de Orden
2. **Chef** acepta la orden
   - `preparedBy` = Chef que acepta
   - Estado: `PENDING` → `IN_PREPARATION`

3. **Chef** marca como lista
   - Estado: `IN_PREPARATION` → `READY`

### Entrega y Cobro
4. **Mesero** entrega la orden
   - Estado: `READY` → `DELIVERED`

5. **Cajero o Mesero** cobra la orden
   - Si método de pago = EFECTIVO: Solo **Cajero** puede marcar como `PAID`
   - Si método de pago = OTRO: **Mesero o Cajero** puede marcar como `PAID`
   - `paidBy` = Empleado que cobra
   - Estado: `DELIVERED` → `PAID`

## 📊 Beneficios

1. **Trazabilidad Completa**: Ahora se puede rastrear exactamente quién:
   - Tomó el pedido
   - Preparó la comida
   - Cobró al cliente

2. **Claridad de Responsabilidades**: Cada empleado tiene un rol claro en el proceso

3. **Auditoría Mejorada**: Mejor seguimiento para reportes y análisis de desempeño

4. **Historial Personal del Chef**: Cada chef ve solo los pedidos que él preparó

5. **Control de Efectivo**: Solo el cajero puede manejar pagos en efectivo, mejorando el control financiero

## 🧪 Pruebas Recomendadas

1. **Crear orden como mesero**
   - Verificar que `employee` se establece correctamente

2. **Aceptar orden como chef**
   - Verificar que `preparedBy` se establece al cambiar a `IN_PREPARATION`

3. **Cobrar orden como mesero con tarjeta**
   - Verificar que `paidBy` se establece correctamente
   - Verificar que la orden se marca como `PAID`

4. **Intentar cobrar orden en efectivo como mesero**
   - Verificar que se rechaza con mensaje de error
   - Verificar que no se establece `paidBy`

5. **Cobrar orden en efectivo como cajero**
   - Verificar que se permite y `paidBy` se establece correctamente

6. **Ver historial como chef**
   - Verificar que solo aparecen órdenes preparadas por ese chef
   - Verificar que se muestran correctamente los nombres de empleados

## � Restricciones de Acceso del Chef

### Vista "Gestión de Pedidos" (`/chef/orders/pending`)

El chef puede ver:
- **PENDING**: Todas las órdenes pendientes (disponibles para aceptar por cualquier chef)
- **IN_PREPARATION**: Solo las órdenes que ÉL aceptó (donde `preparedBy` = chef actual)

El chef NO puede ver:
- Órdenes IN_PREPARATION aceptadas por otros chefs
- Órdenes que el admin cambió a IN_PREPARATION sin asignar un chef

### Vista "Historial de Pedidos" (`/chef/orders/my-orders`)

El chef solo ve órdenes donde:
- `preparedBy` = chef actual
- Estado != PENDING && Estado != IN_PREPARATION

### Validaciones de Cambio de Estado

- **PENDING → IN_PREPARATION**: Cualquier chef puede aceptar
- **IN_PREPARATION → READY**: Solo el chef que aceptó la orden (`preparedBy`) puede marcarla como lista
- Si otro chef intenta cambiar el estado: Error "Solo el chef que aceptó esta orden puede cambiar su estado"

## �🚀 Próximos Pasos

1. **⚠️ EJECUTAR SQL**: Ejecutar el script SQL en la base de datos
   ```bash
   mysql -u [usuario] -p [base_de_datos] < database/add_prepared_paid_by_columns.sql
   ```

2. Reiniciar la aplicación

3. **Probar el flujo completo**:
   - a) Mesero crea orden → verifica `employee` se establece
   - b) Chef acepta orden (PENDING → IN_PREPARATION) → verifica `preparedBy` se establece
   - c) Verificar que otros chefs NO ven esa orden en su lista de "En Preparación"
   - d) Chef marca como lista (IN_PREPARATION → READY) → verifica que funciona
   - e) Verificar que solo el chef que preparó la orden la ve en su historial

4. **Probar restricciones de cobro**:
   - Mesero intenta cobrar efectivo → debe rechazar
   - Cajero cobra efectivo → debe funcionar
   - Mesero cobra con tarjeta → debe funcionar y establecer `paidBy`

5. Capacitar al personal sobre las nuevas reglas

---

**Fecha de Implementación**: 4 de Noviembre, 2025
**Desarrollador**: Sistema de Refactorización
**Estado**: ✅ Completado - Con Restricciones de Acceso por Chef
