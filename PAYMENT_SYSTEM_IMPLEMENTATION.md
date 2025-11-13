# Sistema de Pagos - Implementación Completa

## Resumen de Cambios

Se ha implementado un sistema completo de procesamiento de pagos para las órdenes del restaurante.

## Funcionalidades Implementadas

### 1. Nuevo Tipo de Pago - TRANSFERENCIA

- ✅ Agregado `TRANSFER` al enum `PaymentMethodType`
- ✅ Integrado en la configuración del sistema
- ✅ Disponible en el formulario de configuración
- ✅ Deshabilitado por defecto (se puede habilitar desde configuración)

### 2. Campo Propina en Órdenes

- ✅ Agregado campo `tip` (DECIMAL(10,2)) a la entidad `Order`
- ✅ Valor por defecto: 0.00
- ✅ Validación: no puede ser negativo
- ✅ Métodos auxiliares:
  - `getFormattedTip()`: Devuelve la propina formateada
  - `getTotalWithTip()`: Calcula total + propina
  - `getFormattedTotalWithTip()`: Devuelve el total con propina formateado

### 3. Controlador de Pagos (`PaymentController`)

**Ruta:** `/admin/payments`

#### Endpoints:

1. **GET `/form/{orderId}`** - Muestra formulario de pago

   - Solo permite pagar órdenes con estado `DELIVERED`
   - Muestra solo métodos de pago habilitados en configuración
   - Valida que existan métodos de pago habilitados

2. **POST `/process/{orderId}`** - Procesa el pago
   - Valida que la orden esté en estado `DELIVERED`
   - Valida que el método de pago esté habilitado
   - Valida que la propina no sea negativa
   - Guarda la propina en la orden
   - Cambia el estado de la orden a `PAID`
   - **Gestión automática de mesas (realizada por OrderService):**
     - Si la mesa está `OCCUPIED` → Cambia a `AVAILABLE`
     - Si la mesa está `RESERVED` con `isOccupied=true` → Marca `isOccupied=false`

### 4. Vista de Pago (`form.html`)

**Ubicación:** `templates/admin/payments/form.html`

**Características:**

- ✅ Diseño moderno sin sidebar (pantalla completa)
- ✅ Vista dividida en 2 columnas:
  - **Izquierda:** Información del pedido y lista de items
  - **Derecha:** Formulario de pago y resumen
- ✅ Información mostrada:
  - Número de orden
  - Tipo de orden
  - Mesa (si aplica)
  - Cliente (si aplica)
  - Estado
  - Empleado que atendió
  - Lista de items con cantidades y comentarios
  - Subtotal, impuesto y total
- ✅ Formulario de pago:
  - Selector de método de pago (solo muestra métodos habilitados)
  - Campo de propina (opcional)
  - Cálculo automático del total con propina
  - Confirmación con SweetAlert2 antes de procesar

### 5. Botón de Pago en Lista de Órdenes

**Ubicación:** `templates/admin/orders/list.html`

- ✅ Botón verde con icono de dólar ($)
- ✅ Solo visible para órdenes con estado `DELIVERED`
- ✅ Redirige a `/admin/payments/form/{orderId}`
- ✅ Diseño consistente con los demás botones de acción

### 6. Scripts SQL

#### `add_tip_column.sql`

```sql
ALTER TABLE orders
ADD COLUMN tip DECIMAL(10,2) DEFAULT 0.00 COMMENT 'Propina dejada en la orden';

UPDATE orders SET tip = 0.00 WHERE tip IS NULL;
```

#### `add_transfer_payment_method.sql`

```sql
INSERT INTO system_payment_methods (system_configuration_id, payment_method_type, enabled)
SELECT id, 'TRANSFER', false
FROM system_configuration
WHERE NOT EXISTS (
    SELECT 1 FROM system_payment_methods
    WHERE system_configuration_id = system_configuration.id
    AND payment_method_type = 'TRANSFER'
);
```

## Flujo de Trabajo

1. **Orden creada** → Estado: `PENDING` (Mesa se ocupa)
2. **Preparación** → Estado: `IN_PREPARATION` (Mesa sigue ocupada)
3. **Listo** → Estado: `READY` (Mesa sigue ocupada)
4. **En camino** (solo DELIVERY) → Estado: `ON_THE_WAY` (Mesa sigue ocupada si es DINE_IN)
5. **Entregado** → Estado: `DELIVERED` ✨ **BOTÓN DE PAGO APARECE** (Mesa sigue ocupada)
6. **Procesar pago:**
   - Seleccionar método de pago
   - Ingresar propina (opcional)
   - Confirmar pago
7. **Pago procesado** → Estado: `PAID` 🎉 **MESA SE LIBERA AUTOMÁTICAMENTE**

## Validaciones Implementadas

### En el Controlador:

- ✅ Solo se pueden pagar órdenes en estado `DELIVERED`
- ✅ Solo se aceptan métodos de pago habilitados en configuración
- ✅ La propina no puede ser negativa
- ✅ Valida que la orden exista

### En la Vista:

- ✅ Método de pago obligatorio
- ✅ Confirmación antes de procesar pago
- ✅ Validación de propina no negativa en frontend
- ✅ Cálculo automático del total con propina

## Gestión de Estados de Mesa

### ⚠️ IMPORTANTE: Liberación de Mesa

**Las mesas se liberan SOLO cuando la orden se paga (estado PAID), NO cuando se entrega.**

### Después de Pagar (Estado PAID):

1. **Si la mesa está OCCUPIED:**

   - Cambia a `AVAILABLE`
   - `isOccupied` se marca como `false`
   - Logging: "Table {id} freed and marked as AVAILABLE after order payment"

2. **Si la mesa está RESERVED con isOccupied=true:**
   - Marca `isOccupied = false`
   - Mantiene estado `RESERVED`
   - Logging: "Table {id} is_occupied set to false after order payment"

### ℹ️ Nota Técnica:

- La liberación de la mesa es manejada automáticamente por `OrderService.changeStatus()`
- Cuando el estado cambia a `PAID`, el servicio libera la mesa automáticamente
- El `PaymentController` solo cambia el estado a `PAID`, no maneja mesas directamente

## Archivos Modificados

### Backend (Java):

1. `PaymentMethodType.java` - Agregado TRANSFER
2. `Order.java` - Agregado campo tip y métodos auxiliares
3. `SystemConfiguration.java` - Agregado TRANSFER en inicialización
4. `PaymentController.java` - ⭐ NUEVO controlador
5. `SystemConfigurationController.java` - Agregado parámetro paymentTransfer
6. `OrderController.java` - Mesa asignada en edición (cambio previo)

### Frontend (HTML):

1. `admin/payments/form.html` - ⭐ NUEVA vista de pago
2. `admin/orders/list.html` - Agregado botón de pago
3. `admin/system-configuration/form.html` - Agregado checkbox de Transferencia

### Base de Datos (SQL):

1. `add_tip_column.sql` - ⭐ NUEVO script
2. `add_transfer_payment_method.sql` - ⭐ NUEVO script

## Requisitos para Usar el Sistema

1. **Ejecutar scripts SQL:**

   ```bash
   # Agregar columna tip
   mysql -u root -p elgransazon < database/add_tip_column.sql

   # Agregar método de pago Transfer
   mysql -u root -p elgransazon < database/add_transfer_payment_method.sql
   ```

2. **Habilitar métodos de pago:**

   - Ir a "Configuración del Sistema"
   - Marcar los métodos de pago deseados (CASH, CREDIT_CARD, DEBIT_CARD, TRANSFER)
   - Guardar configuración

3. **Procesar pagos:**
   - La orden debe estar en estado `DELIVERED`
   - Click en botón verde "$" en la lista de órdenes
   - Seleccionar método de pago
   - Ingresar propina (opcional)
   - Confirmar pago

## Características Destacadas

✨ **Diseño sin sidebar** - Vista de pago ocupa toda la pantalla para mejor experiencia
💰 **Propinas opcionales** - Sistema flexible para manejar propinas
🔒 **Validaciones completas** - Tanto en frontend como backend
🎨 **UI moderna** - Diseño limpio con Tailwind CSS
📱 **Responsive** - Funciona en dispositivos móviles
🔔 **Confirmaciones** - SweetAlert2 para mejor UX
🔄 **Gestión automática de mesas** - Liberación automática al pagar
📊 **Cálculos automáticos** - Total con propina calculado en tiempo real

## Roles y Permisos

- ✅ `ROLE_ADMIN` - Acceso completo
- ✅ `ROLE_WAITER` - Acceso completo
- ❌ Otros roles - Sin acceso

## Notas Importantes

1. **Solo órdenes DELIVERED pueden ser pagadas**
2. **Los métodos de pago se configuran en "Configuración del Sistema"**
3. **La propina es opcional y se suma al total**
4. **El estado PAID es final** - No se puede revertir
5. **⚠️ Las mesas se liberan SOLO cuando se paga (PAID), NO cuando se entrega (DELIVERED)**
6. **TRANSFER está deshabilitado por defecto** - Se debe habilitar manualmente
7. **La liberación de mesas es automática** - Manejada por OrderService.changeStatus()

## Próximas Mejoras Sugeridas

- 📊 Reporte de propinas por empleado
- 📈 Estadísticas de métodos de pago más utilizados
- 🧾 Impresión de recibo de pago
- 💳 Integración con pasarelas de pago
- 📧 Envío de comprobante por email
