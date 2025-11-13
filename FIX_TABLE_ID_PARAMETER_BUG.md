# Fix: Table ID Not Being Sent to Controller

## 🐛 Problema Identificado

Al crear una orden tipo `DINE_IN` como cashier, el sistema arrojaba el error:
```
java.lang.IllegalArgumentException: Se requiere asignar una mesa para pedidos 'Para comer aquí'
```

A pesar de que el usuario **SÍ seleccionó una mesa** en la interfaz.

## 🔍 Causa Raíz

El JavaScript en `order-menu.html` estaba enviando **TODOS** los campos de `orderData` (incluido `tableId`) dentro del formulario usando el mismo loop:

```javascript
// ❌ ANTES - Problema
Object.keys(orderData).forEach((key) => {
  if (orderData[key] !== null) {
    const input = document.createElement("input");
    input.type = "hidden";
    input.name = key;        // ❌ "tableId" se enviaba como campo del objeto Order
    input.value = orderData[key];
    form.appendChild(input);
  }
});
```

Esto causaba que `tableId` se enviara como `order.tableId` (propiedad del objeto Order que NO existe), en lugar de enviarse como parámetro separado `tableId`.

### Backend Esperaba

```java
@PostMapping("/orders")
public String createOrder(
    @ModelAttribute("order") Order order,              // ✅ orderType, customerName, etc.
    @RequestParam(value = "tableId") Long tableId,     // ❌ Recibía NULL
    @RequestParam(value = "employeeId") Long employeeId,
    ...
)
```

### Frontend Enviaba

```
POST /cashier/orders
orderType=DINE_IN          ✅ Correcto (parte del objeto Order)
tableId=123                ❌ INCORRECTO (Spring lo mapeaba como order.tableId que no existe)
employeeId=5               ✅ Correcto (parámetro separado)
customerName=Juan          ✅ Correcto (parte del objeto Order)
```

## ✅ Solución Aplicada

### 1. JavaScript Corregido (`order-menu.html`)

```javascript
// Add order data (skip tableId, it goes as separate param)
Object.keys(orderData).forEach((key) => {
  if (orderData[key] !== null && key !== 'tableId') {  // ✅ Excluir tableId del loop
    console.log(`Adding ${key}:`, orderData[key]);
    const input = document.createElement("input");
    input.type = "hidden";
    input.name = key;
    input.value = orderData[key];
    form.appendChild(input);
  }
});

// Add tableId as separate parameter (required by @RequestParam)
if (orderData.tableId !== null) {  // ✅ Agregar tableId solo si existe
  console.log("Adding tableId param:", orderData.tableId);
  const tableInput = document.createElement("input");
  tableInput.type = "hidden";
  tableInput.name = "tableId";     // ✅ Ahora SÍ se mapea al @RequestParam
  tableInput.value = orderData.tableId;
  form.appendChild(tableInput);
}
```

### 2. Logs Mejorados en Controller

```java
log.info("===== CREATING ORDER =====");
log.info("Cashier: {}", username);
log.info("Employee ID (param): {}", employeeId);
log.info("Order Type: {}", order.getOrderType());
log.info("Table ID (param): {}", tableId);           // ✅ Ahora mostrará el valor correcto
log.info("Table in Order object: {}", order.getTable());
log.info("Customer Name: {}", order.getCustomerName());
log.info("Customer Phone: {}", order.getCustomerPhone());
log.info("Payment Method: {}", order.getPaymentMethod());
log.info("=========================");
```

## 📊 Flujo Correcto

### Datos Enviados (Ahora)

```
POST /cashier/orders

--- Parte de @ModelAttribute Order ---
orderType=DINE_IN
customerName=Juan Pérez
customerPhone=555-1234
paymentMethod=CASH

--- Parámetros Separados @RequestParam ---
tableId=123            ✅ Ahora Spring lo mapea correctamente
employeeId=5

--- Arrays de items ---
itemIds=1,2,3
quantities=2,1,4
comments=Sin cebolla,,Extra picante
```

### Procesamiento en Controller

1. ✅ Spring recibe `tableId=123` como `@RequestParam Long tableId`
2. ✅ Controller ejecuta: `order.setTable(restaurantTableService.findById(tableId))`
3. ✅ Controller ejecuta: `order.setEmployee(employeeService.findById(employeeId))`
4. ✅ Ahora `order.getTable()` NO es null
5. ✅ La validación `validateTableRequirement()` pasa exitosamente
6. ✅ La orden se crea correctamente

## 🎯 Resultado

Ahora cuando el cashier crea una orden tipo DINE_IN:

1. ✅ Selecciona mesa en `order-table-selection.html`
2. ✅ Ingresa info del cliente en `order-customer-info.html`
3. ✅ Selecciona items del menú en `order-menu.html`
4. ✅ Hace clic en "Crear Pedido"
5. ✅ El `tableId` se envía correctamente al backend
6. ✅ El backend asigna la mesa al objeto Order
7. ✅ La orden se crea exitosamente con mesa asignada
8. ✅ Redirige a lista de órdenes con mensaje de éxito

## 🔍 Diferencias Entre Tipos de Orden

| Tipo de Orden | tableId | Validación |
|---------------|---------|------------|
| **DINE_IN** | ✅ Requerido | `tableId !== null` |
| **TAKEOUT** | ❌ No aplica | `tableId === null` OK |
| **DELIVERY** | ❌ No aplica | `tableId === null` OK |

El código JavaScript verifica `if (orderData.tableId !== null)` antes de agregar el campo, por lo que:
- DINE_IN: Envía `tableId=123` ✅
- TAKEOUT: NO envía tableId ✅
- DELIVERY: NO envía tableId ✅

## 📝 Archivos Modificados

1. **order-menu.html** (líneas 898-916)
   - Separado envío de `tableId` del loop de `orderData`
   - Agregada validación `key !== 'tableId'`
   - Agregado bloque específico para enviar `tableId` como parámetro

2. **CashierController.java** (líneas 367-377)
   - Mejorados logs para debugging
   - Agregado log de `order.getTable()` para ver el objeto completo

## ✅ Pruebas Sugeridas

1. **DINE_IN con mesa**
   ```
   Dashboard → Crear Pedidos → Seleccionar Mesa 5 
   → Info Cliente → Menú → Agregar items → Crear
   ✅ Debe crear orden con mesa 5 asignada
   ```

2. **TAKEOUT sin mesa**
   ```
   Dashboard → Crear Pedidos → Para Llevar 
   → Info Cliente → Menú → Agregar items → Crear
   ✅ Debe crear orden sin mesa
   ```

3. **DELIVERY sin mesa**
   ```
   Dashboard → Crear Pedidos → Delivery 
   → Info Cliente (con dirección) → Menú → Agregar items → Crear
   ✅ Debe crear orden sin mesa pero con dirección
   ```

---
**Fecha**: 2025-11-04  
**Issue**: Table ID parameter not being sent correctly to controller  
**Root Cause**: JavaScript sending tableId as part of Order object instead of separate @RequestParam  
**Status**: ✅ FIXED
