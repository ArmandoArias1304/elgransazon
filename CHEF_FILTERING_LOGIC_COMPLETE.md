# Lógica Completa de Filtrado para el Chef

## 🎯 Objetivo

El chef **SOLO debe ver**:

1. Órdenes que contengan AL MENOS UN item que requiera preparación
2. Dentro de esas órdenes, SOLO los items que requieren preparación (ocultar bebidas, postres pre-empacados, etc.)

## 📋 Flujo de Lógica

### Escenario 1: Orden solo con bebidas

```
Usuario crea orden:
- Mesa 5
- 2x Coca-Cola (requires_preparation = FALSE)
- 1x Pepsi (requires_preparation = FALSE)

Resultado para CHEF:
❌ Esta orden NO aparece en la vista del chef
✅ La orden pasa directamente a READY (auto-advance)
✅ El mesero/cajero puede proceder a entregarla
```

### Escenario 2: Orden solo con comida

```
Usuario crea orden:
- Mesa 8
- 1x Hamburguesa (requires_preparation = TRUE)
- 1x Pizza (requires_preparation = TRUE)

Resultado para CHEF:
✅ Esta orden SÍ aparece en la vista del chef
✅ El chef ve AMBOS items (Hamburguesa y Pizza)
✅ Estado de la orden: PENDING
✅ El chef debe aceptarla para comenzar a prepararla
```

### Escenario 3: Orden mixta (comida + bebidas)

```
Usuario crea orden:
- Mesa 3
- 1x Hamburguesa (requires_preparation = TRUE)
- 2x Coca-Cola (requires_preparation = FALSE)
- 1x Ensalada (requires_preparation = TRUE)

Resultado para CHEF:
✅ Esta orden SÍ aparece en la vista del chef
✅ El chef ve SOLO:
   - Hamburguesa ✅
   - Ensalada ✅
✅ El chef NO ve:
   - Coca-Cola ❌ (oculta del listado)
✅ Estado de la orden: PENDING
✅ El chef debe preparar solo los items visibles
```

### Escenario 4: Agregar items a orden existente

**Caso A: Orden inicia con bebidas, luego se agrega comida**

```
1. Usuario crea orden:
   - Mesa 10
   - 2x Coca-Cola (requires_preparation = FALSE)

   Resultado: Chef NO ve la orden (auto READY)

2. Usuario AGREGA items:
   - 1x Pizza (requires_preparation = TRUE)

   Resultado:
   ✅ Ahora la orden SÍ aparece para el chef
   ✅ Chef ve SOLO la Pizza
   ✅ Chef NO ve las Coca-Colas
   ✅ Estado vuelve a PENDING (espera aceptación del chef)
```

**Caso B: Orden inicia con comida, luego se agregan bebidas**

```
1. Usuario crea orden:
   - Mesa 7
   - 1x Hamburguesa (requires_preparation = TRUE)

   Resultado: Chef ve la orden con Hamburguesa

2. Usuario AGREGA items:
   - 2x Pepsi (requires_preparation = FALSE)

   Resultado:
   ✅ La orden sigue visible para el chef
   ✅ Chef SIGUE viendo SOLO la Hamburguesa
   ✅ Las Pepsi se agregan pero NO son visibles para el chef
```

## 🔧 Implementación Técnica

### 1. Filtrado a Nivel de Orden (ChefOrderServiceImpl)

```java
@Override
public List<Order> findAll() {
    // Cargar órdenes con detalles
    List<Order> allOrders = orderRepository.findAllWithDetails();

    // Filtrar órdenes que tengan AL MENOS UN item requiring preparation
    return allOrders.stream()
        .filter(this::hasItemsRequiringPreparation)
        .map(this::filterOrderDetailsForChef) // Filtrar items dentro
        .collect(Collectors.toList());
}
```

### 2. Verificación si la orden debe ser visible

```java
private boolean hasItemsRequiringPreparation(Order order) {
    return order.getOrderDetails().stream()
        .anyMatch(detail ->
            detail.getItemMenu() != null &&
            Boolean.TRUE.equals(detail.getItemMenu().getRequiresPreparation())
        );
}
```

### 3. Filtrado de Items Dentro de la Orden

```java
private Order filterOrderDetailsForChef(Order order) {
    // Filtrar SOLO items con requiresPreparation = true
    List<OrderDetail> filteredDetails = order.getOrderDetails().stream()
        .filter(detail ->
            detail.getItemMenu() != null &&
            Boolean.TRUE.equals(detail.getItemMenu().getRequiresPreparation())
        )
        .collect(Collectors.toList());

    // Reemplazar la lista de detalles con la versión filtrada
    order.getOrderDetails().clear();
    order.getOrderDetails().addAll(filteredDetails);

    return order;
}
```

### 4. Query Optimizada con FETCH JOIN

```java
// OrderRepository.java
@Query("SELECT DISTINCT o FROM Order o " +
       "LEFT JOIN FETCH o.orderDetails od " +
       "LEFT JOIN FETCH od.itemMenu " +
       "ORDER BY o.createdAt DESC")
List<Order> findAllWithDetails();
```

## 📊 Ejemplo Visual

### Vista del ADMIN/MESERO

```
Orden #ORD-001 - Mesa 5
├── Hamburguesa Clásica (x1) - $150.00 [PENDING]
├── Coca-Cola 3L (x2) - $100.00 [READY]
└── Ensalada César (x1) - $200.00 [PENDING]
Total: $550.00
```

### Vista del CHEF (misma orden)

```
Orden #ORD-001 - Mesa 5
├── Hamburguesa Clásica (x1) - $150.00 [PENDING]
└── Ensalada César (x1) - $200.00 [PENDING]

🚫 Coca-Cola NO visible (no requiere preparación)
```

## 🧪 Casos de Prueba

### Test 1: Orden solo bebidas

```sql
-- Crear orden con solo bebidas
INSERT INTO `order` (...) VALUES (...);
INSERT INTO order_detail (id_order, id_item_menu, quantity)
VALUES (LAST_INSERT_ID(), 1, 2); -- Coca-Cola

-- Verificar
SELECT * FROM item_menu WHERE id_item_menu = 1;
-- requires_preparation debe ser 0 (FALSE)

-- Resultado esperado:
-- Chef NO ve esta orden en su lista
```

### Test 2: Orden mixta

```sql
-- Crear orden mixta
INSERT INTO `order` (...) VALUES (...);
INSERT INTO order_detail (id_order, id_item_menu, quantity) VALUES
(LAST_INSERT_ID(), 2, 1), -- Hamburguesa (requires_preparation = 1)
(LAST_INSERT_ID(), 1, 2); -- Coca-Cola (requires_preparation = 0)

-- Resultado esperado:
-- Chef SÍ ve la orden
-- Chef ve SOLO la Hamburguesa (1 item en lugar de 3)
```

### Test 3: Agregar comida a orden de bebidas

```sql
-- 1. Crear orden con bebidas
INSERT INTO `order` (order_number, status, ...) VALUES ('ORD-001', 'READY', ...);
INSERT INTO order_detail VALUES (LAST_INSERT_ID(), 1, 2); -- Coca-Cola

-- 2. Agregar hamburguesa
INSERT INTO order_detail VALUES (1, 2, 1); -- Hamburguesa
UPDATE `order` SET status = 'PENDING' WHERE id_order = 1;

-- Resultado esperado:
-- Orden ahora visible para chef
-- Chef ve SOLO Hamburguesa
-- Estado: PENDING
```

## 📝 Logs de Debugging

Al ejecutar `chefOrderService.findAll()`, verás:

```
🔍 Chef findAll() - Loading orders with details for filtering
🔍 Total orders in DB: 10
🔍 Order ORD-001, Item 'Hamburguesa': requiresPreparation = true
🔍 Order ORD-001, Item 'Coca-Cola': requiresPreparation = false
🔍 Hiding item 'Coca-Cola' from chef view (doesn't require preparation)
🔍 Order ORD-001: 3 total items, 2 visible to chef
🔍 Order ORD-001 hasItemsRequiringPreparation: true
🔍 Order ORD-002, Item 'Pepsi': requiresPreparation = false
🔍 Order ORD-002 hasItemsRequiringPreparation: false
🔍 Orders visible to chef (after filtering): 8
```

## ✅ Beneficios

1. **Claridad para el chef**: Solo ve lo que debe preparar
2. **Eficiencia**: No pierde tiempo viendo bebidas/items listos
3. **Workflow correcto**: Las bebidas se marcan READY automáticamente
4. **Flexibilidad**: Si se agrega comida después, la orden aparece
5. **Consistencia**: Mismo comportamiento en todas las vistas del chef

## 🎯 Resultado Final

- ✅ Chef ve SOLO órdenes con items que requieren preparación
- ✅ Chef ve SOLO los items que debe preparar (no ve bebidas)
- ✅ Items sin preparación se marcan READY automáticamente
- ✅ El total de la orden se mantiene correcto en vista de admin/mesero
- ✅ La vista del chef está optimizada para la cocina

---

**Fecha**: 2025-11-09  
**Estado**: ✅ Implementado  
**Archivos modificados**:

- `ChefOrderServiceImpl.java` - Filtrado de órdenes e items
- `OrderRepository.java` - Query optimizada con FETCH JOIN
- `ItemMenu.java` - Campo requiresPreparation
