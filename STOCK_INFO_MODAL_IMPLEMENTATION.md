# Implementación de Información de Stock en Modal de Cancelación

## 📋 Resumen Ejecutivo

Se implementó un sistema de mensajes dinámicos para mostrar información precisa sobre la devolución de stock al cancelar pedidos. La implementación incluye cambios en **frontend** y **backend** para proporcionar retroalimentación detallada al usuario.

---

## 🎯 Problema Solucionado

### ❌ Problema Original

- Los modales mostraban mensajes incorrectos sobre devolución de stock
- Se basaban en el estado del **pedido completo**, no en el estado individual de cada **item**
- Causaban confusión: mostraban "stock debe ser devuelto manualmente" cuando en realidad se devolvía automáticamente

### ✅ Solución Implementada

- Mensajes genéricos en el modal de confirmación
- Análisis **item por item** en el backend después de cancelar
- Información detallada en el modal de éxito basada en la respuesta del backend

---

## 📦 Archivos Modificados

### Frontend (3 archivos)

1. **admin/orders/list.html**
2. **waiter/orders/list.html**
3. **cashier/orders/list.html**

### Backend (2 archivos)

4. **OrderController.java**
5. **CashierController.java**

---

## 🔧 Cambios en Frontend

### Función `confirmCancel()` - Antes

```javascript
html:
  `<p>¿Estás seguro de que deseas cancelar el pedido <strong>${orderNumber}</strong>?</p>` +
  `<ul class="text-left text-sm mt-3">` +
  // ... bullets complejos con lógica de estado del pedido
```

### Función `confirmCancel()` - Después

```javascript
html: `<p>¿Estás seguro de que deseas cancelar el pedido <strong>${orderNumber}</strong>?</p>` +
  `<p class="text-sm text-gray-600 dark:text-gray-400 mt-3">` +
  `El sistema analizará cada item y devolverá el stock según corresponda.` +
  `</p>`;
```

### Modal de Éxito - Mejoras

```javascript
// Muestra el stockInfo del backend
if (result.value.stockInfo) {
    message += `<br><br><p class="text-sm mt-2">${result.value.stockInfo}</p>`;
}

// Configuración mejorada
timer: 3000,  // Antes: 2000ms
showConfirmButton: true  // Antes: false
```

---

## 🔧 Cambios en Backend

### 1. Modificación del método `cancelOrder()`

**OrderController.java** (líneas ~838-848)

```java
// ANTES
if (!cancelled.getStatus().shouldReturnStockOnCancel()) {
    response.put("warning", "Los ingredientes deben ser devueltos manualmente al inventario");
}

// DESPUÉS
String stockInfo = analyzeStockReturn(cancelled);
if (stockInfo != null && !stockInfo.isEmpty()) {
    response.put("stockInfo", stockInfo);
}
```

**CashierController.java** - Mismo cambio aplicado

---

### 2. Nuevo método `analyzeStockReturn()`

Agregado en ambos controladores:

```java
/**
 * Analyze items to determine stock return information
 * Returns a message describing how stock was handled
 */
private String analyzeStockReturn(Order order) {
    if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
        return null;
    }

    int automaticItems = 0;
    int manualItems = 0;

    for (OrderDetail detail : order.getOrderDetails()) {
        OrderStatus itemStatus = detail.getItemStatus();

        // PENDING -> always automatic
        if (itemStatus == OrderStatus.PENDING) {
            automaticItems++;
            continue;
        }

        // READY -> check if requires preparation
        if (itemStatus == OrderStatus.READY) {
            if (detail.getItemMenu() != null &&
                !Boolean.TRUE.equals(detail.getItemMenu().getRequiresPreparation())) {
                // Auto-advanced to READY, never touched by chef
                automaticItems++;
            } else {
                // Chef prepared it, used ingredients
                manualItems++;
            }
            continue;
        }

        // IN_PREPARATION -> always manual
        if (itemStatus == OrderStatus.IN_PREPARATION) {
            manualItems++;
        }
    }

    // Build appropriate message
    if (automaticItems > 0 && manualItems == 0) {
        return "✅ Stock devuelto automáticamente para todos los items (" + automaticItems + " items)";
    } else if (manualItems > 0 && automaticItems == 0) {
        return "⚠️ Stock debe ser devuelto manualmente para todos los items (" + manualItems + " items)";
    } else if (automaticItems > 0 && manualItems > 0) {
        return "ℹ️ Stock devuelto: " + automaticItems + " items automáticos, " +
               manualItems + " items requieren devolución manual";
    }

    return null;
}
```

---

## 🔍 Lógica de Análisis de Items

### Devolución Automática de Stock

Un item tiene **devolución automática** cuando:

1. **Estado PENDING** (nunca tocado por el chef)

   ```
   Item nunca entró en preparación → ingredientes nunca usados
   ```

2. **Estado READY + requiresPreparation = FALSE**
   ```
   Item auto-avanzado a READY → nunca pasó por el chef
   Ej: Bebidas, ensaladas pre-hechas, etc.
   ```

### Devolución Manual de Stock

Un item requiere **devolución manual** cuando:

1. **Estado READY + requiresPreparation = TRUE**

   ```
   Chef preparó el item → usó ingredientes
   Stock debe ser devuelto manualmente al inventario
   ```

2. **Estado IN_PREPARATION**
   ```
   Chef está trabajando en el item → puede haber usado ingredientes
   Stock debe ser devuelto manualmente al inventario
   ```

---

## 📊 Mensajes Posibles

### 1. Todos Automáticos

```
✅ Stock devuelto automáticamente para todos los items (3 items)
```

**Escenario:** Pedido recién creado con items que no requieren preparación

---

### 2. Todos Manuales

```
⚠️ Stock debe ser devuelto manualmente para todos los items (4 items)
```

**Escenario:** Chef ya comenzó a preparar todos los items

---

### 3. Mixto

```
ℹ️ Stock devuelto: 2 items automáticos, 3 items requieren devolución manual
```

**Escenario:**

- 2 bebidas (auto-avanzadas a READY, nunca tocadas)
- 3 platos en preparación (chef trabajando en ellos)

---

## 🎨 Respuesta JSON del Backend

### Cancelación Exitosa

```json
{
  "success": true,
  "message": "Pedido ORD-2024-001 cancelado exitosamente",
  "order": {
    /* DTO del pedido */
  },
  "stockInfo": "✅ Stock devuelto automáticamente para todos los items (3 items)"
}
```

### Cancelación con Error

```json
{
  "success": false,
  "message": "Los meseros solo pueden cancelar pedidos en estado PENDIENTE. Este pedido está en estado: En Preparación"
}
```

---

## 🧪 Casos de Prueba

### Caso 1: Pedido PENDING con items sin preparación

```
Estado pedido: PENDING
Items:
  - Coca Cola (requiresPreparation = FALSE, itemStatus = READY)
  - Ensalada César (requiresPreparation = FALSE, itemStatus = READY)

Resultado esperado:
✅ Stock devuelto automáticamente para todos los items (2 items)
```

---

### Caso 2: Pedido IN_PREPARATION con items mixtos

```
Estado pedido: IN_PREPARATION
Items:
  - Coca Cola (requiresPreparation = FALSE, itemStatus = READY)
  - Pizza (requiresPreparation = TRUE, itemStatus = IN_PREPARATION)
  - Hamburguesa (requiresPreparation = TRUE, itemStatus = IN_PREPARATION)

Resultado esperado:
ℹ️ Stock devuelto: 1 items automáticos, 2 items requieren devolución manual
```

---

### Caso 3: Pedido READY con items preparados por chef

```
Estado pedido: READY
Items:
  - Lasaña (requiresPreparation = TRUE, itemStatus = READY)
  - Risotto (requiresPreparation = TRUE, itemStatus = READY)

Resultado esperado:
⚠️ Stock debe ser devuelto manualmente para todos los items (2 items)
```

---

### Caso 4: Pedido PENDING recién creado

```
Estado pedido: PENDING
Items:
  - Pasta (requiresPreparation = TRUE, itemStatus = PENDING)
  - Sopa (requiresPreparation = TRUE, itemStatus = PENDING)

Resultado esperado:
✅ Stock devuelto automáticamente para todos los items (2 items)
```

---

## ✅ Beneficios de la Implementación

### 1. **Precisión**

- Análisis item por item vs estado del pedido completo
- Información basada en el estado real de cada ingrediente

### 2. **Transparencia**

- Usuario sabe exactamente qué pasará con el stock
- Mensajes claros con conteo de items

### 3. **Flexibilidad**

- Maneja escenarios mixtos (algunos items auto, otros manual)
- Adaptable a diferentes flujos de trabajo

### 4. **UX Mejorada**

- Mensaje genérico antes de cancelar (no abruma al usuario)
- Información detallada después de cancelar (cuando es relevante)
- Mayor tiempo de visualización (3s) para leer la información

---

## 🔄 Flujo Completo

```
1. Usuario hace clic en "Cancelar Pedido"
   ↓
2. Modal muestra mensaje genérico:
   "El sistema analizará cada item y devolverá el stock según corresponda"
   ↓
3. Usuario confirma cancelación
   ↓
4. Backend ejecuta cancelOrder()
   ↓
5. Backend ejecuta analyzeStockReturn()
   - Recorre cada OrderDetail
   - Clasifica como automático o manual
   - Construye mensaje apropiado
   ↓
6. Backend retorna JSON con stockInfo
   ↓
7. Frontend muestra modal de éxito con:
   - "Pedido cancelado exitosamente"
   - stockInfo detallado (ej: "✅ Stock devuelto automáticamente...")
   ↓
8. Modal se cierra después de 3 segundos (o al hacer clic en OK)
   ↓
9. Página se recarga automáticamente
```

---

## 📝 Notas Técnicas

### Estado de Items vs Estado de Pedido

- **Estado del Pedido:** Calculado automáticamente en base al estado de TODOS los items
- **Estado del Item:** Independiente, refleja el ciclo de vida individual
- **Análisis de Stock:** Se basa en estado individual de items, NO en estado del pedido

### Auto-avance a READY

- Items con `requiresPreparation = FALSE` se marcan automáticamente como `READY` al crear el pedido
- Esto permite que bebidas, ensaladas, etc. estén disponibles inmediatamente sin pasar por el chef

### Lógica de Devolución

- La lógica está **duplicada** en `OrderServiceImpl.shouldReturnStockAutomatically()` y en `analyzeStockReturn()`
- Esto garantiza consistencia entre la acción real (devolución de stock) y el mensaje al usuario

---

## 🎉 Resumen de Impacto

| Aspecto            | Antes                         | Después                          |
| ------------------ | ----------------------------- | -------------------------------- |
| **Precisión**      | Basada en estado del pedido   | Basada en análisis item por item |
| **Información**    | Genérica y a veces incorrecta | Detallada y precisa              |
| **UX**             | Modal complicado con bullets  | Modal simple + info después      |
| **Transparencia**  | Baja                          | Alta (muestra conteo exacto)     |
| **Mantenibilidad** | Lógica en frontend            | Lógica centralizada en backend   |

---

## 🚀 Implementación Completada

**Fecha:** 2024
**Estado:** ✅ COMPLETADO
**Archivos modificados:** 5 (3 frontend + 2 backend)
**Compilación:** ✅ Sin errores
**Testing:** Pendiente de pruebas en entorno de desarrollo

---

## 📌 Próximos Pasos Sugeridos

1. ✅ Testing en diferentes roles (Admin, Waiter, Cashier)
2. ✅ Testing con pedidos en diferentes estados
3. ✅ Verificar comportamiento con items mixtos
4. ✅ Validar mensajes de error cuando no se puede cancelar
5. ✅ Testing de UX (legibilidad, tiempo de visualización)

---

## 🔗 Documentos Relacionados

- `FEATURE_REQUIRES_PREPARATION.md` - Implementación del campo requiresPreparation
- `CHEF_FILTERING_LOGIC_COMPLETE.md` - Filtrado de items para el chef
- `ORDER_STATUS_WORKFLOW.md` - Flujo completo de estados de pedido
- `REFACTORING_CASHIER_CONSOLIDATION.md` - Unificación de servicios de cajero

---

**Fin del Documento**
