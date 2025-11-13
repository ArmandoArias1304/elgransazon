# 🍽️ Feature: Agregar Items a Órdenes Existentes

## 📋 Descripción General

Esta funcionalidad permite agregar items adicionales a órdenes que ya han sido creadas, especialmente útil para clientes que comen en el restaurante y quieren ordenar más items (ej: postre, bebidas adicionales) después de que su pedido original ya fue entregado.

## 🎯 Características Principales

### ✅ Estado de Items Individual

- Cada `OrderDetail` (item) ahora tiene su propio estado independiente
- Estados posibles: `PENDING`, `IN_PREPARATION`, `READY`, `DELIVERED`
- El estado general de la orden se calcula automáticamente basado en los estados de sus items

### ✅ Items Nuevos vs Originales

- Items agregados después de la creación inicial se marcan con `isNewItem = true`
- Permite identificar visualmente cuáles items son adicionales
- Timestamp `addedAt` para saber cuándo se agregó cada item

### ✅ Control por Chef

- El chef ve qué items están pendientes en cada orden
- Los items nuevos se destacan visualmente con un badge "NUEVO ITEM"
- Puede cambiar el estado de items individuales
- Solo puede modificar items que él mismo aceptó

## 🗂️ Cambios en la Base de Datos

### Nuevas Columnas en `order_details`

```sql
ALTER TABLE order_details
ADD COLUMN item_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
ADD COLUMN is_new_item BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN added_at DATETIME DEFAULT NULL,
ADD COLUMN prepared_by VARCHAR(100) DEFAULT NULL;
```

**Descripción:**

- `item_status`: Estado individual del item (PENDING, IN_PREPARATION, READY, DELIVERED)
- `is_new_item`: TRUE si el item fue agregado después de la creación inicial
- `added_at`: Timestamp de cuándo se agregó el item
- `prepared_by`: Username del chef que preparó este item específico

### Índices Agregados

```sql
CREATE INDEX idx_order_details_item_status ON order_details(item_status);
CREATE INDEX idx_order_details_is_new_item ON order_details(is_new_item);
```

## 🔄 Flujo de Trabajo

### Escenario Típico: Cliente Ordena Postre

1. **Pedido Inicial**

   ```
   Mesa 5 → Pizza + Coca-Cola
   Estado Items: PENDING
   Estado Orden: PENDING
   ```

2. **Chef Acepta**

   ```
   Chef marca items como IN_PREPARATION
   Estado Items: IN_PREPARATION
   Estado Orden: IN_PREPARATION
   ```

3. **Chef Completa**

   ```
   Chef marca items como READY
   Estado Items: READY
   Estado Orden: READY
   ```

4. **Mesero Entrega**

   ```
   Mesero marca items como DELIVERED
   Estado Items: DELIVERED
   Estado Orden: DELIVERED
   ```

5. **🆕 Cliente Quiere Postre**

   ```
   Mesero agrega: Flan
   - Pizza: DELIVERED
   - Coca-Cola: DELIVERED
   - Flan: PENDING (isNewItem = true)
   Estado Orden: IN_PREPARATION (recalculado)
   ```

6. **Chef Ve la Orden de Nuevo**

   ```
   En vista de "Órdenes Pendientes":
   - Solo ve Flan (PENDING)
   - Badge "NUEVO ITEM" destacado
   - Pizza y Coca-Cola no aparecen (ya DELIVERED)
   ```

7. **Chef Prepara Solo el Nuevo Item**

   ```
   Chef acepta solo Flan → IN_PREPARATION
   Chef completa solo Flan → READY
   ```

8. **Mesero Entrega el Postre**
   ```
   Flan: DELIVERED
   Todos los items DELIVERED → Orden: DELIVERED
   ```

## 🛠️ API Endpoints

### 1. Agregar Items a Orden Existente

**POST** `/{role}/orders/{id}/add-items`

**Parámetros:**

```json
{
  "itemIds": [5, 7],
  "quantities": [1, 2],
  "comments": ["Sin azúcar", "Extra caliente"]
}
```

**Restricciones:**

- Solo órdenes tipo `DINE_IN`
- Solo estados `READY`, `DELIVERED`, `IN_PREPARATION`
- Valida stock antes de agregar
- Solo roles: WAITER, ADMIN, MANAGER

**Respuesta Exitosa:**

```json
{
  "success": true,
  "message": "Se agregaron 2 items al pedido. Los nuevos items aparecerán en cocina como PENDIENTES.",
  "newItemsCount": 2,
  "newTotal": "$85.50",
  "order": {
    "id": 123,
    "orderNumber": "ORD-20251109-045",
    "status": "IN_PREPARATION",
    "pendingItemsCount": 2,
    "newItemsCount": 2,
    "hasPendingItems": true,
    "hasNewItems": true
  }
}
```

### 2. Cambiar Estado de Items Específicos

**POST** `/{role}/orders/{id}/change-items-status`

**Parámetros:**

```json
{
  "itemDetailIds": [789, 790],
  "newStatus": "IN_PREPARATION"
}
```

**Restricciones:**

- Chef solo puede cambiar sus propios items
- Transiciones válidas:
  - PENDING → IN_PREPARATION
  - IN_PREPARATION → READY
- No se puede cambiar items DELIVERED

**Respuesta Exitosa:**

```json
{
  "success": true,
  "message": "Se cambió el estado de 2 items a EN PREPARACIÓN",
  "orderStatus": "IN_PREPARATION",
  "order": { ... }
}
```

## 📊 Entidad OrderDetail (Actualizada)

```java
@Entity
@Table(name = "order_details")
public class OrderDetail {
    // Campos existentes...

    // NUEVOS CAMPOS
    @Enumerated(EnumType.STRING)
    @Column(name = "item_status")
    private OrderStatus itemStatus = OrderStatus.PENDING;

    @Column(name = "is_new_item")
    private Boolean isNewItem = false;

    @Column(name = "added_at")
    private LocalDateTime addedAt;

    @Column(name = "prepared_by")
    private String preparedBy;

    // NUEVOS MÉTODOS
    public boolean isNew() { return Boolean.TRUE.equals(isNewItem); }
    public boolean isPending() { return itemStatus == OrderStatus.PENDING; }
    public boolean isInPreparation() { return itemStatus == OrderStatus.IN_PREPARATION; }
    public boolean isReady() { return itemStatus == OrderStatus.READY; }
    public boolean isDelivered() { return itemStatus == OrderStatus.DELIVERED; }
    public void markAsNew() {
        this.isNewItem = true;
        this.addedAt = LocalDateTime.now();
    }
}
```

## 📊 Entidad Order (Métodos Nuevos)

```java
@Entity
@Table(name = "orders")
public class Order {
    // Métodos existentes...

    // NUEVOS MÉTODOS
    public OrderStatus calculateStatusFromItems() { ... }
    public void updateStatusFromItems() { ... }
    public long getPendingItemsCount() { ... }
    public long getNewItemsCount() { ... }
    public boolean hasPendingItems() { ... }
    public boolean hasNewItems() { ... }
    public List<OrderDetail> getPendingItems() { ... }
    public List<OrderDetail> getItemsInPreparation() { ... }
    public List<OrderDetail> getReadyItems() { ... }
    public boolean canAcceptNewItems() {
        return orderType == OrderType.DINE_IN &&
               (status == OrderStatus.READY ||
                status == OrderStatus.DELIVERED ||
                status == OrderStatus.IN_PREPARATION);
    }
}
```

## 🔒 Validaciones y Reglas de Negocio

### 1. ¿Cuándo se pueden agregar items?

- ✅ Solo pedidos `DINE_IN`
- ✅ Estados: `IN_PREPARATION`, `READY`, `DELIVERED`
- ❌ No en pedidos `TAKEOUT` o `DELIVERY`
- ❌ No en pedidos `CANCELLED` o `PAID`

### 2. Validación de Stock

- Se valida stock antes de agregar
- Se deduce stock automáticamente
- Error si no hay suficiente inventario

### 3. Control de Acceso por Rol

#### WAITER

- ✅ Puede agregar items a sus propias órdenes
- ✅ Puede agregar items a órdenes de mesas activas

#### CHEF

- ❌ No puede agregar items
- ✅ Puede cambiar estado de items pendientes
- ✅ Solo items que él aceptó

#### ADMIN/MANAGER

- ✅ Puede agregar items a cualquier orden
- ✅ Puede cambiar estado de cualquier item

### 4. Cálculo de Estado de Orden

El estado general de la orden se calcula automáticamente:

```java
- Todos DELIVERED → Orden: DELIVERED
- Todos READY → Orden: READY
- Todos PENDING → Orden: PENDING
- Al menos uno IN_PREPARATION → Orden: IN_PREPARATION
```

## 🎨 Interfaz de Usuario (Próximos Pasos)

### Vista del Chef (pending.html)

**Cambios Necesarios:**

1. Mostrar solo items con `itemStatus = PENDING`
2. Badge especial para items con `isNewItem = true`
3. Botones para cambiar estado de items individuales
4. Contador de items nuevos en la card

**Ejemplo de Badge:**

```html
<span class="badge badge-success badge-pulse">
  <i class="fas fa-plus-circle"></i> NUEVO ITEM
</span>
```

### Vista del Mesero (Ver Orden)

**Botón "Agregar Items":**

- Visible solo si `order.canAcceptNewItems() == true`
- Abre modal con menú completo
- Envía request a `/orders/{id}/add-items`

### Vista de Detalles de Orden

**Tabla de Items Mejorada:**

```html
<tr>
  <td>Pizza Margarita</td>
  <td>1</td>
  <td>$12.00</td>
  <td>
    <span class="badge badge-success">ENTREGADO</span>
  </td>
  <td>-</td>
</tr>
<tr class="table-info">
  <td>
    Flan
    <span class="badge badge-warning">NUEVO</span>
  </td>
  <td>1</td>
  <td>$5.00</td>
  <td>
    <span class="badge badge-primary">EN PREPARACIÓN</span>
  </td>
  <td>Juan Pérez (Chef)</td>
</tr>
```

## 📝 Logs y Auditoría

### Eventos Registrados

```java
// Cuando se agregan items
log.info("Added {} new items to order {}. New total: {}",
         newItems.size(), orderNumber, formattedTotal);

// Cuando chef cambia estado de items
log.info("Item '{}' status changed: {} -> {}",
         itemName, oldStatus, newStatus);

// Cuando se recalcula estado de orden
log.info("Order {} status recalculated to: {}",
         orderNumber, newStatus);
```

## 🚀 Migración de Datos Existentes

El script de migración:

1. Agrega las columnas nuevas
2. Establece valores por defecto
3. Sincroniza `item_status` con el estado de la orden
4. Marca todos los items existentes como NO nuevos

```sql
UPDATE order_details od
JOIN orders o ON od.id_order = o.id_order
SET
    od.item_status = o.status,
    od.is_new_item = FALSE,
    od.added_at = od.created_at
WHERE od.item_status = 'PENDING' OR od.item_status IS NULL;
```

## ✅ Checklist de Implementación

### Backend

- [x] Modificar entidad `OrderDetail`
- [x] Modificar entidad `Order`
- [x] Actualizar `OrderService` interface
- [x] Implementar en `OrderServiceImpl`
- [x] Implementar en `ChefOrderServiceImpl`
- [x] Agregar endpoints en `OrderController`
- [x] Crear script de migración SQL

### Frontend (Pendiente)

- [ ] Actualizar vista `chef/orders/pending.html`
- [ ] Agregar badge "NUEVO ITEM"
- [ ] Botón "Agregar Items" en vista de mesero
- [ ] Modal para seleccionar items adicionales
- [ ] Actualizar tabla de detalles de orden
- [ ] JavaScript para llamar endpoints AJAX

### Testing (Pendiente)

- [ ] Test unitarios para `OrderDetail` métodos
- [ ] Test unitarios para `Order.calculateStatusFromItems()`
- [ ] Test de integración para `addItemsToExistingOrder()`
- [ ] Test de integración para `changeItemsStatus()`
- [ ] Test de validaciones de negocio

## 🐛 Casos Edge y Manejo de Errores

### 1. Stock Insuficiente

```java
throw new IllegalStateException(
    "Stock insuficiente para: Pizza Hawaiana"
);
```

### 2. Orden No Válida

```java
throw new IllegalStateException(
    "No se pueden agregar items a este pedido. " +
    "Solo pedidos para COMER AQUÍ pueden recibir items adicionales."
);
```

### 3. Item Ya Entregado

```java
throw new IllegalStateException(
    "No se puede cambiar el estado de un item ya entregado: Flan"
);
```

### 4. Chef No Autorizado

```java
throw new IllegalStateException(
    "Solo el chef que aceptó este item puede cambiar su estado: Pizza"
);
```

## 📚 Referencias

- **OrderService.java** - Interface con métodos nuevos
- **OrderServiceImpl.java** - Implementación completa
- **ChefOrderServiceImpl.java** - Implementación con restricciones
- **OrderController.java** - Endpoints REST
- **OrderDetail.java** - Entidad actualizada
- **Order.java** - Entidad con métodos de cálculo
- **add_item_status_fields.sql** - Script de migración

---

**Fecha de Implementación:** 9 de Noviembre, 2025  
**Desarrollador:** AI Assistant  
**Estado:** ✅ Backend Completo - Frontend Pendiente
