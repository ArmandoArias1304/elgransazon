# Flujo de Estados de Pedidos - Sistema de Gestión de Restaurante

## 📋 Resumen

Este documento explica el flujo completo de estados de pedidos, las reglas de negocio implementadas y cómo funciona la cancelación automática/manual de stock.

## 🎯 Estados Disponibles

### Para DINE_IN (Para comer aquí) y TAKEOUT (Para llevar)

```
PENDING → IN_PREPARATION → READY → DELIVERED → PAID
            ↓                ↓        ↓
        CANCELLED        CANCELLED  ❌ No se puede cancelar
```

**Estados:**

- **PENDING** (Pendiente): Pedido recién creado
- **IN_PREPARATION** (En preparación): Pedido en la cocina
- **READY** (Listo): Pedido listo para servir/entregar
- **DELIVERED** (Entregado): Pedido entregado al cliente
- **PAID** (Pagado): Pedido pagado
- **CANCELLED** (Cancelado): Pedido cancelado

### Para DELIVERY (Entrega a domicilio)

```
PENDING → IN_PREPARATION → READY → ON_THE_WAY → DELIVERED → PAID
            ↓                ↓          ↓           ↓
        CANCELLED        CANCELLED  ❌ No se puede cancelar
```

**Estados adicionales:**

- **ON_THE_WAY** (En camino): Pedido en ruta de entrega (solo para DELIVERY)

## 🔄 Reglas de Transición de Estados

### PENDING → IN_PREPARATION

- ✅ Permitido para todos los tipos de pedido
- 📦 Stock ya descontado al crear el pedido

### IN_PREPARATION → READY

- ✅ Permitido para todos los tipos de pedido
- 👨‍🍳 Indica que la cocina terminó de preparar

### READY → DELIVERED (para DINE_IN y TAKEOUT)

- ✅ Permitido
- 🍽️ Pedido servido al cliente (DINE_IN) o recogido (TAKEOUT)

### READY → ON_THE_WAY (solo para DELIVERY)

- ✅ Permitido únicamente para pedidos DELIVERY
- 🚗 Repartidor en camino
- ❌ ERROR si se intenta con DINE_IN o TAKEOUT

### ON_THE_WAY → DELIVERED (solo para DELIVERY)

- ✅ Permitido
- 📍 Pedido entregado en el domicilio

### DELIVERED → PAID

- ✅ Permitido para todos los tipos
- 💰 Cliente realizó el pago
- 🪑 **IMPORTANTE**: Si es DINE_IN, la mesa se libera automáticamente

### Cualquier estado → CANCELLED

- ✅ Permitido desde: PENDING, IN_PREPARATION, READY
- ❌ NO permitido desde: ON_THE_WAY, DELIVERED, PAID, CANCELLED

## ❌ Reglas de Cancelación

### 1. Cancelación desde PENDING

```java
Estado: PENDING
Acción: Cancelar pedido
Resultado:
  - ✅ Stock DEVUELTO AUTOMÁTICAMENTE
  - ✅ Mesa liberada (si es DINE_IN)
  - ✅ Estado cambia a CANCELLED
```

**Motivo:** El pedido aún no ha sido preparado, todos los ingredientes están intactos.

### 2. Cancelación desde IN_PREPARATION

```java
Estado: IN_PREPARATION
Acción: Cancelar pedido
Resultado:
  - ⚠️ Stock NO se devuelve automáticamente
  - ✅ Mesa liberada (si es DINE_IN)
  - ✅ Estado cambia a CANCELLED
  - 📝 Log: "Stock must be returned MANUALLY"
```

**Motivo:** El pedido puede estar parcialmente preparado. Algunos ingredientes ya se usaron, otros no. El administrador debe revisar qué ingredientes se pueden recuperar y devolverlos manualmente al inventario.

### 3. Cancelación desde READY

```java
Estado: READY
Acción: Cancelar pedido
Resultado:
  - ⚠️ Stock NO se devuelve automáticamente
  - ✅ Mesa liberada (si es DINE_IN)
  - ✅ Estado cambia a CANCELLED
  - 📝 Log: "Stock must be returned MANUALLY"
```

**Motivo:** El pedido ya está preparado. El administrador debe decidir si el plato se puede reutilizar, guardar o desechar.

### 4. Cancelación NO PERMITIDA

```java
Estados: ON_THE_WAY, DELIVERED, PAID
Acción: Intentar cancelar
Resultado:
  - ❌ ERROR: "No se puede cancelar un pedido con estado: [estado]"
  - ❌ El botón de cancelar NO aparece en la interfaz
```

**Motivo:**

- **ON_THE_WAY**: El repartidor ya salió con el pedido
- **DELIVERED**: El pedido ya fue entregado al cliente
- **PAID**: El pedido ya fue pagado, transacción completa

## 🪑 Manejo de Mesas (Solo para DINE_IN)

### Al Crear Pedido DINE_IN

**Mesa AVAILABLE:**

```
Estado inicial: AVAILABLE, is_occupied=false
Acción: Crear pedido DINE_IN
Resultado: OCCUPIED, is_occupied=false
```

**Mesa RESERVED:**

```
Estado inicial: RESERVED, is_occupied=false
Validación: Tiempo suficiente antes de próxima reservación
Acción: Crear pedido DINE_IN
Resultado: RESERVED, is_occupied=true
```

### Al Cancelar Pedido DINE_IN

**Mesa OCCUPIED:**

```
Acción: Cancelar pedido
Resultado: AVAILABLE, is_occupied=false
```

**Mesa RESERVED:**

```
Acción: Cancelar pedido
Resultado: RESERVED, is_occupied=false
(Sigue reservada, solo libera la ocupación física)
```

### Al Completar Pedido (DELIVERED o PAID)

**Mesa OCCUPIED:**

```
Acción: Cambiar a DELIVERED/PAID
Resultado: AVAILABLE, is_occupied=false
```

**Mesa RESERVED:**

```
Acción: Cambiar a DELIVERED/PAID
Resultado: RESERVED, is_occupied=false
```

## 🖥️ Interfaz de Usuario

### Botón "Cambiar Estado"

**Funcionalidad:**

1. Carga dinámicamente los estados válidos desde el backend
2. Muestra modal con dropdown de estados permitidos
3. Valida la transición en el backend
4. Actualiza la base de datos
5. Recarga la página mostrando el nuevo estado

**Disponible cuando:**

- Estado NO es CANCELLED
- Estado NO es PAID

### Botón "Cancelar Pedido"

**Funcionalidad:**

1. Muestra confirmación con advertencias según el estado
2. Valida si la cancelación es permitida en el backend
3. Devuelve stock automáticamente si es PENDING
4. Libera mesa si es DINE_IN
5. Muestra advertencia si el stock debe ser devuelto manualmente

**Disponible cuando:**

- Estado es PENDING, IN_PREPARATION, o READY
- NO disponible para ON_THE_WAY, DELIVERED, PAID, CANCELLED

**Mensajes de advertencia:**

```
PENDING:
  "El stock será devuelto automáticamente"

IN_PREPARATION o READY:
  "⚠️ Los ingredientes deben ser devueltos manualmente al inventario"
```

## 📊 Ejemplo de Flujo Completo

### Pedido DINE_IN - Mesa 5

```
1. Crear pedido (Mesa 5 AVAILABLE)
   → Estado: PENDING
   → Mesa: OCCUPIED, is_occupied=false
   → Stock: Descontado automáticamente

2. Iniciar preparación
   → Estado: IN_PREPARATION
   → Mesa: OCCUPIED, is_occupied=false

3. Terminar preparación
   → Estado: READY
   → Mesa: OCCUPIED, is_occupied=false

4. Servir al cliente
   → Estado: DELIVERED
   → Mesa: AVAILABLE, is_occupied=false

5. Cliente paga
   → Estado: PAID
   → Mesa: AVAILABLE, is_occupied=false
```

### Pedido DELIVERY - Cancelación en IN_PREPARATION

```
1. Crear pedido DELIVERY
   → Estado: PENDING
   → Stock: Descontado

2. Iniciar preparación
   → Estado: IN_PREPARATION
   → Stock: Algunos ingredientes en uso

3. Cliente cancela
   → Estado: CANCELLED
   → Stock: NO devuelto automáticamente
   → Warning: "Los ingredientes deben ser devueltos manualmente"
   → Acción manual: Revisar qué ingredientes se pueden recuperar
```

## 🔧 Endpoints API

### POST `/admin/orders/{id}/change-status`

**Parámetros:**

- `newStatus`: Estado al que se quiere cambiar

**Validaciones:**

- Transición válida según el tipo de pedido
- Estado actual permite cambios
- ON_THE_WAY solo para DELIVERY

**Respuesta:**

```json
{
  "success": true,
  "message": "Estado del pedido cambiado a En Preparación",
  "order": {
    "id": 123,
    "orderNumber": "ORD-20251024-001",
    "status": "IN_PREPARATION",
    "statusLabel": "En Preparación",
    ...
  }
}
```

### POST `/admin/orders/{id}/cancel`

**Validaciones:**

- Estado permite cancelación
- No está en ON_THE_WAY, DELIVERED, PAID

**Respuesta:**

```json
{
  "success": true,
  "message": "Pedido ORD-20251024-001 cancelado exitosamente",
  "warning": "Los ingredientes deben ser devueltos manualmente al inventario",
  "order": {...}
}
```

### GET `/admin/orders/{id}/valid-statuses`

**Respuesta:**

```json
{
  "success": true,
  "currentStatus": "READY",
  "currentStatusLabel": "Listo",
  "orderType": "DELIVERY",
  "validStatuses": [{ "value": "ON_THE_WAY", "label": "En Camino" }],
  "canBeCancelled": true
}
```

## ✅ Testing Checklist

- [ ] Crear pedido DINE_IN y verificar mesa OCCUPIED
- [ ] Cambiar estado de PENDING a IN_PREPARATION
- [ ] Cambiar estado de IN_PREPARATION a READY
- [ ] Cambiar estado de READY a DELIVERED (DINE_IN)
- [ ] Verificar que mesa se libera al DELIVERED
- [ ] Crear pedido DELIVERY y cambiar a ON_THE_WAY
- [ ] Intentar poner ON_THE_WAY en pedido TAKEOUT (debe fallar)
- [ ] Cancelar pedido PENDING y verificar stock devuelto
- [ ] Cancelar pedido IN_PREPARATION y verificar advertencia
- [ ] Intentar cancelar pedido DELIVERED (debe fallar)
- [ ] Verificar que botones aparecen/desaparecen según estado

---

**Última actualización:** 24 de octubre de 2025  
**Autor:** Sistema de Gestión de Restaurante
