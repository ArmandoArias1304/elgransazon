# 🌓 Sistema de Modo Claro/Oscuro - El Gran Sazón

## ✅ Implementación Completada

Se ha implementado un sistema de modo claro/oscuro que cambia **únicamente**:

- ✅ **Fondo blanco → Gris oscuro** (#1a1a1a, #242424, #2d2d2d)
- ✅ **Texto negro → Blanco** (#ffffff, #e0e0e0)
- ✅ **Mantiene todos los demás colores** (verde corporativo, estados de pedidos, mesas, etc.)

---

## 📁 Archivos Creados

### 1. **CSS Global del Sistema de Temas**

📄 `src/main/resources/static/css/theme.css`

Define las variables CSS que controlan los colores de fondo y texto. Contiene:

- Variables CSS para modo claro y oscuro
- Clases utilitarias para aplicar el tema
- Estilos del botón de toggle
- Transiciones suaves entre modos

### 2. **JavaScript del Toggle**

📄 `src/main/resources/static/js/theme-toggle.js`

Maneja la lógica del cambio de tema:

- Toggle entre modo claro y oscuro
- Guarda la preferencia en `localStorage`
- Aplica automáticamente el tema guardado al cargar la página

### 3. **Fragmento HTML Reutilizable**

📄 `src/main/resources/templates/fragments/theme.html`

Contiene fragmentos Thymeleaf para incluir en las páginas:

- `themeResources`: Incluye CSS y JS del sistema
- `themeToggleButtonMaterial`: Botón con Material Icons (⚡ recomendado)
- `themeToggleButton`: Botón con Font Awesome
- `themeToggleButtonText`: Botón con texto y emojis

---

## 🚀 Cómo Aplicar en Cada Página HTML

### Paso 1: Incluir los Recursos en el `<head>`

Agrega esta línea dentro del `<head>` de tu página HTML:

```html
<!-- Sistema de Temas (Modo Claro/Oscuro) -->
<div th:replace="~{fragments/theme :: themeResources}"></div>
```

**Ejemplo:**

```html
<head>
  <meta charset="utf-8" />
  <title>Mi Página</title>

  <!-- Otros links y scripts -->

  <!-- Sistema de Temas (Modo Claro/Oscuro) -->
  <div th:replace="~{fragments/theme :: themeResources}"></div>
</head>
```

### Paso 2: Incluir el Botón de Toggle en el `<body>`

Agrega esta línea al inicio del `<body>`:

```html
<!-- Botón de Toggle Modo Claro/Oscuro -->
<div th:replace="~{fragments/theme :: themeToggleButtonMaterial}"></div>
```

**Ejemplo:**

```html
<body>
  <!-- Botón de Toggle Modo Claro/Oscuro -->
  <div th:replace="~{fragments/theme :: themeToggleButtonMaterial}"></div>

  <!-- Resto del contenido -->
</body>
```

### Paso 3 (OPCIONAL): Usar Clases Utilitarias

Si quieres forzar que un elemento específico use los colores del tema, puedes usar estas clases:

```html
<!-- Fondos -->
<div class="bg-primary-theme">Fondo principal</div>
<div class="bg-secondary-theme">Fondo secundario</div>
<div class="bg-card-theme">Fondo de card</div>

<!-- Textos -->
<p class="text-primary-theme">Texto principal</p>
<p class="text-secondary-theme">Texto secundario</p>
<p class="text-muted-theme">Texto atenuado</p>

<!-- Bordes y sombras -->
<div class="border-theme shadow-theme">Con borde y sombra del tema</div>
```

---

## 📋 Páginas a Actualizar

Debes agregar las 2 líneas (recursos + botón) en cada archivo HTML:

### Admin

- ✅ `admin/dashboard.html` (Ya actualizado como ejemplo)
- ⬜ `admin/tables.html`
- ⬜ `admin/reservations.html`
- ⬜ `admin/reservations/list.html`
- ⬜ `admin/reservations/form.html`
- ⬜ `admin/tables/list.html`
- ⬜ `admin/tables/form.html`
- ⬜ `admin/suppliers/list.html`
- ⬜ `admin/suppliers/form.html`
- ⬜ `admin/Sales/list.html`
- ⬜ `admin/shifts/list.html`
- ⬜ `admin/shifts/form.html`
- ⬜ `admin/shifts/detail.html`
- ⬜ `admin/shifts/assign-employees.html`
- ⬜ `admin/system-configuration/form.html`
- ⬜ `admin/system-configuration/social-network-form.html`
- ⬜ Y todas las demás vistas de admin...

### Waiter

- ⬜ `waiter/dashboard.html`
- ⬜ `waiter/orders/list.html`
- ⬜ `waiter/orders/form.html`
- ⬜ `waiter/orders/view.html`
- ⬜ `waiter/orders/order-menu.html`
- ⬜ `waiter/menu/view.html`
- ⬜ `waiter/payments/form.html`
- ⬜ `waiter/profile/view.html`
- ⬜ `waiter/reports/view.html`
- ⬜ `waiter/ranking/view.html`
- ⬜ `waiter/tip/view.html`

### Chef

- ⬜ `chef/dashboard.html`
- ⬜ `chef/orders/pending.html`
- ⬜ `chef/orders/my-orders.html`
- ⬜ `chef/menu/view.html`
- ⬜ `chef/profile/view.html`
- ⬜ `chef/reports/view.html`
- ⬜ `chef/ranking/view.html`

### Cashier

- ⬜ `cashier/dashboard.html`
- ⬜ `cashier/orders/list.html`
- ⬜ `cashier/orders/view.html`
- ⬜ `cashier/orders/order-menu.html`
- ⬜ `cashier/payments/form.html`
- ⬜ `cashier/profile/view.html`
- ⬜ `cashier/reports/view.html`

### Delivery

- ⬜ `delivery/dashboard.html`
- ⬜ `delivery/orders/pending.html`
- ⬜ `delivery/orders/completed.html`
- ⬜ `delivery/payments/form.html`
- ⬜ `delivery/profile/view.html`
- ⬜ `delivery/reports/view.html`
- ⬜ `delivery/tip/view.html`

### Home y Errores

- ⬜ `home.html`
- ⬜ `home/landing.html`
- ⬜ `errores/400.html`
- ⬜ `errores/401.html`
- ⬜ `errores/403.html`
- ⬜ `errores/404.html`
- ⬜ `errores/408.html`
- ⬜ `errores/500.html`
- ⬜ `errores/503.html`

---

## 🎨 Variaciones del Botón

### Opción 1: Material Icons (Recomendado - ya tienes Material Icons)

```html
<div th:replace="~{fragments/theme :: themeToggleButtonMaterial}"></div>
```

✅ Usa los iconos que ya tienes en tu proyecto

### Opción 2: Font Awesome

```html
<div th:replace="~{fragments/theme :: themeToggleButton}"></div>
```

⚠️ Requiere tener Font Awesome incluido

### Opción 3: Solo Texto/Emojis

```html
<div th:replace="~{fragments/theme :: themeToggleButtonText}"></div>
```

🌞 Muestra "🌞 Claro" o "🌙 Oscuro"

---

## 🎯 Elementos que Cambian Automáticamente

El sistema aplica automáticamente el modo oscuro a:

- ✅ `<body>` - Fondo y color de texto
- ✅ `.card` - Tarjetas
- ✅ `.modal-content` - Modales
- ✅ `.dropdown-menu` - Menús desplegables
- ✅ `input`, `textarea`, `select` - Formularios
- ✅ `table`, `th`, `tr` - Tablas
- ✅ Placeholders de inputs

**No necesitas modificar el CSS de cada página para estos elementos.**

---

## 🔧 Personalización Avanzada

### Cambiar Colores del Modo Oscuro

Edita `src/main/resources/static/css/theme.css`:

```css
body.dark-mode {
  /* Cambia estos valores según prefieras */
  --bg-primary: #1a1a1a; /* Fondo principal */
  --bg-secondary: #2d2d2d; /* Fondo secundario */
  --bg-card: #242424; /* Fondo de cards */

  --text-primary: #ffffff; /* Texto principal */
  --text-secondary: #e0e0e0; /* Texto secundario */
  --text-muted: #9e9e9e; /* Texto atenuado */
}
```

### Agregar Más Elementos Específicos

Si necesitas que un elemento específico cambie en modo oscuro:

```css
body.dark-mode .tu-clase-especial {
  background-color: var(--bg-card);
  color: var(--text-primary);
}
```

---

## 🧪 Cómo Probar

1. **Abre cualquier página actualizada**
2. **Verás un botón flotante** en la esquina superior derecha
3. **Haz clic en el botón** para alternar entre modos
4. **Recarga la página** - el modo se mantiene guardado
5. **Verifica que**:
   - ✅ Los fondos blancos cambian a gris oscuro
   - ✅ El texto negro cambia a blanco
   - ✅ El verde corporativo NO cambia
   - ✅ Los colores de estados NO cambian

---

## 🐛 Solución de Problemas

### El botón no aparece

- Verifica que incluiste `themeResources` en el `<head>`
- Verifica que incluiste el fragmento del botón en el `<body>`
- Revisa la consola del navegador por errores

### Los colores no cambian

- Asegúrate de que los archivos CSS/JS están en las carpetas correctas
- Limpia la caché del navegador (Ctrl + Shift + R)
- Verifica que Spring Boot está sirviendo los archivos estáticos

### El tema no se guarda

- Verifica que `localStorage` está habilitado en el navegador
- Abre las DevTools → Application → Local Storage
- Busca la clave `elgransazon-theme`

---

## 💡 Uso Programático

Si necesitas cambiar el tema desde JavaScript:

```javascript
// Cambiar a modo oscuro
window.themeToggle.setTheme("dark");

// Cambiar a modo claro
window.themeToggle.setTheme("light");

// Alternar entre modos
window.themeToggle.toggle();

// Obtener el tema actual
const currentTheme = window.themeToggle.getTheme(); // 'dark' o 'light'
```

### Escuchar Cambios de Tema

```javascript
document.addEventListener("themeChanged", function (event) {
  console.log("Nuevo tema:", event.detail.theme);
  // Tu código aquí
});
```

---

## ✨ Características

- ✅ **Cambio instantáneo** - Sin recargar la página
- ✅ **Persistente** - Se guarda en localStorage
- ✅ **Transiciones suaves** - Cambio animado de colores
- ✅ **Automático** - Aplica el tema al cargar
- ✅ **Reutilizable** - Fragmentos Thymeleaf
- ✅ **Ligero** - Solo ~200 líneas de código
- ✅ **Accesible** - Botón con aria-label y title

---

## 📝 Notas Importantes

1. **No modificar colores corporativos**: El verde (#38e07b) y los colores de estados se mantienen igual en ambos modos.

2. **Tailwind CSS**: Tu proyecto usa Tailwind, que ya tiene soporte para modo oscuro con `dark:`. Nuestro sistema es compatible y complementario.

3. **Aplicación progresiva**: Puedes ir agregando el sistema página por página. No necesitas actualizar todo a la vez.

4. **Posición del botón**: Por defecto está en la esquina superior derecha. Puedes cambiar la posición editando `.theme-toggle-btn` en `theme.css`.

---

## 🎉 ¡Listo!

Ya tienes todo el sistema implementado. Solo necesitas:

1. Copiar las 2 líneas en cada página HTML
2. Guardar y recargar
3. ¡Disfrutar del modo oscuro!

---

**Ejemplo Completo (admin/dashboard.html ya actualizado):**

```html
<!DOCTYPE html>
<html lang="es">
  <head>
    <!-- ... otros links y scripts ... -->

    <!-- Sistema de Temas (Modo Claro/Oscuro) -->
    <div th:replace="~{fragments/theme :: themeResources}"></div>
  </head>

  <body>
    <!-- Botón de Toggle Modo Claro/Oscuro -->
    <div th:replace="~{fragments/theme :: themeToggleButtonMaterial}"></div>

    <!-- ... resto del contenido ... -->
  </body>
</html>
```

---

**¿Necesitas ayuda?** Cualquier duda sobre la implementación, revisa este documento o consulta los archivos de ejemplo.
