# Implementación del Módulo de Clientes

## Resumen

Se ha implementado un sistema completo de autenticación y gestión de pedidos para clientes, integrado con Spring Security y el sistema existente de órdenes.

## Archivos Creados

### 1. Entidades y Repositorios

- **`Customer.java`**: Entidad que representa a los clientes del sistema
  - Campos: ID, nombre completo, email, teléfono, dirección, contraseña (BCrypt), activo, timestamps
  - Relación con Order mediante `id_customer`

- **`CustomerRepository.java`**: Repositorio JPA para clientes
  - Métodos: findByEmail, existsByEmail, findByActiveTrue

### 2. Servicios

- **`CustomerService.java`** y **`CustomerServiceImpl.java`**: 
  - CRUD completo de clientes
  - Registro con encriptación de contraseña (BCrypt)
  - Actualización de último acceso
  - Activación/desactivación de cuentas

- **`CustomerOrderServiceImpl.java`**: Implementación de OrderService para clientes
  - **Restricciones**:
    - Solo pueden crear órdenes TAKEOUT y DELIVERY (no DINE_IN)
    - No pueden editar, cancelar o cambiar estados de órdenes
    - Solo pueden ver sus propias órdenes (filtradas por customer.email)
  - **Características**:
    - Las órdenes se asocian al cliente mediante `Order.customer`
    - Usa la dirección del cliente para entregas si no se especifica otra
    - Respeta aplicación de promociones automáticamente
    - Los pedidos se marcan con `createdBy = email del cliente`

### 3. Seguridad

- **`CustomUserDetailsService.java`**: UserDetailsService personalizado
  - Maneja autenticación tanto de empleados como de clientes
  - Busca primero en Employee (por username), luego en Customer (por email)
  - Asigna `ROLE_CLIENT` a clientes automáticamente

- **`CustomAuthenticationSuccessHandler.java`**: Actualizado
  - Redirige clientes a `/client/menu` después del login
  - Actualiza `lastAccess` tanto de empleados como de clientes

- **`SecurityConfig.java`**: Actualizado
  - Rutas públicas: `/client/register`, `/client/login`
  - Rutas protegidas para clientes: `/client/**` requiere `ROLE_CLIENT`

### 4. Controladores

- **`ClientAuthController.java`**: Maneja registro de clientes
  - GET `/client/register`: Muestra formulario de registro
  - POST `/client/register`: Procesa registro de cliente
  - Validación de email duplicado
  - Redirección a login después de registro exitoso

- **`ClientController.java`**: Panel principal de clientes
  - GET `/client/menu`: Muestra menú con items disponibles
  - GET `/client/orders`: Historial de pedidos del cliente
  - GET `/client/orders/{id}`: Detalle de un pedido
  - POST `/client/orders/create`: Crea nuevo pedido (AJAX)
  - GET `/client/promotions/active`: Obtiene promociones activas (AJAX)
  - GET/POST `/client/profile`: Ver y actualizar perfil

### 5. Base de Datos

- **`ADD_CUSTOMER_SUPPORT.sql`**: Script SQL para migración
  - Crea tabla `customers`
  - Añade columna `id_customer` a tabla `orders`
  - Hace `id_employee` nullable en `orders` (para pedidos de clientes)
  - Inserta `ROLE_CLIENT` en tabla `roles`
  - Incluye índices para optimización
  - Incluye script de rollback

### 6. Vistas (Templates)

- **`registerClient.html`**: Formulario de registro de clientes
  - Campos: nombre completo, email, teléfono, dirección (opcional), contraseña
  - Validación en cliente y servidor
  - Soporte para modo oscuro
  - Diseño responsive con TailwindCSS

## Cambios en Entidades Existentes

### Order.java

```java
// Añadido campo para relación con Customer
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "id_customer", nullable = true)
private Customer customer;

// id_employee ahora es nullable
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "id_employee", nullable = true)
private Employee employee;
```

### OrderRepository.java

```java
// Nuevos métodos para consultar pedidos por cliente
List<Order> findByCustomerId(Long customerId);
List<Order> findByCustomerEmail(String customerEmail);
```

### Role.java

```java
// Nueva constante para rol de cliente
public static final String CLIENT = "ROLE_CLIENT";
```

## Flujo de Trabajo del Cliente

### 1. Registro
1. Cliente accede a `/client/register`
2. Completa formulario con datos personales
3. Sistema valida y crea cuenta con contraseña encriptada
4. Redirección a login

### 2. Login
1. Cliente accede a `/login` con su email y contraseña
2. Spring Security valida credenciales usando `CustomUserDetailsService`
3. Se asigna `ROLE_CLIENT` automáticamente
4. `CustomAuthenticationSuccessHandler` redirige a `/client/menu`

### 3. Visualización del Menú
1. Cliente ve items agrupados por categoría
2. Items muestran promociones activas (badges y precios con descuento)
3. Cliente puede filtrar por categoría
4. Solo se muestran items disponibles (en stock)

### 4. Creación de Pedido
1. Cliente selecciona tipo de orden: TAKEOUT o DELIVERY
2. Añade items al carrito (con cantidad y comentarios)
3. Si es DELIVERY, puede usar dirección guardada o ingresar nueva
4. Sistema aplica promociones automáticamente
5. Validación de stock en tiempo real
6. Al confirmar:
   - Se crea Order con `customer` = cliente actual
   - Se asocian OrderDetail con precios promocionales si aplica
   - Se deduce stock de inventario
   - Estado inicial: PENDING

### 5. Historial de Pedidos
1. Cliente accede a `/client/orders`
2. Ve lista de todos sus pedidos (ordenados por fecha)
3. Puede ver estado actual de cada pedido
4. Puede ver detalle completo (items, cantidades, precios, total)
5. **No puede modificar ni cancelar pedidos** (solo visualización)

### 6. Perfil
1. Cliente puede ver y actualizar su información
2. Puede cambiar: nombre, teléfono, dirección
3. Puede cambiar contraseña
4. No puede cambiar email (es su identificador único)

## Restricciones y Seguridad

### Clientes NO pueden:
- Crear pedidos DINE_IN (para comer en el restaurante)
- Editar pedidos existentes
- Cancelar pedidos
- Cambiar estados de pedidos
- Ver pedidos de otros clientes
- Acceder a panel de administración
- Ver información de empleados o inventario

### Clientes SÍ pueden:
- Crear pedidos TAKEOUT y DELIVERY
- Ver menú completo con precios y promociones
- Ver historial completo de sus propios pedidos
- Ver detalle y estado de sus pedidos
- Actualizar su perfil y dirección

## Aplicación de Promociones

El sistema aplica promociones automáticamente cuando el cliente añade items al pedido:

1. `ItemMenu.getBestPromotion()` devuelve la mejor promoción activa para el item
2. `Promotion.calculateDiscountedPrice()` calcula el precio con descuento
3. Se guarda en `OrderDetail`:
   - `unitPrice`: Precio original del item
   - `promotionAppliedPrice`: Precio con descuento (si aplica)
   - `appliedPromotionId`: ID de la promoción aplicada
4. `OrderDetail.calculateSubtotal()` usa `promotionAppliedPrice` si existe

## Consideraciones Importantes

### Base de Datos
- **Ejecutar `ADD_CUSTOMER_SUPPORT.sql`** antes de usar el sistema
- Respaldar base de datos antes de ejecutar el script
- Verificar que todas las tablas se crearon correctamente

### Datos de Prueba
Para crear un cliente de prueba, usar BCrypt para la contraseña:
```sql
INSERT INTO customers (full_name, email, phone, address, password, active)
VALUES (
    'Juan Pérez',
    'juan@email.com',
    '555-0123',
    'Calle Principal 123',
    '$2a$10$...', -- BCrypt hash de la contraseña
    TRUE
);
```

### Login Unificado
- Empleados usan su `username` para login
- Clientes usan su `email` para login
- Ambos usan el mismo formulario `/login`
- El sistema identifica automáticamente si es empleado o cliente

### Direcciones de Entrega
- Clientes pueden guardar una dirección por defecto en su perfil
- Al crear pedido DELIVERY, pueden:
  - Usar dirección guardada (se llena automáticamente)
  - Ingresar dirección diferente (sobrescribe la guardada solo para ese pedido)
- Si no hay dirección guardada y no se proporciona, se rechaza el pedido DELIVERY

## Vistas Pendientes de Crear

Aunque la lógica del backend está completa, estas vistas deben ser creadas:

1. **`client/menu.html`**: Vista del menú para clientes (puede reutilizar/adaptar `admin/orders/order-menu.html`)
2. **`client/orders.html`**: Lista de pedidos del cliente
3. **`client/order-detail.html`**: Detalle de un pedido específico
4. **`client/profile.html`**: Perfil del cliente

Estas vistas deben:
- Usar el sistema de temas existente (modo claro/oscuro)
- Ser responsive
- Seguir el diseño de TailwindCSS usado en el resto del proyecto
- Mostrar promociones con badges similares al menú de empleados

## Integración con Sistema Existente

- **OrderController**: No requiere cambios, sigue funcionando para empleados
- **PromotionController**: Ya soporta consultas AJAX para clientes (`/client/promotions/active`)
- **ItemMenuController**: No requiere cambios
- **Dashboard**: No accesible para clientes

## Testing

### Casos de Prueba Sugeridos

1. **Registro**:
   - Registrar cliente con datos válidos
   - Intentar registrar con email duplicado
   - Validar campos requeridos
   - Verificar contraseña encriptada

2. **Login**:
   - Login exitoso con email/contraseña correctos
   - Login fallido con credenciales incorrectas
   - Verificar redirección a `/client/menu`

3. **Crear Pedido**:
   - Crear pedido TAKEOUT exitoso
   - Crear pedido DELIVERY con dirección guardada
   - Crear pedido DELIVERY con dirección nueva
   - Intentar crear pedido DINE_IN (debe fallar)
   - Validar stock insuficiente
   - Verificar aplicación de promociones

4. **Visualización**:
   - Ver historial de pedidos
   - Ver detalle de pedido
   - Intentar acceder a pedido de otro cliente (debe fallar)
   - Ver perfil

5. **Seguridad**:
   - Intentar acceder a rutas de admin (debe denegar)
   - Intentar modificar pedido (debe fallar)
   - Intentar ver pedidos de otro cliente (debe denegar)

## Próximos Pasos

1. Ejecutar script SQL `ADD_CUSTOMER_SUPPORT.sql`
2. Crear las vistas HTML pendientes
3. Probar el flujo completo de registro, login y creación de pedidos
4. Añadir validaciones adicionales según necesidades del negocio
5. Implementar notificaciones por email (confirmación de pedido, cambio de estado, etc.)
6. Considerar añadir tracking en tiempo real de pedidos
7. Implementar sistema de puntos/recompensas para clientes frecuentes
