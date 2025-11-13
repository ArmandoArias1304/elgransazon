# Implementación del Patrón Strategy para Orders con Roles Dinámicos

## Resumen de la Implementación

Se ha implementado exitosamente el patrón Strategy para manejar las funcionalidades de Orders según el rol del usuario (Admin y Waiter) con rutas dinámicas.

## Componentes Creados

### 1. Patrón Strategy

#### OrderStrategy Interface
**Ubicación:** `com.aatechsolutions.elgransazon.application.strategy.OrderStrategy`

Define las operaciones basadas en roles:
- `filterOrders()` - Filtrar pedidos según el rol
- `canMarkAsPaid()` - Validar si puede marcar como pagado
- `canCancelOrder()` - Validar si puede cancelar
- `canEditOrder()` - Validar si puede editar
- `canViewOrder()` - Validar si puede ver detalles
- `getRoleName()` - Obtener nombre del rol
- `getBasePath()` - Obtener path base (admin/waiter)

#### AdminOrderStrategy
**Ubicación:** `com.aatechsolutions.elgransazon.application.strategy.AdminOrderStrategy`

Permisos completos:
- ✅ Ve todos los pedidos del sistema
- ✅ Puede marcar cualquier pedido como PAID (incluso efectivo)
- ✅ Puede cancelar cualquier pedido
- ✅ Puede editar cualquier pedido
- ✅ Puede ver cualquier pedido
- 📍 Base path: `admin`

#### WaiterOrderStrategy
**Ubicación:** `com.aatechsolutions.elgransazon.application.strategy.WaiterOrderStrategy`

Permisos restringidos:
- ⚠️ Solo ve sus propios pedidos (creados por él)
- ⚠️ NO puede marcar pedidos CASH como PAID (solo otros métodos de pago)
- ⚠️ Solo puede cancelar sus propios pedidos
- ⚠️ Solo puede editar sus propios pedidos
- ⚠️ Solo puede ver sus propios pedidos
- 📍 Base path: `waiter`

#### OrderStrategyFactory
**Ubicación:** `com.aatechsolutions.elgransazon.application.strategy.OrderStrategyFactory`

Factory que:
- Obtiene la estrategia correcta según el rol del usuario autenticado
- Prioriza ADMIN sobre WAITER si tiene ambos roles
- Proporciona métodos auxiliares para validaciones

### 2. Controller Refactorizado

#### OrderController
**Ubicación:** `com.aatechsolutions.elgransazon.presentation.controller.OrderController`

**Cambios principales:**

1. **Rutas dinámicas:** `@RequestMapping("/{role}/orders")`
   - `/admin/orders` para administradores
   - `/waiter/orders` para meseros

2. **Inyección del Strategy Factory:**
   ```java
   private final OrderStrategyFactory strategyFactory;
   ```

3. **Validación de roles en cada endpoint:**
   ```java
   private void validateRole(String role, Authentication authentication, RedirectAttributes redirectAttributes)
   ```

4. **Aplicación de estrategias:**
   - Filtrado de pedidos por rol
   - Validaciones de permisos antes de operaciones
   - Restricción de botón "Pagar" para efectivo

5. **Métodos actualizados:**
   - ✅ `listOrders()` - Filtra por rol
   - ✅ `selectTable()` - Rutas dinámicas
   - ✅ `customerInfoForm()` - Rutas dinámicas
   - ✅ `menuSelection()` - Rutas dinámicas
   - ✅ `newOrderForm()` - Rutas dinámicas
   - ✅ `createOrder()` - Validación de permisos
   - ✅ `editOrderForm()` - Validación de permisos
   - ✅ `updateOrder()` - Validación de permisos
   - ✅ `viewOrder()` - Validación de permisos
   - ✅ `cancelOrder()` - Validación de permisos (AJAX)
   - ✅ `changeStatus()` - Validación especial para CASH (AJAX)
   - ✅ `getValidStatuses()` - Filtrado de estados permitidos (AJAX)

### 3. Vistas Creadas

#### Directorio Waiter
**Ubicación:** `src/main/resources/templates/waiter/orders/`

Vistas creadas:
- ✅ `list.html` - Lista de pedidos (con restricciones para CASH)
- ✅ `order-table-selection.html` - Selección de mesa
- ✅ `order-customer-info.html` - Información del cliente
- ✅ `order-menu.html` - Menú de selección de items
- ✅ `form.html` - Formulario de pedido
- ✅ `view.html` - Vista de detalles

#### Características especiales en list.html (Waiter):

1. **Botón "Pagar" para efectivo:**
   ```html
   <!-- Cash Payment - Disabled (show message) -->
   <button
     th:if="${order.status.name() == 'DELIVERED' && order.paymentMethod.name() == 'CASH'}"
     class="p-2 rounded-lg bg-gray-100 dark:bg-gray-800 text-gray-400 dark:text-gray-600 cursor-not-allowed"
     disabled
     title="Los pagos en efectivo solo pueden ser procesados por el cajero"
   >
     <i class="fas fa-ban"></i>
   </button>
   ```

2. **Botón "Pagar" para otros métodos:**
   ```html
   <!-- Pay (only DELIVERED and NON-CASH) -->
   <button
     th:if="${order.status.name() == 'DELIVERED' && order.paymentMethod.name() != 'CASH'}"
     class="btn-mark-paid p-2 rounded-lg bg-green-50 dark:bg-green-900/20 text-green-600 dark:text-green-400..."
     th:data-order-id="${order.idOrder}"
     th:data-order-number="${order.orderNumber}"
     title="Marcar como Pagado"
   >
     <i class="fas fa-dollar-sign"></i>
   </button>
   ```

3. **Validación en JavaScript:**
   ```javascript
   // Add warning for CASH payments if PAID status is not available
   let warningHtml = '';
   if (paymentMethod === 'CASH' && !data.canMarkAsPaid) {
     warningHtml = '<div class="mt-4 p-3 bg-yellow-50 border border-yellow-200 rounded-lg text-sm text-yellow-800">
       <i class="fas fa-exclamation-triangle mr-2"></i>
       Los pagos en efectivo solo pueden ser procesados por el cajero
     </div>';
   }
   ```

### 4. Dashboard Actualizado

#### Waiter Dashboard
**Ubicación:** `src/main/resources/templates/waiter/dashboard.html`

**Cambio principal:**
```html
<!-- CARD 2: Pedidos -->
<a href="/waiter/orders" ...>
  <h3>Mis Pedidos</h3>
  <p>Gestionar mis pedidos asignados</p>
</a>
```

## Flujo de Trabajo para Waiter

### 1. Crear Pedido
1. Dashboard → Click en "Mis Pedidos"
2. `/waiter/orders` → Click en "Nuevo Pedido"
3. `/waiter/orders/select-table` → Seleccionar tipo de pedido y mesa (si es DINE_IN)
4. `/waiter/orders/customer-info` → Registrar datos del cliente (si es TAKEOUT o DELIVERY)
5. `/waiter/orders/menu` → Seleccionar items del menú
6. Crear pedido → Redirección a `/waiter/orders`

### 2. Ver y Gestionar Pedidos
- Solo ve pedidos creados por él
- Puede editar pedidos PENDING
- Puede cambiar estado según el workflow
- Puede cancelar si el estado lo permite

### 3. Procesar Pagos
- **Tarjeta/Transferencia:** Puede marcar como PAID directamente
- **Efectivo:** Botón deshabilitado, mensaje: "Los pagos en efectivo solo pueden ser procesados por el cajero"

## Rutas Disponibles

### Admin
- `GET /admin/orders` - Listar todos los pedidos
- `GET /admin/orders/select-table` - Selección de mesa
- `GET /admin/orders/customer-info` - Información del cliente
- `GET /admin/orders/menu` - Menú de selección
- `GET /admin/orders/new` - Formulario nuevo pedido
- `POST /admin/orders` - Crear pedido
- `GET /admin/orders/edit/{id}` - Formulario editar
- `POST /admin/orders/{id}` - Actualizar pedido
- `GET /admin/orders/view/{id}` - Ver detalles
- `POST /admin/orders/{id}/cancel` - Cancelar (AJAX)
- `POST /admin/orders/{id}/change-status` - Cambiar estado (AJAX)
- `GET /admin/orders/{id}/valid-statuses` - Estados válidos (AJAX)

### Waiter
- `GET /waiter/orders` - Listar mis pedidos
- `GET /waiter/orders/select-table` - Selección de mesa
- `GET /waiter/orders/customer-info` - Información del cliente
- `GET /waiter/orders/menu` - Menú de selección
- `GET /waiter/orders/new` - Formulario nuevo pedido
- `POST /waiter/orders` - Crear pedido
- `GET /waiter/orders/edit/{id}` - Formulario editar
- `POST /waiter/orders/{id}` - Actualizar pedido
- `GET /waiter/orders/view/{id}` - Ver detalles
- `POST /waiter/orders/{id}/cancel` - Cancelar (AJAX)
- `POST /waiter/orders/{id}/change-status` - Cambiar estado (AJAX)
- `GET /waiter/orders/{id}/valid-statuses` - Estados válidos (AJAX)

## Validaciones Implementadas

### 1. Validación de Rol en Path
```java
private void validateRole(String role, Authentication authentication, RedirectAttributes redirectAttributes) {
    String expectedRole = strategyFactory.getBasePath(authentication);
    if (!role.equalsIgnoreCase(expectedRole)) {
        throw new IllegalStateException("Access denied: Role mismatch");
    }
}
```

### 2. Filtrado de Pedidos (Waiter)
```java
// Waiter solo ve sus propios pedidos
orders = strategy.filterOrders(orders, username);
```

### 3. Validación para Marcar como PAID
```java
if (status == OrderStatus.PAID) {
    if (!strategy.canMarkAsPaid(order, order.getPaymentMethod())) {
        response.put("success", false);
        response.put("message", "Los pedidos en efectivo solo pueden ser cobrados por un cajero");
        return response;
    }
}
```

### 4. Validación de Permisos de Edición
```java
if (!strategy.canEditOrder(existingOrder, username)) {
    redirectAttributes.addFlashAttribute("errorMessage", 
        "No tiene permisos para editar este pedido");
    return "redirect:/" + role + "/orders";
}
```

## Reglas de Negocio

### Para Administrador (ADMIN)
- Acceso completo a todos los pedidos
- Puede cobrar cualquier método de pago
- Sin restricciones

### Para Mesero (WAITER)
1. **Pedidos:**
   - Solo puede ver/editar/cancelar sus propios pedidos
   - Puede crear nuevos pedidos

2. **Pagos:**
   - ✅ Puede cobrar: Tarjeta de Crédito, Tarjeta de Débito, Transferencia
   - ❌ NO puede cobrar: Efectivo (requiere cajero)
   - Mensaje mostrado: "Los pagos en efectivo solo pueden ser procesados por el cajero"

3. **Estados:**
   - Puede cambiar estados según el workflow normal
   - Estado PAID filtrado automáticamente si el pago es en CASH

## Archivos Modificados/Creados

### Backend
- ✅ `OrderStrategy.java` (nuevo)
- ✅ `AdminOrderStrategy.java` (nuevo)
- ✅ `WaiterOrderStrategy.java` (nuevo)
- ✅ `OrderStrategyFactory.java` (nuevo)
- ✅ `OrderController.java` (modificado)

### Frontend
- ✅ `waiter/orders/list.html` (nuevo)
- ✅ `waiter/orders/order-table-selection.html` (nuevo)
- ✅ `waiter/orders/order-customer-info.html` (nuevo)
- ✅ `waiter/orders/order-menu.html` (nuevo)
- ✅ `waiter/orders/form.html` (nuevo)
- ✅ `waiter/orders/view.html` (nuevo)
- ✅ `waiter/dashboard.html` (modificado)

## Próximos Pasos (Futuro)

1. Crear módulo de Cajero (Cashier) para procesar pagos en efectivo
2. Agregar reportes de ventas por mesero
3. Implementar sistema de propinas
4. Agregar notificaciones en tiempo real para cambios de estado

## Notas Importantes

- El sistema usa Spring Security para autenticación
- Las rutas son dinámicas basadas en el rol del usuario
- El patrón Strategy permite fácil extensión para nuevos roles
- Las vistas son responsivas y tienen modo oscuro
- JavaScript usa SweetAlert2 para modales elegantes
- AJAX para operaciones sin recargar página
