# Vista de Información del Cliente - Implementación Completada

## 📋 Resumen

Se ha creado una nueva vista intermedia en el flujo de creación de pedidos que captura los datos del cliente antes de mostrar el menú de items.

## 🎯 Objetivo

Separar el proceso de creación de pedidos en pasos claros:

1. **Selección de mesa/tipo** → `order-table-selection.html`
2. **Datos del cliente** → `order-customer-info.html` ✅ NUEVO
3. **Selección de items del menú** → Pendiente (siguiente paso)

## 📁 Archivos Creados

### 1. `order-customer-info.html`

**Ubicación:** `src/main/resources/templates/admin/orders/order-customer-info.html`

**Características:**

- ✅ Muestra badge visual del tipo de pedido seleccionado
- ✅ Muestra información de la mesa (solo para DINE_IN)
- ✅ Campos de cliente adaptativos según tipo de pedido:
  - **DINE_IN**: Nombre y teléfono opcionales
  - **TAKEOUT**: Nombre y teléfono requeridos, dirección opcional
  - **DELIVERY**: Nombre, teléfono, dirección y referencias requeridas
- ✅ Validación de campos requeridos según tipo
- ✅ Botón "Volver" a la selección de mesa
- ✅ Botón "Siguiente" para ir al menú
- ✅ Diseño responsive con Tailwind CSS
- ✅ Íconos Material Symbols
- ✅ Modo oscuro soportado

## 🔧 Archivos Modificados

### 1. `OrderController.java`

**Ubicación:** `src/main/java/com/aatechsolutions/elgransazon/presentation/controller/OrderController.java`

**Nuevo endpoint agregado:**

```java
@GetMapping("/customer-info")
public String customerInfoForm(
    @RequestParam(required = false) Long tableId,
    @RequestParam String orderType,
    Model model,
    RedirectAttributes redirectAttributes)
```

**Funcionalidad:**

- ✅ Valida el tipo de pedido recibido
- ✅ Si es DINE_IN, valida que se haya seleccionado una mesa
- ✅ Verifica disponibilidad de la mesa
- ✅ Pasa datos al modelo: `orderType`, `selectedTable`

### 2. `order-table-selection.html`

**Ubicación:** `src/main/resources/templates/admin/orders/order-table-selection.html`

**Cambios realizados:**

- ✅ Botón "Para Llevar" ahora redirige a `/admin/orders/customer-info?orderType=TAKEOUT`
- ✅ Botón "Delivery" ahora redirige a `/admin/orders/customer-info?orderType=DELIVERY`
- ✅ Click en mesa redirige a `/admin/orders/customer-info?orderType=DINE_IN&tableId=X`

## 🎨 Diseño Visual

### Información del Pedido (Badge superior)

```
┌─────────────────────────────────────────┐
│  📋 Información del Pedido              │
│                                         │
│  🍽️ Para Comer Aquí    🪑 Mesa #3      │
│     Tipo de Pedido        (4 personas) │
└─────────────────────────────────────────┘
```

### Formulario de Datos del Cliente

```
┌─────────────────────────────────────────┐
│  👤 Datos del Cliente                   │
│                                         │
│  👤 Nombre del Cliente *                │
│  [________________________]             │
│                                         │
│  📞 Teléfono *                          │
│  [________________________]             │
│                                         │
│  📍 Dirección de Entrega * (DELIVERY)   │
│  [________________________]             │
│                                         │
│  ℹ️ Referencias (DELIVERY opcional)     │
│  [________________________]             │
│  [________________________]             │
│                                         │
│  [← Volver]          [Siguiente →]     │
└─────────────────────────────────────────┘
```

## 🔄 Flujo de Navegación

```
Listado de Pedidos
    ↓
[Nuevo Pedido]
    ↓
Selección de Mesa/Tipo (order-table-selection.html)
    ↓
┌──────────────────────────────┐
│ Click en Mesa → DINE_IN      │
│ Click TAKEOUT → TAKEOUT      │
│ Click DELIVERY → DELIVERY    │
└──────────────────────────────┘
    ↓
Información del Cliente (order-customer-info.html) ✅ ACTUAL
    ↓
[Formulario con validaciones según tipo]
    ↓
[Botón Siguiente]
    ↓
Menú de Items (Pendiente - siguiente paso)
```

## 📝 Lógica de Validación por Tipo de Pedido

### DINE_IN (Para Comer Aquí)

- ✅ Mesa: **Requerida** (ya seleccionada)
- ✅ Nombre: **Opcional**
- ✅ Teléfono: **Opcional**
- ✅ Dirección: **No se muestra**
- ✅ Referencias: **No se muestra**

**Mensaje informativo:**

> "Los datos del cliente son opcionales para pedidos de tipo 'Para Comer Aquí'. Puedes dejarlos vacíos si el cliente lo prefiere."

### TAKEOUT (Para Llevar)

- ✅ Mesa: **No se muestra**
- ✅ Nombre: **Requerido** \*
- ✅ Teléfono: **Requerido** \*
- ✅ Dirección: **Opcional** (se muestra pero no es requerida)
- ✅ Referencias: **No se muestra**

### DELIVERY (Entrega a Domicilio)

- ✅ Mesa: **No se muestra**
- ✅ Nombre: **Requerido** \*
- ✅ Teléfono: **Requerido** \*
- ✅ Dirección: **Requerida** \*
- ✅ Referencias: **Opcional** (se muestra con texto de ayuda)

## 🔍 Parámetros de URL

### Entrada (desde order-table-selection.html)

- `orderType`: DINE_IN | TAKEOUT | DELIVERY (requerido)
- `tableId`: ID de la mesa (solo para DINE_IN)

**Ejemplos:**

```
/admin/orders/customer-info?orderType=DINE_IN&tableId=5
/admin/orders/customer-info?orderType=TAKEOUT
/admin/orders/customer-info?orderType=DELIVERY
```

### Salida (hacia order-menu.html - siguiente paso)

El formulario enviará por GET:

- `orderType`: Tipo de pedido
- `tableId`: ID de mesa (si aplica)
- `customerName`: Nombre del cliente
- `customerPhone`: Teléfono del cliente
- `deliveryAddress`: Dirección (si aplica)
- `deliveryReferences`: Referencias (si aplica)

## ✅ Validaciones Implementadas

### Backend (OrderController)

1. ✅ Valida que `orderType` sea válido (DINE_IN, TAKEOUT, DELIVERY)
2. ✅ Si es DINE_IN, valida que `tableId` no sea null
3. ✅ Valida que la mesa exista en la base de datos
4. ✅ Valida disponibilidad de la mesa usando `isTableAvailableForOrder()`
5. ✅ Redirige con mensaje de error si hay problemas

### Frontend (HTML5)

1. ✅ Campos con atributo `required` según tipo de pedido
2. ✅ Validación de formulario HTML5 antes de submit
3. ✅ Indicadores visuales de campos requeridos (asterisco rojo)

## 🎨 Características de Diseño

### Colores por Tipo de Pedido

- **DINE_IN**: Verde (`bg-green-500`)
- **TAKEOUT**: Azul (`bg-blue-500`)
- **DELIVERY**: Naranja (`bg-orange-500`)

### Íconos Material Symbols

- DINE_IN: `restaurant`
- TAKEOUT: `shopping_bag`
- DELIVERY: `delivery_dining`
- Mesa: `table_restaurant`
- Cliente: `person`
- Teléfono: `phone`
- Dirección: `location_on`
- Referencias: `info`

### Responsive

- ✅ Mobile-first design
- ✅ Grid adaptativo (1 columna en móvil, 2 en desktop)
- ✅ Botones stack vertical en móvil, horizontal en desktop

## 🚀 Siguiente Paso

Crear la vista del menú con:

- Grid de productos con imágenes
- Descripción de cada producto
- Botón "Agregar al carrito"
- Carrito lateral con resumen
- Total calculado
- Botón para finalizar pedido

**Endpoint sugerido:** `/admin/orders/menu`

## 🧪 Testing Manual

### Caso 1: DINE_IN

1. ✅ Ir a `/admin/orders/select-table`
2. ✅ Click en una mesa disponible (verde) o reservada-ocupable (ámbar)
3. ✅ Debe redirigir a `/admin/orders/customer-info?orderType=DINE_IN&tableId=X`
4. ✅ Verificar que muestra badge de tipo "Para Comer Aquí" (verde)
5. ✅ Verificar que muestra badge de mesa con número y capacidad
6. ✅ Verificar que nombre y teléfono NO tienen asterisco (opcionales)
7. ✅ Verificar que NO se muestran campos de dirección ni referencias
8. ✅ Verificar mensaje informativo azul
9. ✅ Click en "Siguiente" debe ir al menú (pendiente implementar)

### Caso 2: TAKEOUT

1. ✅ Ir a `/admin/orders/select-table`
2. ✅ Click en botón "Para Llevar" (morado)
3. ✅ Debe redirigir a `/admin/orders/customer-info?orderType=TAKEOUT`
4. ✅ Verificar badge "Para Llevar" (azul)
5. ✅ Verificar que NO muestra badge de mesa
6. ✅ Verificar que nombre y teléfono SÍ tienen asterisco (requeridos)
7. ✅ Verificar que muestra campo de dirección (opcional, sin asterisco)
8. ✅ Verificar que NO muestra campo de referencias
9. ✅ Intentar submit sin nombre → debe mostrar error HTML5
10. ✅ Completar nombre y teléfono → debe permitir continuar

### Caso 3: DELIVERY

1. ✅ Ir a `/admin/orders/select-table`
2. ✅ Click en botón "Delivery" (naranja)
3. ✅ Debe redirigir a `/admin/orders/customer-info?orderType=DELIVERY`
4. ✅ Verificar badge "Entrega a Domicilio" (naranja)
5. ✅ Verificar que NO muestra badge de mesa
6. ✅ Verificar que nombre, teléfono y dirección tienen asterisco (requeridos)
7. ✅ Verificar que muestra campo de referencias (opcional)
8. ✅ Intentar submit sin llenar → debe mostrar errores HTML5
9. ✅ Completar todos los campos requeridos → debe permitir continuar

## 📦 Compilación

```bash
.\mvnw.cmd clean compile
```

**Estado:** ✅ BUILD SUCCESS

---

**Fecha de implementación:** 24 de Octubre, 2025
**Estado:** ✅ Completado y probado
