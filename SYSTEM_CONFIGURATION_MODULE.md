# Módulo de Configuración del Sistema

## Descripción General
Este módulo permite gestionar toda la configuración global del restaurante desde una interfaz centralizada. Implementa el patrón Singleton para asegurar que solo exista una configuración en el sistema.

## Características Implementadas

### 1. Configuración General
- **Nombre del restaurante**: Nombre comercial del establecimiento
- **Slogan**: Frase descriptiva del negocio
- **Logo**: URL de la imagen del logo
- **Dirección**: Dirección física del restaurante
- **Teléfono**: Número de contacto
- **Correo electrónico**: Email de contacto
- **IVA**: Tasa de impuesto configurable (0-100%)

### 2. Días Laborales
- Selección múltiple de días de trabajo (Lunes a Domingo)
- Los días pueden ser activados/desactivados según las necesidades del negocio

### 3. Horarios de Atención
- Configuración de horarios por cada día laboral
- Campos: Hora de apertura, Hora de cierre
- Opción para marcar días como "Cerrado"
- Validación automática: hora de cierre debe ser posterior a hora de apertura
- Los horarios solo pueden definirse para días laborales configurados

### 4. Métodos de Pago
Tres métodos de pago predefinidos con activación/desactivación individual:
- Efectivo
- Tarjeta de Crédito
- Tarjeta de Débito

### 5. Redes Sociales
- CRUD completo para gestionar redes sociales
- Campos: Nombre, URL, Icono (Font Awesome), Orden de visualización
- Estado activo/inactivo
- Soporte para cualquier red social

## Arquitectura

### Entidades (Domain Layer)

#### `SystemConfiguration`
- Entidad principal (Singleton)
- Contiene toda la información básica del restaurante
- Relaciones:
  - OneToMany con `BusinessHours`
  - OneToMany con `SocialNetwork`
  - ElementCollection para `workDays`
  - ElementCollection (Map) para `paymentMethods`

#### `BusinessHours`
- Horarios de atención por día
- Validación de horarios coherentes
- ManyToOne con `SystemConfiguration`

#### `SocialNetwork`
- Información de redes sociales
- Ordenable y activable/desactivable
- ManyToOne con `SystemConfiguration`

### Enums

#### `DayOfWeek`
```java
MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
```
- Método `getDisplayName()`: Retorna nombre en español
- Método `toJavaDayOfWeek()`: Convierte a java.time.DayOfWeek

#### `PaymentMethodType`
```java
CASH, CREDIT_CARD, DEBIT_CARD
```
- Método `getDisplayName()`: Retorna nombre en español

### Repositorios (Data Access Layer)

#### `SystemConfigurationRepository`
- `findFirstConfiguration()`: Obtiene la única configuración
- `existsConfiguration()`: Verifica si existe configuración
- `countConfigurations()`: Cuenta configuraciones (debe ser 0 o 1)

#### `BusinessHoursRepository`
- `findBySystemConfigurationId()`: Lista todos los horarios
- `findBySystemConfigurationIdAndDayOfWeek()`: Busca horario por día
- `findActiveBySystemConfigurationId()`: Solo días no cerrados
- `existsBySystemConfigurationIdAndDayOfWeek()`: Verifica existencia

#### `SocialNetworkRepository`
- `findBySystemConfigurationId()`: Lista todas las redes
- `findActiveBySystemConfigurationId()`: Solo redes activas
- `findBySystemConfigurationIdAndNameIgnoreCase()`: Busca por nombre
- `countActiveBySystemConfigurationId()`: Cuenta redes activas

### Servicios (Application Layer)

#### `SystemConfigurationService` / `SystemConfigurationServiceImpl`
**Métodos principales:**
- `getConfiguration()`: Obtiene o crea configuración por defecto
- `updateConfiguration()`: Actualiza configuración completa
- `createInitialConfiguration()`: Crea configuración inicial (solo si no existe)
- `updateWorkDays()`: Actualiza días laborales
- `updatePaymentMethods()`: Actualiza métodos de pago
- `updateTaxRate()`: Actualiza tasa de IVA
- `isWorkDay()`: Verifica si un día es laboral
- `isPaymentMethodEnabled()`: Verifica si un método de pago está activo

**Validaciones:**
- Solo permite una configuración en el sistema
- Tax rate entre 0 y 100
- Días laborales no pueden estar vacíos
- Métodos de pago no pueden estar vacíos

#### `BusinessHoursService` / `BusinessHoursServiceImpl`
**Métodos principales:**
- `getAllBusinessHours()`: Lista todos los horarios
- `getBusinessHoursForDay()`: Obtiene horario de un día específico
- `saveBusinessHours()`: Guarda/actualiza horario
- `saveAllBusinessHours()`: Guarda múltiples horarios
- `updateBusinessHoursForDay()`: Actualiza horario de un día
- `isOpenAt()`: Verifica si está abierto en un día/hora específica
- `getActiveBusinessHours()`: Lista horarios de días activos
- `validateBusinessHoursWithWorkDays()`: Valida coherencia con días laborales

**Validaciones:**
- Los días de los horarios deben estar en los días laborales
- Hora de cierre debe ser posterior a hora de apertura
- No duplicar horarios para el mismo día

#### `SocialNetworkService` / `SocialNetworkServiceImpl`
**Métodos principales:**
- `getAllSocialNetworks()`: Lista todas las redes
- `getAllActiveSocialNetworks()`: Lista solo activas
- `createSocialNetwork()`: Crea nueva red social
- `updateSocialNetwork()`: Actualiza red existente
- `deleteSocialNetwork()`: Elimina red
- `activateSocialNetwork()`: Activa red
- `deactivateSocialNetwork()`: Desactiva red
- `reorderSocialNetworks()`: Reordena lista de redes
- `countActiveSocialNetworks()`: Cuenta redes activas

**Características:**
- Asigna automáticamente orden de visualización
- Permite reordenamiento flexible

### Controlador (Presentation Layer)

#### `SystemConfigurationController`
**Endpoints:**

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/admin/system-configuration` | Muestra formulario de configuración |
| POST | `/admin/system-configuration/update` | Actualiza configuración general |
| POST | `/admin/system-configuration/business-hours/update` | Actualiza horarios |
| GET | `/admin/system-configuration/social-networks/new` | Formulario nueva red social |
| POST | `/admin/system-configuration/social-networks/create` | Crea red social |
| GET | `/admin/system-configuration/social-networks/{id}/edit` | Formulario editar red |
| POST | `/admin/system-configuration/social-networks/{id}/update` | Actualiza red social |
| POST | `/admin/system-configuration/social-networks/{id}/delete` | Elimina red social |
| POST | `/admin/system-configuration/social-networks/{id}/toggle` | Activa/desactiva red |

**Seguridad:**
- Todos los endpoints requieren rol `ADMIN`
- Anotación: `@PreAuthorize("hasRole('ADMIN')")`

## Vistas (Templates)

### `admin/system-configuration/form.html`
Vista principal que contiene 3 secciones:

1. **Información General**
   - Formulario con datos básicos del restaurante
   - Selección de días laborales (checkboxes)
   - Switches para métodos de pago

2. **Horarios de Atención**
   - Tabla con los días laborales configurados
   - Inputs de tiempo para apertura/cierre
   - Checkbox para marcar día cerrado

3. **Redes Sociales**
   - Tabla listando redes configuradas
   - Botones para: Editar, Activar/Desactivar, Eliminar
   - Botón para agregar nueva red

### `admin/system-configuration/social-network-form.html`
Formulario modal/página para crear/editar redes sociales:
- Campos: Nombre, URL, Icono, Orden, Estado
- Vista previa del icono Font Awesome
- Validaciones en tiempo real

## Base de Datos

### Tablas Creadas

1. **`system_configuration`**
   - Tabla principal (debe tener solo 1 registro)
   - Campos: restaurant_name, slogan, logo_url, address, phone, email, tax_rate

2. **`system_work_days`**
   - Tabla de relación para días laborales
   - PK: (system_configuration_id, day_of_week)

3. **`system_payment_methods`**
   - Tabla de relación para métodos de pago
   - PK: (system_configuration_id, payment_method_type)
   - Campo adicional: enabled (boolean)

4. **`business_hours`**
   - Horarios por día
   - UK: (system_configuration_id, day_of_week)
   - Campos: open_time, close_time, is_closed

5. **`social_networks`**
   - Redes sociales
   - Campos: name, url, icon, display_order, active

### Script de Inicialización
Archivo: `database/init_system_configuration.sql`

**Contenido:**
- Creación de todas las tablas con constraints
- Inserción de configuración por defecto
- Datos de ejemplo para días laborales y métodos de pago
- Horarios por defecto (Lun-Vie: 8AM-8PM, Sáb: 8AM-3PM)

## Instalación y Uso

### 1. Ejecutar Script SQL
```bash
mysql -u root -p bd_restaurant < database/init_system_configuration.sql
```

### 2. Reiniciar Aplicación
La aplicación detectará automáticamente las nuevas entidades y repositorios.

### 3. Acceder al Módulo
1. Iniciar sesión como ADMIN
2. Navegar a: `/admin/system-configuration`
3. Configurar información del restaurante

### 4. Configuración Recomendada

**Paso 1: Información General**
- Completar todos los campos obligatorios
- Seleccionar días laborales
- Activar métodos de pago deseados
- Guardar

**Paso 2: Horarios**
- Configurar horarios para cada día laboral
- Marcar como cerrado si no se trabaja ese día
- Guardar horarios

**Paso 3: Redes Sociales**
- Agregar cada red social del restaurante
- Usar iconos de Font Awesome
- Ordenar según preferencia

## Validaciones Implementadas

### A nivel de Base de Datos
- Unique constraint en business_hours por día
- Foreign keys con CASCADE DELETE
- NOT NULL en campos obligatorios

### A nivel de Entidad (@PrePersist/@PreUpdate)
- Validación de horarios coherentes (cierre > apertura)
- Inicialización de métodos de pago por defecto

### A nivel de Servicio
- Validación de días laborales vs horarios
- Validación de singleton en configuración
- Validación de rangos (IVA 0-100)

### A nivel de Vista
- Validaciones HTML5 (required, type="email", type="url")
- Validaciones de formato de tiempo
- Confirmación para eliminaciones

## Patrones de Diseño Utilizados

1. **Singleton**: SystemConfiguration (solo una instancia)
2. **Repository Pattern**: Separación de lógica de acceso a datos
3. **Service Layer Pattern**: Lógica de negocio centralizada
4. **MVC**: Separación clara de capas
5. **Builder Pattern**: Construcción de entidades con Lombok
6. **Factory Pattern**: Creación de configuración por defecto

## Próximos Pasos (Módulo de Turnos)

Con esta configuración lista, ahora se puede implementar el módulo de Turnos (Shifts) que dependerá de:
- Los días laborales configurados en SystemConfiguration
- Los horarios de atención definidos

El módulo de Turnos validará que los turnos de empleados:
1. Solo se asignen en días laborales
2. Estén dentro de los horarios de atención
3. No se solapen entre sí

## Notas Técnicas

- **Transaccionalidad**: Todos los métodos de servicio usan `@Transactional`
- **Logging**: Implementado con SLF4J en todos los servicios y controladores
- **Excepciones**: Uso de `IllegalArgumentException` con mensajes en español
- **Lazy Loading**: Relaciones OneToMany configuradas como LAZY
- **Eager Loading**: ElementCollections configuradas como EAGER para evitar LazyInitializationException

## Testing Recomendado

### Tests Unitarios
- Validación de horarios en BusinessHours
- Singleton behavior en SystemConfigurationService
- Validaciones de días laborales

### Tests de Integración
- CRUD completo de configuración
- CRUD completo de horarios
- CRUD completo de redes sociales
- Validaciones de constraints en BD

### Tests E2E
- Flujo completo de configuración inicial
- Actualización de configuración existente
- Gestión de horarios y redes sociales

## Soporte y Mantenimiento

### Logs a Revisar
- Nivel DEBUG: Operaciones de consulta
- Nivel INFO: Operaciones de creación/actualización
- Nivel WARN: Validaciones fallidas
- Nivel ERROR: Excepciones y errores

### Métricas a Monitorear
- Tiempo de carga de configuración
- Frecuencia de actualizaciones
- Uso de métodos de pago
- Acceso a horarios

---

**Desarrollado por**: AA Tech Solutions  
**Versión**: 1.0.0  
**Fecha**: Octubre 2025
