# Feature: Badges de Promoción en Items del Menú

## 📋 Descripción

Se agregaron badges visuales en cada item del menú para indicar si tiene promociones activas.

## ✨ Características

### Lógica del Badge

- **1 promoción**: Muestra el tipo de promoción (ej: "2x1", "-30%", "-$5000")
- **Más de 1 promoción**: Muestra el número de promociones (ej: "2 Promociones", "3 Promociones")
- **Sin promoción**: No se muestra badge

### Diseño

- **Color**: Gradiente ámbar-naranja (from-amber-500 to-orange-500)
- **Icono**: 🏷️ emoji de etiqueta de precio
- **Texto**: Fuente bold, tamaño xs
- **Estilo**: Badge redondeado con sombra
- **Ubicación**: Debajo del precio del item

## 🔧 Implementación Técnica

### Archivos Modificados

1. **admin/orders/order-menu.html**
2. **waiter/orders/order-menu.html**
3. **cashier/orders/order-menu.html**

### Componentes Agregados

#### 1. Badge HTML (en cada tarjeta de item)

```html
<!-- Promotion Badge -->
<div
  class="promotion-badge-indicator"
  th:data-item-id="${item.idItemMenu}"
  style="display: none;"
>
  <div
    class="mt-1 inline-flex items-center gap-1 bg-gradient-to-r from-amber-500 to-orange-500 text-white text-xs font-bold px-2 py-0.5 rounded-full shadow-md"
  >
    <span>🏷️</span>
    <span class="promo-text">Promoción</span>
  </div>
</div>
```

#### 2. Función JavaScript `updatePromotionBadges()`

```javascript
function updatePromotionBadges() {
  document.querySelectorAll(".promotion-badge-indicator").forEach((badge) => {
    const itemId = parseInt(badge.getAttribute("data-item-id"));
    const itemPromotions = itemPromotionsMap.get(itemId) || [];

    if (itemPromotions.length > 0) {
      const promoText = badge.querySelector(".promo-text");

      if (itemPromotions.length === 1) {
        // Muestra el tipo de promoción
        const promo = itemPromotions[0];
        promoText.textContent = promo.displayLabel;
      } else {
        // Muestra el número de promociones
        promoText.textContent = `${itemPromotions.length} Promociones`;
      }

      badge.style.display = "block";
    }
  });
}
```

#### 3. Llamada Automática

La función se ejecuta automáticamente después de cargar las promociones:

```javascript
async function loadPromotions() {
  try {
    // ... carga promociones ...

    // Actualizar badges después de cargar
    updatePromotionBadges();
  } catch (error) {
    console.error("Error loading promotions:", error);
  }
}
```

## 📊 Ejemplos Visuales

### Item con 1 promoción

```
┌─────────────────────┐
│   [Imagen Item]     │
│                     │
│  Hamburguesa        │
│  $15.000            │
│  🏷️ 2x1             │ ← Badge muestra tipo
└─────────────────────┘
```

### Item con múltiples promociones

```
┌─────────────────────┐
│   [Imagen Item]     │
│                     │
│  Pizza Grande       │
│  $30.000            │
│  🏷️ 3 Promociones   │ ← Badge muestra cantidad
└─────────────────────┘
```

### Item sin promoción

```
┌─────────────────────┐
│   [Imagen Item]     │
│                     │
│  Ensalada César     │
│  $12.000            │
│                     │ ← No hay badge
└─────────────────────┘
```

## 🎯 Tipos de Promoción Soportados

### displayLabel de cada tipo

1. **BUY_X_PAY_Y** (Compra X Paga Y)
   - Ejemplo: "2x1", "3x2"
2. **PERCENTAGE_DISCOUNT** (Descuento Porcentual)
   - Ejemplo: "-30%", "-50%"
3. **FIXED_AMOUNT_DISCOUNT** (Descuento Fijo)
   - Ejemplo: "-$5000", "-$10000"

## 🔄 Flujo de Datos

1. **Carga de Página** → Se llama `loadPromotions()`
2. **Fetch API** → Obtiene promociones activas del servidor
3. **Map de Promociones** → Se construye `itemPromotionsMap` (itemId → [promociones])
4. **Update Badges** → Se llama `updatePromotionBadges()`
5. **DOM Update** → Se muestran/ocultan badges según corresponda

## ✅ Ventajas

1. **Visibilidad**: Los usuarios ven inmediatamente qué items tienen promociones
2. **Claridad**: Si hay una sola promoción, saben exactamente cuál es
3. **Múltiples Ofertas**: Si hay varias, saben cuántas opciones tienen
4. **UX Mejorada**: Incentiva a hacer clic para ver detalles de la promoción
5. **Responsive**: Funciona en todos los tamaños de pantalla
6. **Modo Oscuro**: Compatible con dark mode (colores cálidos visibles)

## 📱 Compatibilidad

- ✅ Desktop (Chrome, Firefox, Edge, Safari)
- ✅ Mobile (iOS Safari, Chrome Mobile)
- ✅ Tablet (iPad, Android)
- ✅ Modo Claro y Oscuro

## 🎨 Diseño del Badge

### Colores

- **Gradiente Base**: `from-amber-500` (#F59E0B) → `to-orange-500` (#F97316)
- **Texto**: Blanco (#FFFFFF)
- **Sombra**: `shadow-md` (medio contraste)

### Tipografía

- **Tamaño**: `text-xs` (0.75rem)
- **Peso**: `font-bold` (700)
- **Espaciado**: `gap-1` (0.25rem entre emoji y texto)

### Espaciado

- **Margen Superior**: `mt-1` (0.25rem)
- **Padding Horizontal**: `px-2` (0.5rem)
- **Padding Vertical**: `py-0.5` (0.125rem)
- **Border Radius**: `rounded-full` (999px)

## 🚀 Próximas Mejoras Sugeridas

1. **Animación de Entrada**: Fade-in cuando se carga el badge
2. **Hover Effect**: Tooltip con detalles al pasar mouse
3. **Destacado Visual**: Pulso/glow en items con promociones muy buenas
4. **Filtro por Promociones**: Botón para mostrar solo items con ofertas
5. **Badge de Urgencia**: Indicador si la promoción vence pronto

## 🐛 Troubleshooting

### Badge no aparece

- ✅ Verificar que `itemPromotionsMap` contenga el item
- ✅ Revisar consola: "Items with promotions: X"
- ✅ Confirmar que la promoción esté activa (fechas válidas)

### Texto del badge incorrecto

- ✅ Verificar `promo.displayLabel` en respuesta del API
- ✅ Revisar tipo de promoción en base de datos

### Badge aparece en item sin promoción

- ✅ Limpiar caché del navegador
- ✅ Verificar endpoint `/api/promotions/active`

## 📝 Notas de Implementación

- **Performance**: La función `updatePromotionBadges()` es eficiente (O(n) donde n = número de items)
- **Memory**: `itemPromotionsMap` se mantiene en memoria durante toda la sesión
- **Network**: Solo 1 request HTTP al cargar la página para obtener promociones
- **DOM Updates**: Mínimos (solo muestra/oculta y actualiza texto)

---

**Fecha de Implementación**: 11 de Noviembre, 2025
**Versión**: 1.0
**Roles Afectados**: Admin, Waiter, Cashier
