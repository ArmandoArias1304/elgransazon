# REFACTORING CASHIER VIEWS - CONSOLIDACIÓN

## Fecha
2024-11-04

## Objetivo
Simplificar las vistas del cajero eliminando la separación entre "Mis Pedidos" y "Mis Cobros", consolidando toda la información en una sola vista `list.html` que muestre:
1. **Tabla 1**: Pedidos creados por el cajero actual (como funciona para el waiter)
2. **Tabla 2**: Pedidos globales sin cobrar (estado DELIVERED) que cualquier cajero puede cobrar

## Cambios Realizados

### 1. Backend: `CashierOrderServiceImpl.java`

#### Modificación del método `findAll()`
**Antes:**
```java
@Override
public List<Order> findAll() {
    // Cashier can see all orders (no filtering)
    return adminOrderService.findAll();
}
```

**Después:**
```java
@Override
public List<Order> findAll() {
    // Cashier can only see orders created by themselves (like waiter)
    String currentUsername = getCurrentUsername();
    log.debug("Cashier {} fetching their orders", currentUsername);
    return adminOrderService.findAll().stream()
            .filter(order -> order.getCreatedBy().equalsIgnoreCase(currentUsername))
            .collect(Collectors.toList());
}
```

#### Nuevo método agregado
```java
/**
 * Find orders created by current cashier employee
 * Used for "My Orders" view
 */
public List<Order> findOrdersByCurrentEmployee() {
    return findAll();
}
```

**Razón:** Ahora el cajero solo ve sus propios pedidos creados, igual que el mesero. Esto mantiene consistencia con el requerimiento: "quiero las mismas funcionalidades del waiter para el cashier".

---

### 2. Controller: `CashierController.java`

#### Eliminación del método `myOrders()`
**Método eliminado completo:**
```java
@GetMapping("/orders/my-orders")
public String myOrders(Authentication authentication, Model model) {
    // ... código completo eliminado ...
    return "cashier/orders/my-orders";
}
```

**Razón:** Ya no se necesita una ruta separada para "Mis Cobros" porque toda la información se muestra en `/cashier/orders`.

#### Refactorización del método `listOrders()`

**Cambios principales:**

1. **Obtención de pedidos del cajero actual:**
```java
// Antes: 
List<Order> orders = cashierOrderService.findAll(); // Devolvía todos los pedidos

// Después:
List<Order> myOrders = cashierOrderService.findOrdersByCurrentEmployee(); // Solo los del cajero actual
```

2. **Obtención de pedidos globales sin cobrar:**
```java
// NUEVO: Get global unpaid orders (DELIVERED status)
List<Order> unpaidOrders = cashierOrderService.findByStatus(OrderStatus.DELIVERED);
unpaidOrders = unpaidOrders.stream()
    .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
    .collect(Collectors.toList());
```

3. **Estadísticas separadas:**
```java
// Estadísticas para pedidos del cajero
long myTodayCount = ...
BigDecimal myTodayRevenue = ...
long myPendingCount = ...
long myPaidCount = ...

// Estadísticas para pedidos sin cobrar (global)
long unpaidCount = unpaidOrders.size();
BigDecimal unpaidTotal = ...
```

4. **Atributos del modelo actualizados:**
```java
// Antes:
model.addAttribute("orders", orders);
model.addAttribute("todayCount", todayCount);
// ... etc

// Después:
model.addAttribute("myOrders", myOrders);          // Pedidos del cajero
model.addAttribute("unpaidOrders", unpaidOrders);  // Pedidos sin cobrar (global)
model.addAttribute("myTodayCount", myTodayCount);
model.addAttribute("unpaidCount", unpaidCount);
// ... etc
```

---

### 3. Frontend: Nueva vista `list.html`

#### Estructura de la nueva vista

**Características principales:**

1. **Header único:**
   - Título: "Gestión de Pedidos"
   - Descripción: "Mis pedidos creados y pedidos por cobrar"
   - Botón: "Nuevo Pedido"

2. **Sección de estadísticas 1: "Mis Pedidos"**
   ```html
   <h3>Mis Pedidos</h3>
   - Pedidos Hoy (myTodayCount)
   - Ingresos Hoy (myTodayRevenue)
   - Pendientes (myPendingCount)
   - Cobrados (myPaidCount)
   ```

3. **Sección de estadísticas 2: "Pedidos Por Cobrar (Global)"**
   ```html
   <h3>Pedidos Por Cobrar (Global)</h3>
   - Órdenes Sin Cobrar (unpaidCount)
   - Total Por Cobrar (unpaidTotal)
   ```

4. **Tabla 1: "Mis Pedidos Creados"**
   - Muestra: `${myOrders}` (pedidos creados por el cajero actual)
   - Columnas: Nº Pedido, Fecha, Mesa, Tipo, Cliente, Estado, Total, Pago, Acciones
   - Botón "Cobrar" solo visible si estado = DELIVERED

5. **Tabla 2: "Pedidos Por Cobrar (Global)"**
   - Muestra: `${unpaidOrders}` (todos los pedidos con estado DELIVERED)
   - Columnas: Nº Pedido, Fecha, Mesa, Tipo, Cliente, **Creado Por**, Total, Pago, Acciones
   - Nota adicional: "Estos pedidos están listos para ser cobrados por cualquier cajero"
   - Botón "Cobrar" siempre visible
   - Estilo diferenciado con borde naranja

#### JavaScript
```javascript
function markAsPaid(orderId, orderNumber) {
    // AJAX call to /cashier/orders/{id}/change-status
    // newStatus=PAID
    // Cashier puede cobrar con cualquier método de pago
}
```

---

### 4. Dashboard: `dashboard.html`

**Cambio realizado:**

```html
<!-- Antes: 2 opciones -->
<a href="/cashier/orders/select-table">Crear Pedidos</a>
<a href="/cashier/orders/my-orders">Mis Pedidos</a>

<!-- Después: 2 opciones (pero diferente) -->
<a href="/cashier/orders/select-table">Crear Pedidos</a>
<a href="/cashier/orders">Ver Pedidos</a>
```

**Descripción actualizada:**
```
"Ver tus pedidos creados y todos los pedidos listos para cobrar.
Gestiona y cobra pedidos con cualquier método de pago."
```

---

### 5. Archivos Eliminados

1. **`my-orders.html`** - Ya no se necesita vista separada
2. **Método `myOrders()` en CashierController** - Ruta eliminada

---

## Comportamiento Final

### Flujo de trabajo del cajero:

1. **Login como cajero** → Dashboard

2. **Crear Pedidos:**
   - Selecciona tipo de pedido (DINE_IN, TAKEOUT, DELIVERY)
   - Si es DINE_IN → selecciona mesa
   - Ingresa datos del cliente
   - Selecciona items del menú
   - Crea el pedido
   - Pedido queda con estado PENDING
   - El pedido aparece en "Mis Pedidos Creados" (Tabla 1)

3. **Ver Pedidos:**
   - **Tabla 1 (Mis Pedidos):** Muestra todos los pedidos que el cajero ha creado
     - Puede ver pedidos en cualquier estado (PENDING, CONFIRMED, IN_PREPARATION, READY, DELIVERED, PAID)
     - Solo puede cobrar los que están en estado DELIVERED
   
   - **Tabla 2 (Pedidos Por Cobrar):** Muestra TODOS los pedidos del sistema en estado DELIVERED
     - Puede cobrar pedidos creados por otros empleados (meseros, otros cajeros)
     - Puede usar cualquier método de pago (incluido CASH)
     - Al cobrar, el pedido cambia a PAID y se asocia al cajero que lo cobró (paidBy)

4. **Restricciones del cajero:**
   - ✅ Puede crear pedidos
   - ✅ Solo ve sus propios pedidos creados (Tabla 1)
   - ✅ Puede ver TODOS los pedidos sin cobrar del sistema (Tabla 2)
   - ✅ Puede cobrar con CUALQUIER método de pago (incluyendo CASH)
   - ✅ Solo puede cambiar estado DELIVERED → PAID
   - ❌ No puede cancelar pedidos de otros
   - ❌ No puede editar pedidos de otros
   - ❌ No puede cambiar estados intermedios (PENDING → CONFIRMED, etc.)

---

## Diferencias entre Waiter y Cashier

| Característica | Waiter | Cashier |
|---------------|--------|---------|
| **Ver pedidos creados por sí mismo** | ✅ Solo los suyos | ✅ Solo los suyos |
| **Ver pedidos globales sin cobrar** | ❌ No | ✅ Sí (Tabla 2) |
| **Cobrar con CASH** | ❌ No puede | ✅ Sí puede |
| **Cobrar pedidos de otros** | ❌ No | ✅ Sí (solo DELIVERED) |
| **Cambio de estado** | READY → DELIVERED<br>DELIVERED → PAID (no CASH) | DELIVERED → PAID (cualquier método) |

---

## Ventajas de esta Refactorización

1. **Simplicidad:** Una sola vista en lugar de dos
2. **Claridad:** Separación visual clara entre "mis pedidos" y "pedidos por cobrar"
3. **Eficiencia:** El cajero puede ver de un vistazo:
   - Sus propios pedidos creados
   - Todos los pedidos que necesitan ser cobrados
4. **Consistencia:** Comportamiento similar al waiter para pedidos propios
5. **Flexibilidad:** Puede cobrar pedidos de cualquier empleado
6. **Estadísticas separadas:** Métricas claras para ambas categorías

---

## Testing Recomendado

### Caso 1: Crear y Cobrar Propio Pedido
1. Login como cajero (pedro)
2. Crear pedido → Mesa 5
3. Verificar que aparece en Tabla 1 (Mis Pedidos)
4. Cambiar estado manualmente a DELIVERED (desde chef/admin)
5. Verificar que aparece en:
   - Tabla 1 con botón "Cobrar"
   - Tabla 2 con botón "Cobrar"
6. Cobrar desde cualquier tabla
7. Verificar que desaparece de Tabla 2 y queda solo en Tabla 1 con estado PAID

### Caso 2: Cobrar Pedido de Otro Empleado
1. Login como waiter (maria)
2. Crear pedido → Mesa 3
3. Cambiar estado a DELIVERED
4. Logout
5. Login como cajero (pedro)
6. Ver pedidos → Verificar que el pedido de Maria aparece en Tabla 2
7. Cobrar el pedido
8. Verificar que:
   - Pedido cambia a PAID
   - Campo `paidBy` = pedro
   - Pedido desaparece de Tabla 2

### Caso 3: Estadísticas Separadas
1. Crear 2 pedidos como cajero pedro
2. Verificar "Pedidos Hoy" = 2 en sección "Mis Pedidos"
3. Tener 3 pedidos DELIVERED de otros empleados
4. Verificar "Órdenes Sin Cobrar" = 3 en sección "Pedidos Por Cobrar"
5. Las estadísticas deben ser independientes

---

## Conclusión

✅ El cajero ahora tiene las mismas funcionalidades del mesero para sus propios pedidos
✅ Además, tiene la capacidad exclusiva de cobrar pedidos globales con cualquier método de pago
✅ La interfaz es clara y eficiente con dos tablas separadas
✅ Se eliminó la complejidad de tener vistas separadas
✅ Todo está consolidado en una sola vista `list.html`

