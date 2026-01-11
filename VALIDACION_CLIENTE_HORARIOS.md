# Validación de Horarios Laborables - Módulo Cliente

## Cambios Implementados

Se ha implementado la misma validación de horarios laborables para el módulo de **Clientes (ROLE_CLIENT)**, con una diferencia en la experiencia de usuario:

- **Backend**: Igual que admin/waiter/cashier → Rechaza operaciones fuera de horario
- **Frontend**: En lugar de deshabilitar, permite **ver el menú en modo vista** cuando está cerrado

---

## 1. Backend - ClientController.java

### Dependencia agregada (línea ~43):

```java
private final BusinessHoursService businessHoursService;
```

### Constructor actualizado (líneas ~45-66):

```java
public ClientController(
    @Qualifier("customerOrderService") OrderService orderService,
    ItemMenuService itemMenuService,
    CategoryService categoryService,
    SystemConfigurationService systemConfigurationService,
    CustomerService customerService,
    PromotionService promotionService,
    ReviewService reviewService,
    PasswordEncoder passwordEncoder,
    TicketPdfService ticketPdfService,
    BusinessHoursService businessHoursService) {  // ← NUEVO PARÁMETRO
    // ... inicialización
    this.businessHoursService = businessHoursService;
}
```

---

### Validación en `showDashboard()` (líneas ~87-92):

```java
// Check if restaurant is currently open
boolean isRestaurantOpen = businessHoursService.isOpenNow();
model.addAttribute("isRestaurantOpen", isRestaurantOpen);
log.debug("Restaurant is currently: {}", isRestaurantOpen ? "open" : "closed");

return "client/dashboard";
```

**Propósito**: Pasar estado del restaurante al dashboard para renderizado condicional.

---

### Nuevo método: `showMenuViewOnly()` (líneas ~107-143)

**Ruta**: `GET /client/view`

```java
@GetMapping("/view")
public String showMenuViewOnly(Authentication authentication, Model model) {
    log.debug("Customer {} accessing menu in view-only mode", authentication.getName());

    try {
        // Update item availability
        itemMenuService.updateAllItemsAvailability();

        // Get active categories and available items
        List<Category> categories = categoryService.getAllActiveCategories();
        List<ItemMenu> availableItems = itemMenuService.findAvailableItems();

        // Group items by category
        Map<Long, List<ItemMenu>> itemsByCategory = availableItems.stream()
                .collect(Collectors.groupingBy(item -> item.getCategory().getIdCategory()));

        // Get system configuration
        SystemConfiguration config = systemConfigurationService.getConfiguration();

        // Get customer info
        Customer customer = customerService.findByUsernameOrEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Cliente no encontrado"));

        model.addAttribute("config", config);
        model.addAttribute("categories", categories);
        model.addAttribute("itemsByCategory", itemsByCategory);
        model.addAttribute("currentRole", "client");
        model.addAttribute("customer", customer);

        return "client/view";

    } catch (Exception e) {
        log.error("Error showing view-only menu", e);
        model.addAttribute("errorMessage", "Error al cargar el menú: " + e.getMessage());
        return "error";
    }
}
```

**Características**:

- Carga menú completo (categorías e items)
- **NO** carga métodos de pago ni tipos de pedido
- Retorna vista `client/view.html` (solo lectura)
- Disponible **siempre** (incluso cuando está cerrado)

---

### Validación en `createOrder()` (líneas ~237-245):

**Ruta**: `POST /client/orders/create`

```java
try {
    // Validate restaurant is open
    if (!businessHoursService.isOpenNow()) {
        log.warn("Attempt to create order outside business hours by customer: {}", authentication.getName());
        return ResponseEntity.badRequest().body(Map.of(
            "success", false,
            "message", "No se puede crear el pedido. El restaurante no se encuentra en horario laborable en este momento."
        ));
    }

    // Get customer
    Customer customer = customerService.findByUsernameOrEmail(authentication.getName())
            .orElseThrow(() -> new IllegalStateException("Cliente no encontrado"));
    // ... continúa con la creación
}
```

**Protección**: Rechaza creación de pedidos fuera de horario (AJAX).

---

### Validación en `addItemsToOrder()` (líneas ~488-497):

**Ruta**: `POST /client/orders/{orderId}/add-items`

```java
try {
    // Validate restaurant is open
    if (!businessHoursService.isOpenNow()) {
        log.warn("Attempt to add items to order outside business hours by customer: {}", authentication.getName());
        return ResponseEntity.badRequest().body(Map.of(
            "success", false,
            "message", "No se pueden agregar items al pedido. El restaurante no se encuentra en horario laborable en este momento."
        ));
    }

    // ... continúa con agregar items
}
```

**Protección**: Rechaza agregar items a pedidos existentes fuera de horario (AJAX).

---

## 2. Frontend - client/dashboard.html

### Renderizado condicional del botón (líneas ~245-308):

#### Estado CERRADO → Botón "Ver Menú" (líneas ~246-280):

```html
<!-- Cuando el restaurante está CERRADO - Modo Vista -->
<a
  th:if="${!isRestaurantOpen}"
  href="/client/view"
  class="dashboard-card block rounded-3xl bg-white dark:bg-gray-900 shadow-lg hover:shadow-2xl border border-orange-300 dark:border-orange-700 p-6 sm:p-8 text-center animate-fadeInUp"
  style="animation-delay: 0.2s"
  title="El restaurante está cerrado pero puedes ver nuestro menú"
>
  <div class="flex flex-col items-center">
    <div class="mb-4">
      <span
        class="material-symbols-outlined text-6xl sm:text-7xl text-orange-500"
      >
        visibility
      </span>
    </div>
    <h3 class="text-lg sm:text-xl font-bold text-gray-900 dark:text-white mb-2">
      Ver Menú
    </h3>
    <div class="space-y-1">
      <p
        class="text-xs sm:text-sm text-orange-600 dark:text-orange-400 font-semibold"
      >
        🔒 Restaurante Cerrado
      </p>
      <p class="text-xs text-gray-500 dark:text-gray-400">
        Visualiza nuestro menú
      </p>
      <p class="text-xs text-gray-400 dark:text-gray-500">
        (Fuera de horario laborable)
      </p>
    </div>
  </div>
</a>
```

**Características visuales**:

- 🎨 **Icono**: `visibility` (ojo) en naranja
- 🟠 **Borde**: Naranja (`border-orange-300`)
- 📝 **Título**: "Ver Menú"
- 🔒 **Mensaje**: "Restaurante Cerrado"
- 📍 **Enlace**: `/client/view` (modo vista)
- ✨ **Hover**: Sigue teniendo efecto hover (es clickeable)

---

#### Estado ABIERTO → Botón "Crear Pedido" (líneas ~282-308):

```html
<!-- Cuando el restaurante está ABIERTO - Modo Pedido -->
<a
  th:if="${isRestaurantOpen}"
  href="/client/menu"
  class="dashboard-card block rounded-3xl bg-white dark:bg-gray-900 shadow-lg hover:shadow-2xl border border-gray-200 dark:border-gray-800 p-6 sm:p-8 text-center animate-fadeInUp"
  style="animation-delay: 0.2s"
>
  <div class="flex flex-col items-center">
    <div class="mb-4">
      <span
        class="material-symbols-outlined text-6xl sm:text-7xl text-green-500"
      >
        add_shopping_cart
      </span>
    </div>
    <h3 class="text-lg sm:text-xl font-bold text-gray-900 dark:text-white mb-2">
      Crear Pedido
    </h3>
    <p class="text-xs sm:text-sm text-gray-500 dark:text-gray-400">
      Explora nuestro menú y crea un nuevo pedido
    </p>
  </div>
</a>
```

**Características visuales**:

- 🎨 **Icono**: `add_shopping_cart` en verde
- ⚪ **Borde**: Gris normal (`border-gray-200`)
- 📝 **Título**: "Crear Pedido"
- 📍 **Enlace**: `/client/menu` (modo completo con carrito)

---

## 3. Vista de Solo Lectura - client/view.html

**Ya existe en el proyecto**. Características:

- ✅ Muestra todas las categorías e items
- ✅ Permite buscar por nombre
- ✅ Permite filtrar por categoría
- ✅ Muestra detalles de items (modal SweetAlert2)
- ❌ **NO** tiene carrito de compras
- ❌ **NO** permite agregar al carrito
- ❌ **NO** permite crear pedidos
- 🔙 Botón de regresar a `/client/dashboard`

**Vista usada**: `client/view.html` (ya existente, sin modificaciones necesarias)

---

## Comparación de Experiencia de Usuario

| Aspecto               | Waiter/Cashier/Admin               | Cliente                                           |
| --------------------- | ---------------------------------- | ------------------------------------------------- |
| **Dashboard cerrado** | Botón deshabilitado (gris)         | Botón activo naranja "Ver Menú"                   |
| **Mensaje**           | "Fuera de horario laborable"       | "🔒 Restaurante Cerrado - Visualiza nuestro menú" |
| **Acción**            | No permite navegar                 | Permite ver menú en modo vista                    |
| **Backend**           | Rechaza crear/modificar pedidos ✅ | Rechaza crear/modificar pedidos ✅                |
| **Propósito**         | Prevenir acceso completamente      | Permitir ver menú (marketing)                     |

---

## Flujo de Usuario - Cliente

### Escenario 1: Restaurante ABIERTO ✅

1. Cliente accede a `/client/dashboard`
2. Ve botón verde "Crear Pedido"
3. Click → navega a `/client/menu`
4. Puede agregar items al carrito
5. Puede crear pedido
6. **Backend valida** antes de crear

### Escenario 2: Restaurante CERRADO ⛔

1. Cliente accede a `/client/dashboard`
2. Ve botón naranja "Ver Menú" con mensaje "Restaurante Cerrado"
3. Click → navega a `/client/view`
4. Puede ver items y detalles
5. **NO** puede agregar al carrito
6. **NO** puede crear pedido
7. Si intenta POST directo → Backend rechaza

### Escenario 3: Race Condition (Cierra mientras está en el menú) 🔄

1. Cliente abre `/client/menu` (abierto)
2. Llena carrito con items
3. Restaurante cierra mientras completa formulario
4. Click en "Crear Pedido"
5. **Backend valida** → Rechaza con mensaje
6. Cliente ve error: "No se encuentra en horario laborable"

---

## Beneficios de la Implementación Cliente

### 🎯 Marketing y Experiencia

1. ✅ Cliente puede **siempre** ver el menú
2. ✅ Genera interés incluso fuera de horario
3. ✅ Cliente planifica pedidos futuros
4. ✅ No genera frustración (ve menú, sabe que está cerrado)

### 🔒 Seguridad

1. ✅ Backend valida **siempre** antes de crear pedido
2. ✅ Backend valida **siempre** antes de agregar items
3. ✅ Imposible bypassear controles (AJAX protegido)
4. ✅ Logs de intentos fallidos

### 💡 UX Diferenciada

- **Empleados**: Bloqueo total (no pueden trabajar fuera de horario)
- **Clientes**: Acceso de lectura (pueden ver menú siempre)

---

## Testing Recomendado

### Test 1: Dashboard - Restaurante cerrado

1. Configurar horario como cerrado
2. Acceder a `/client/dashboard`
3. ✅ Verificar botón naranja "Ver Menú"
4. Click en botón
5. ✅ Debe navegar a `/client/view`

### Test 2: Dashboard - Restaurante abierto

1. Configurar horario como abierto
2. Acceder a `/client/dashboard`
3. ✅ Verificar botón verde "Crear Pedido"
4. Click en botón
5. ✅ Debe navegar a `/client/menu`

### Test 3: Crear pedido - Cerrado

1. Acceder a `/client/menu` cuando está abierto
2. Agregar items al carrito
3. Cambiar configuración a cerrado (otra pestaña)
4. Click en "Crear Pedido"
5. ✅ Backend debe rechazar con mensaje de error

### Test 4: API directa - Cerrado

1. Usar Postman/curl
2. POST a `/client/orders/create` cuando cerrado
3. ✅ Debe retornar `{"success": false, "message": "...no se encuentra en horario laborable..."}`

### Test 5: Agregar items - Cerrado

1. Tener pedido PENDING existente
2. Cambiar a cerrado
3. POST a `/client/orders/{orderId}/add-items`
4. ✅ Debe rechazar con mensaje

---

## Archivos Modificados

### Backend:

- `ClientController.java`:
  - Línea ~43: Campo `businessHoursService`
  - Líneas ~45-66: Constructor actualizado
  - Líneas ~87-92: `showDashboard()` - pasa `isRestaurantOpen`
  - Líneas ~107-143: **NUEVO** método `showMenuViewOnly()` para `/client/view`
  - Líneas ~237-245: `createOrder()` - validación backend
  - Líneas ~488-497: `addItemsToOrder()` - validación backend

### Frontend:

- `client/dashboard.html`:
  - Líneas ~246-280: Botón "Ver Menú" (cerrado) con diseño naranja
  - Líneas ~282-308: Botón "Crear Pedido" (abierto) con diseño verde

### Sin cambios:

- `client/view.html` (ya existía, funciona como modo lectura)

---

## Compilación

```bash
./mvnw compile -DskipTests
```

✅ **BUILD SUCCESS** - Sin errores

---

## Resumen Ejecutivo

**Problema**: Clientes no podían ver menú fuera de horario  
**Solución**: Modo vista de solo lectura cuando cerrado  
**Protección**: Backend valida siempre antes de crear/modificar  
**Beneficio**: Marketing + Seguridad + Mejor UX

🎯 **Doble propósito**:

1. **Cliente**: Puede ver menú siempre (genera interés)
2. **Restaurante**: Protegido contra pedidos fuera de horario

🔒 **Seguridad garantizada**: Validación en backend (imposible bypassear)
