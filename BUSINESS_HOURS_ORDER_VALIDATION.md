# Validación de Horarios Laborables para Creación de Pedidos

## Descripción General

Se ha implementado un sistema de validación que **impide crear pedidos fuera del horario laborable del restaurante**. La implementación varía según el rol del usuario:

- **Waiter/Cashier**: El botón "Crear Pedido" se deshabilita y muestra un mensaje cuando el restaurante está cerrado
- **Admin/Manager**: Se muestra un SweetAlert2 al intentar crear un pedido cuando el restaurante está cerrado

## Cambios Implementados

### 1. Servicio de Horarios (BusinessHoursService)

#### BusinessHoursService.java (Interfaz)

- **Línea 60-65**: Nuevo método `boolean isOpenNow()`

#### BusinessHoursServiceImpl.java (Implementación)

- **Líneas 151-181**: Implementación de `isOpenNow()`
  ```java
  public boolean isOpenNow() {
      java.time.LocalDateTime now = java.time.LocalDateTime.now();
      java.time.DayOfWeek javaDayOfWeek = now.getDayOfWeek();
      LocalTime currentTime = now.toLocalTime();

      DayOfWeek customDayOfWeek = DayOfWeek.valueOf(javaDayOfWeek.name());

      boolean isOpen = isOpenAt(customDayOfWeek, currentTime);
      log.debug("Restaurant is {} at {} on {}",
                isOpen ? "open" : "closed",
                currentTime,
                customDayOfWeek.getDisplayName());

      return isOpen;
  }
  ```
- **Funcionalidad**:
  - Obtiene la fecha y hora actual del sistema
  - Convierte `java.time.DayOfWeek` al enum personalizado `DayOfWeek`
  - Utiliza el método existente `isOpenAt()` para verificar si está abierto
  - Retorna `true` si el restaurante está en horario laborable

---

### 2. Validación en Backend (OrderController)

#### CRÍTICO: Validación al crear pedidos nuevos

**Método `createOrderAsync()` - Creación AJAX (líneas 750-819)**:

```java
try {
    validateRole(role, authentication);
    OrderService orderService = getOrderService(role);

    // Validate restaurant is open
    if (!businessHoursService.isOpenNow()) {
        throw new IllegalStateException("No se puede crear el pedido. El restaurante no se encuentra en horario laborable en este momento.");
    }

    // Validate payment method
    SystemConfiguration config = systemConfigurationService.getConfiguration();
    // ... continúa con la creación
}
```

**Método `createOrder()` - Creación tradicional (líneas 849-927)**:

```java
// Validate role
validateRole(role, authentication);

// Validate restaurant is open
if (!businessHoursService.isOpenNow()) {
    log.warn("Attempt to create order outside business hours by user: {}", username);
    redirectAttributes.addFlashAttribute("errorMessage",
        "No se puede crear el pedido. El restaurante no se encuentra en horario laborable en este momento.");
    return "redirect:/" + role + "/orders";
}

// Get the correct service based on role
OrderService orderService = getOrderService(role);
```

#### CRÍTICO: Validación al agregar items a pedidos existentes

**Método `addItemsToOrder()` - Agregar items (formulario) (líneas 508-598)**:

```java
// Validate role
validateRole(role, authentication);

// Validate restaurant is open
if (!businessHoursService.isOpenNow()) {
    log.warn("Attempt to add items to order outside business hours by user: {}", username);
    redirectAttributes.addFlashAttribute("errorMessage",
        "No se pueden agregar items al pedido. El restaurante no se encuentra en horario laborable en este momento.");
    return "redirect:/" + role + "/orders";
}

// Get the correct service based on role
OrderService orderService = getOrderService(role);
```

**Método `addItemsToOrderAjax()` - Agregar items AJAX (líneas 1327-1415)**:

```java
// Validate role
validateRole(role, authentication);

// Validate restaurant is open
if (!businessHoursService.isOpenNow()) {
    log.warn("Attempt to add items to order outside business hours by user: {}", username);
    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("success", false);
    errorResponse.put("message", "No se pueden agregar items al pedido. El restaurante no se encuentra en horario laborable en este momento.");
    return errorResponse;
}

// Get the correct service based on role
OrderService orderService = getOrderService(role);
```

**Protección completa**: Ahora es **imposible** crear pedidos o agregar items fuera del horario laborable, incluso si:

- El usuario llegó a la vista cuando estaba abierto
- Se cambió la configuración mientras el usuario estaba en el formulario
- El día cambió a no laborable mientras se completaba el formulario
- Alguien intenta hacer una petición directa a la API

---

### 3. Controladores (Dashboard)

#### AdminController.java

- **Línea 4**: Import de `BusinessHoursService`
- **Línea 32**: Campo `private final BusinessHoursService businessHoursService`
- **Líneas 51-54**: Verificación en el método `dashboard()`
  ```java
  boolean isRestaurantOpen = businessHoursService.isOpenNow();
  model.addAttribute("isRestaurantOpen", isRestaurantOpen);
  log.debug("Restaurant is currently: {}", isRestaurantOpen ? "open" : "closed");
  ```

#### WaiterController.java

- **Línea 4**: Import de `BusinessHoursService`
- **Línea 47**: Campo `private final BusinessHoursService businessHoursService`
- **Líneas 64-67**: Misma lógica de verificación en `dashboard()`

#### CashierController.java

- **Línea 3**: Import con wildcard `import ...service.*;`
- **Líneas 28-62**: Constructor modificado para aceptar `BusinessHoursService`
- **Línea 39**: Campo `private final BusinessHoursService businessHoursService`
- **Líneas 70-77**: Verificación en `dashboard()`

#### OrderController.java

- **Línea 43**: Campo `private final BusinessHoursService businessHoursService`
- **Líneas 48-67**: Constructor actualizado con parámetro `BusinessHoursService`
- **Líneas 219-223**: Agregado en método `listOrders()`
  ```java
  boolean isRestaurantOpen = businessHoursService.isOpenNow();
  model.addAttribute("isRestaurantOpen", isRestaurantOpen);
  log.debug("Restaurant is currently: {}", isRestaurantOpen ? "open" : "closed");
  ```

---

### 3. Vistas (Thymeleaf)

#### waiter/dashboard.html

**Líneas 212-253**: Renderizado condicional del botón "Crear Pedidos"

**Estado CERRADO** (`th:if="${!isRestaurantOpen}"`):

```html
<div
  th:if="${!isRestaurantOpen}"
  class="...cursor-not-allowed opacity-60"
  title="Restaurante cerrado - No es posible crear pedidos..."
>
  <span class="...text-gray-500">table_restaurant</span>
  <h3 class="...text-gray-700">Crear Pedidos</h3>
  <p class="...text-red-600">🔒 Restaurante Cerrado</p>
  <p class="...text-gray-500">Fuera de horario laborable</p>
</div>
```

**Estado ABIERTO** (`th:if="${isRestaurantOpen}"`):

```html
<a
  th:if="${isRestaurantOpen}"
  href="/waiter/orders/select-table"
  class="...hover:shadow-2xl"
>
  <span class="...text-pink-500">table_restaurant</span>
  <h3>Crear Pedidos</h3>
  <p>Selecciona una mesa para crear un pedido</p>
</a>
```

**Características del estado cerrado**:

- Colores grises (`text-gray-500`, `text-gray-700`)
- `cursor-not-allowed` para indicar que no es clickeable
- `opacity-60` para efecto visual de deshabilitado
- Sin efectos hover
- Sin enlace (no es `<a>`, es `<div>`)
- Tooltip explicativo en atributo `title`
- Icono de candado 🔒

---

#### cashier/dashboard.html

**Líneas 212-253**: Misma implementación que waiter, pero el enlace apunta a `/cashier/orders/select-table`

---

#### admin/orders/list.html

**Línea 111**: Botón con ID para JavaScript

```html
<a
  id="createOrderBtn"
  th:href="@{/admin/orders/select-table}"
  class="...bg-gradient-to-r from-primary to-primary-dark..."
>
  <span class="material-symbols-outlined">add</span>
  Nuevo Pedido
</a>
```

**Líneas 1505-1545**: Script de validación con SweetAlert2

```html
<!-- Script de validación de horario laborable -->
<script th:inline="javascript">
  /*<![CDATA[*/
  const isRestaurantOpen = /*[[${isRestaurantOpen}]]*/ true;

  document.addEventListener("DOMContentLoaded", function () {
    const createOrderBtn = document.getElementById("createOrderBtn");

    if (createOrderBtn && !isRestaurantOpen) {
      createOrderBtn.addEventListener("click", function (e) {
        e.preventDefault();

        Swal.fire({
          icon: "warning",
          title: "Restaurante Cerrado",
          html: `
            <p class="text-gray-700 dark:text-gray-300 mb-3">
              El restaurante no se encuentra en horario laborable en este momento.
            </p>
            <p class="text-gray-600 dark:text-gray-400 text-sm">
              <strong>Nota:</strong> Solo puedes crear pedidos durante los días y horarios 
              establecidos en la configuración del sistema.
            </p>
          `,
          confirmButtonText: "Entendido",
          confirmButtonColor: "#38e07b",
          customClass: {
            popup: "dark:bg-gray-900 dark:text-white",
            title: "dark:text-white",
            confirmButton: "px-6 py-2.5 rounded-xl font-semibold",
          },
        });
      });
    }
  });
  /*]]>*/
</script>
```

**Funcionamiento del script**:

1. Obtiene el valor de `isRestaurantOpen` desde el modelo de Thymeleaf
2. Cuando el DOM está listo, busca el botón por ID
3. Si el restaurante está cerrado (`!isRestaurantOpen`), agrega un listener al click
4. Previene la navegación con `e.preventDefault()`
5. Muestra un SweetAlert2 con:
   - Icono de advertencia (`warning`)
   - Título "Restaurante Cerrado"
   - Mensaje explicativo con HTML
   - Botón "Entendido" con color verde del sistema
   - Estilos compatibles con dark mode

---

## Flujo de Validación

### Doble Capa de Protección (Frontend + Backend)

#### Frontend (UI):

1. El usuario accede al dashboard
2. El controlador verifica `businessHoursService.isOpenNow()`
3. Pasa `isRestaurantOpen` al modelo
4. Thymeleaf renderiza condicionalmente:
   - **Abierto**: Botón activo con enlace
   - **Cerrado**: Botón deshabilitado o warning en Admin

#### Backend (Validación crítica):

1. El usuario intenta crear/modificar un pedido
2. **ANTES** de procesar cualquier dato, se valida `businessHoursService.isOpenNow()`
3. Si está cerrado:
   - Lanza `IllegalStateException` (AJAX) o redirige con error (formulario)
   - No se procesa la solicitud
   - Se registra el intento en logs
4. Si está abierto:
   - Continúa con el proceso normal de creación/modificación

### Escenarios Protegidos:

#### ✅ Escenario 1: Usuario llega cuando está abierto, pero cierra antes de enviar

- **Frontend**: Usuario ve formulario activo
- **Backend**: Al enviar, valida que siga abierto → **RECHAZA** si ya cerró
- **Resultado**: Pedido no se crea, mensaje de error claro

#### ✅ Escenario 2: Día laborable termina a medianoche

- **Frontend**: Usuario está creando pedido a las 23:59
- **Backend**: A las 00:00 el día cambia a no laborable → **RECHAZA**
- **Resultado**: Pedido no se crea, debe esperar al siguiente día laborable

#### ✅ Escenario 3: Admin desactiva horario mientras usuario trabaja

- **Frontend**: Usuario empezó a crear pedido cuando estaba activo
- **Backend**: Configuración cambia → **RECHAZA** inmediatamente
- **Resultado**: No se puede completar el pedido, mensaje apropiado

#### ✅ Escenario 4: Intento de manipulación directa de API

- **Frontend**: Atacante intenta POST directo sin pasar por UI
- **Backend**: Valida horario laborable **SIEMPRE** → **RECHAZA**
- **Resultado**: Imposible crear pedidos fuera de horario por ningún método

---

### Para Waiter y Cashier:

1. El usuario accede al dashboard
2. El controlador verifica `businessHoursService.isOpenNow()`
3. Pasa `isRestaurantOpen` al modelo
4. Thymeleaf renderiza condicionalmente:
   - **Abierto**: Botón activo con enlace
   - **Cerrado**: `<div>` deshabilitado con mensaje
5. **Al enviar formulario**: Backend valida nuevamente antes de crear

### Para Admin/Manager:

1. El usuario accede a la lista de pedidos
2. El controlador verifica `businessHoursService.isOpenNow()`
3. Pasa `isRestaurantOpen` al modelo
4. JavaScript intercepta el click del botón
5. Si está cerrado:
   - Previene la navegación
   - Muestra SweetAlert2 con advertencia
6. Si está abierto:
   - Permite la navegación normal
7. **Al enviar formulario**: Backend valida nuevamente antes de crear

---

## Configuración del Horario Laborable

El sistema utiliza la entidad `BusinessHours` con los siguientes campos:

- `dayOfWeek` (DayOfWeek): Día de la semana
- `openTime` (LocalTime): Hora de apertura
- `closeTime` (LocalTime): Hora de cierre
- `isClosed` (Boolean): Indica si está cerrado ese día

### Ejemplos de configuración:

**Restaurante cerrado los domingos**:

```
DayOfWeek: SUNDAY
isClosed: true
```

**Horario normal (Lunes a Sábado)**:

```
DayOfWeek: MONDAY
openTime: 11:00
closeTime: 22:00
isClosed: false
```

**Sin configuración**:
Si no hay configuración de `BusinessHours`, `isOpenNow()` retorna `false` por defecto.

---

## Beneficios de la Implementación

### 🔒 Seguridad Total (Frontend + Backend)

1. ✅ **Prevención en UI**: Usuarios ven botones deshabilitados o advertencias
2. ✅ **Validación crítica en Backend**: Imposible bypassear controles de UI
3. ✅ **Protección contra race conditions**: Valida en el momento exacto de crear/modificar
4. ✅ **Protección contra manipulación directa**: API endpoints validan siempre
5. ✅ **Logs de auditoría**: Todos los intentos fallidos se registran

### 🎯 Experiencia de Usuario

1. ✅ **UX diferenciada por rol**:
   - Waiter/Cashier: Información pasiva (botón deshabilitado)
   - Admin: Advertencia activa (puede intentar, pero se le informa)
2. ✅ **Validación en tiempo real**: Verifica el estado actual al cargar la vista
3. ✅ **Feedback claro**: Mensajes explicativos sobre por qué no se puede crear el pedido
4. ✅ **Prevención de frustración**: Usuario sabe de inmediato si puede o no trabajar

### 🛠️ Técnica

1. ✅ **Configuración centralizada**: Usa el sistema existente de `BusinessHours`
2. ✅ **Compatibilidad con dark mode**: UI adaptada al tema del sistema
3. ✅ **Logs informativos**: Facilita el debugging con mensajes de estado
4. ✅ **Múltiples métodos protegidos**: Crear pedido, agregar items (AJAX y formulario)
5. ✅ **Sin efectos secundarios**: Validación no afecta pedidos existentes o completados

---

## Pruebas Recomendadas

### Pruebas de Frontend (UI)

1. **Horario laborable normal**: Verificar que todos los roles pueden ver botón activo
2. **Fuera de horario**: Verificar que:
   - Waiter ve botón deshabilitado
   - Cashier ve botón deshabilitado
   - Admin ve SweetAlert al hacer click
3. **Día no laborable**: Mismo comportamiento que fuera de horario

### Pruebas de Backend (Validación crítica) ⚠️ IMPORTANTE

#### Test 1: Race condition - Cierre mientras se completa formulario

1. Abrir formulario de crear pedido cuando está ABIERTO
2. Llenar todos los datos del pedido
3. **ANTES de enviar**, cambiar configuración a CERRADO
4. Enviar formulario
5. ✅ **Resultado esperado**: Error "El restaurante no se encuentra en horario laborable..."

#### Test 2: Cambio de día a medianoche

1. Crear pedido a las 23:58 (horario abierto)
2. Llenar formulario lentamente
3. Enviar después de las 00:00 (nuevo día cerrado)
4. ✅ **Resultado esperado**: Error de horario no laborable

#### Test 3: Agregar items a pedido existente

1. Tener un pedido PENDIENTE creado durante horario abierto
2. Cambiar configuración a CERRADO
3. Intentar agregar items al pedido
4. ✅ **Resultado esperado**: Error "No se pueden agregar items al pedido..."

#### Test 4: Petición directa a API (AJAX)

1. Usar herramienta como Postman o curl
2. Enviar POST a `/admin/orders/create-async` cuando está CERRADO
3. ✅ **Resultado esperado**: Response con `success: false` y mensaje de error

#### Test 5: Multiple usuarios simultáneos

1. Usuario A empieza a crear pedido (abierto)
2. Usuario B cambia configuración a cerrado
3. Usuario A intenta enviar
4. ✅ **Resultado esperado**: Usuario A recibe error

#### Test 6: Sin configuración de horarios

1. Eliminar/desactivar toda configuración de `BusinessHours`
2. Intentar crear pedido
3. ✅ **Resultado esperado**: Sistema trata como cerrado, no permite crear

---

## Archivos Modificados

- `BusinessHoursService.java` (interfaz)
- `BusinessHoursServiceImpl.java` (implementación)
- `AdminController.java`
- `WaiterController.java`
- `CashierController.java`
- `OrderController.java`
- `waiter/dashboard.html`
- `cashier/dashboard.html`
- `admin/orders/list.html`

---

## Notas Técnicas

- **SweetAlert2**: Ya estaba incluido en `admin/orders/list.html` (línea 25)
- **Thymeleaf inline JavaScript**: Usa `th:inline="javascript"` y `/*[[${variable}]]*/` para pasar valores al script
- **Event listener**: Se agrega después de que el DOM está completamente cargado
- **Tailwind + Dark mode**: Usa clases `dark:` para compatibilidad con tema oscuro
- **Material Symbols**: Iconos usados en los botones y cards
- **Logging**: Todos los controladores y servicios incluyen logs DEBUG para troubleshooting
