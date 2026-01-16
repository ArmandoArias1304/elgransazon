# Separación de Métodos de Pago para Delivery

## Resumen del Cambio

Se implementó la separación de los métodos de pago para delivery de los métodos de pago del restaurante. Esto permite que el administrador desactive ciertos métodos de pago **solo para entregas a domicilio** sin afectar los pagos en el restaurante (para comer aquí y para llevar).

## Problema Original

Anteriormente, si se desactivaba un método de pago (ej. tarjeta de crédito), se desactivaba para **todos** los roles y tipos de orden:
- Admin, Waiter, Cashier → No podían cobrar con ese método
- Delivery → No podía cobrar con ese método
- Cliente → No podía seleccionar ese método

## Solución Implementada

Se agregó un nuevo conjunto de métodos de pago exclusivo para delivery (`deliveryPaymentMethods`) que es independiente de los métodos de pago del restaurante.

### Comportamiento Actual

| Tipo de Orden | Métodos de Pago Utilizados |
|--------------|---------------------------|
| DINE_IN (Para comer aquí) | `paymentMethods` (Restaurante) |
| TAKEOUT (Para llevar) | `paymentMethods` (Restaurante) |
| DELIVERY (Entrega a domicilio) | `deliveryPaymentMethods` (Delivery) |

## Archivos Modificados

### 1. Script SQL de Migración
- [ADD_DELIVERY_PAYMENT_METHODS.sql](ADD_DELIVERY_PAYMENT_METHODS.sql) - Crea la tabla `system_delivery_payment_methods` con valores por defecto

### 2. Entidad SystemConfiguration
- **Archivo**: [SystemConfiguration.java](src/main/java/com/aatechsolutions/elgransazon/domain/entity/SystemConfiguration.java)
- **Cambios**:
  - Agregado campo `deliveryPaymentMethods` (Map<PaymentMethodType, Boolean>)
  - Agregado método `isDeliveryPaymentMethodEnabled(PaymentMethodType type)`
  - Agregado método `isPaymentMethodEnabledForOrderType(PaymentMethodType type, OrderType orderType)`
  - Modificado `@PrePersist` para inicializar `deliveryPaymentMethods` con valores por defecto

### 3. Controlador de Configuración del Sistema
- **Archivo**: [SystemConfigurationController.java](src/main/java/com/aatechsolutions/elgransazon/presentation/controller/SystemConfigurationController.java)
- **Cambios**:
  - Agregados parámetros para los métodos de pago de delivery (`deliveryPaymentCash`, `deliveryPaymentCreditCard`, etc.)
  - Actualización del método `updateConfiguration` para guardar ambos conjuntos de métodos

### 4. Servicio de Configuración
- **Archivo**: [SystemConfigurationServiceImpl.java](src/main/java/com/aatechsolutions/elgransazon/application/service/SystemConfigurationServiceImpl.java)
- **Cambios**:
  - Actualizado `updateConfiguration` para manejar `deliveryPaymentMethods`
  - Actualizado `createInitialConfiguration` para inicializar `deliveryPaymentMethods`

### 5. Servicio de Órdenes
- **Archivo**: [OrderServiceImpl.java](src/main/java/com/aatechsolutions/elgransazon/application/service/OrderServiceImpl.java)
- **Cambios**:
  - Modificado `validatePaymentMethod` para recibir también el `OrderType`
  - La validación ahora usa `isPaymentMethodEnabledForOrderType` que selecciona el mapa correcto

### 6. Controlador de Órdenes
- **Archivo**: [OrderController.java](src/main/java/com/aatechsolutions/elgransazon/presentation/controller/OrderController.java)
- **Cambios**:
  - Modificado `showOrderMenu` para pasar los métodos de pago correctos según el tipo de orden
  - Modificado `showMenuToAddItems` para usar los métodos de delivery si la orden es DELIVERY
  - Modificado `editOrderForm` para usar los métodos correctos según el tipo de orden
  - Modificado `loadFormData` para considerar el tipo de orden

### 7. Controlador de Delivery
- **Archivo**: [DeliveryController.java](src/main/java/com/aatechsolutions/elgransazon/presentation/controller/DeliveryController.java)
- **Cambios**:
  - Modificado `processPayment` para validar con `isDeliveryPaymentMethodEnabled`

### 8. Controlador del Cliente
- **Archivo**: [ClientController.java](src/main/java/com/aatechsolutions/elgransazon/presentation/controller/ClientController.java)
- **Cambios**:
  - Agregado `deliveryPaymentMethods` al modelo para que JavaScript pueda filtrar
  - El menú del cliente ahora pasa ambos conjuntos de métodos de pago

### 9. Interfaz de Configuración del Sistema
- **Archivo**: [form.html](src/main/resources/templates/admin/system-configuration/form.html)
- **Cambios**:
  - Agregada nueva sección "Métodos de Pago para Delivery" (estilo naranja para diferenciarlo)
  - Los métodos de restaurante mantienen su estilo original (verde)

### 10. Menú del Cliente
- **Archivo**: [menu.html](src/main/resources/templates/client/menu.html)
- **Cambios**:
  - Agregadas variables JavaScript para `restaurantPaymentMethods` y `deliveryPaymentMethods`
  - Agregada función `updatePaymentMethodsForOrderType` que actualiza el dropdown cuando cambia el tipo de orden

## Cómo Ejecutar la Migración

1. **Ejecutar el script SQL**:
   ```sql
   -- Ejecutar en la base de datos MySQL
   source ADD_DELIVERY_PAYMENT_METHODS.sql
   ```

2. **Reiniciar la aplicación** para que Hibernate reconozca la nueva tabla

## Configuración por Defecto

| Método de Pago | Restaurante | Delivery |
|---------------|-------------|----------|
| Efectivo | ✅ Activado | ✅ Activado |
| Tarjeta de Crédito | ✅ Activado | ❌ Desactivado |
| Tarjeta de Débito | ✅ Activado | ❌ Desactivado |
| Transferencia | ❌ Desactivado | ❌ Desactivado |

## Uso

1. Ve a **Configuración del Sistema** como Admin
2. En la sección **Métodos de Pago**, configura los métodos para el restaurante (verde)
3. En la sección **Métodos de Pago para Delivery** (naranja), configura los métodos para entregas
4. Guarda los cambios

## Validaciones

- Al crear/editar un pedido DELIVERY, solo se muestran los métodos habilitados para delivery
- Al crear/editar un pedido DINE_IN o TAKEOUT, se muestran los métodos del restaurante
- El repartidor (delivery) solo puede cobrar con los métodos habilitados para delivery
- Si se intenta crear un pedido DELIVERY con un método deshabilitado, se muestra un error

## Testing Manual Recomendado

1. **Test: Desactivar método para delivery, mantener en restaurante**
   - Desactiva "Tarjeta de Crédito" solo en Delivery
   - Crea un pedido DELIVERY → No debe aparecer "Tarjeta de Crédito"
   - Crea un pedido DINE_IN → Debe aparecer "Tarjeta de Crédito"

2. **Test: Cliente cambia tipo de orden**
   - Entra al menú del cliente con TAKEOUT seleccionado → Ve todos los métodos del restaurante
   - Cambia a DELIVERY → Los métodos se actualizan a los de delivery

3. **Test: Delivery cobra pedido**
   - Configura solo CASH para delivery
   - El repartidor solo puede cobrar en efectivo

4. **Test: Editar pedido DELIVERY existente**
   - Edita un pedido DELIVERY → Solo se muestran métodos de delivery
