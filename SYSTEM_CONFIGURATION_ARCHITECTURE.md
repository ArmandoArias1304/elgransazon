# Arquitectura de SystemConfiguration - Explicación Completa

## 📊 Estructura Actual de Base de Datos

### Tablas creadas automáticamente por JPA:

1. **`system_configuration`** - Tabla principal
2. **`system_work_days`** - Tabla intermedia (NO es entidad, es @ElementCollection)
3. **`system_payment_methods`** - Tabla intermedia (NO es entidad, es @ElementCollection)
4. **`business_hours`** - Tabla entidad completa

```
┌─────────────────────────────┐
│  system_configuration       │
│─────────────────────────────│
│  id (PK)                    │
│  restaurant_name            │
│  address                    │
│  phone                      │
│  email                      │
│  tax_rate                   │
│  avg_consumption_time       │
│  ...                        │
└─────────────────────────────┘
         │ 1
         │
         ├──────────────────────────────┐
         │                              │
         │ *                            │ *
┌────────▼─────────────┐      ┌────────▼──────────────┐
│  system_work_days    │      │  business_hours       │
│──────────────────────│      │───────────────────────│
│  system_config_id(FK)│      │  id (PK)              │
│  day_of_week (ENUM)  │      │  system_config_id(FK) │
└──────────────────────┘      │  day_of_week (ENUM)   │
                              │  open_time            │
                              │  close_time           │
                              │  is_closed            │
                              └───────────────────────┘
```

## 🔍 Relaciones y Conceptos

### 1. **`system_work_days`** (Tabla sin Entidad)

**¿Por qué no hay entidad en código?**
- Es un `@ElementCollection`, NO una entidad separada
- JPA la crea automáticamente
- Solo almacena valores ENUM (MONDAY, TUESDAY, etc.)
- Representa: "¿Qué días trabaja el restaurante?"

```java
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "system_work_days", 
                 joinColumns = @JoinColumn(name = "system_configuration_id"))
@Enumerated(EnumType.STRING)
@Column(name = "day_of_week", nullable = false)
private Set<DayOfWeek> workDays = new HashSet<>();
```

**Ejemplo de datos:**
```
system_configuration_id | day_of_week
------------------------|------------
1                      | MONDAY
1                      | TUESDAY
1                      | WEDNESDAY
1                      | THURSDAY
1                      | FRIDAY
1                      | SATURDAY
```

### 2. **`BusinessHours`** (Entidad Completa)

**¿Por qué pide `system_configuration_id`?**
- Es una entidad completa con relación `@ManyToOne`
- Cada `BusinessHours` pertenece a UNA configuración
- Representa: "¿A qué hora abre/cierra cada día?"

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "system_configuration_id", nullable = false)
private SystemConfiguration systemConfiguration;
```

**Ejemplo de datos:**
```
id | system_config_id | day_of_week | open_time | close_time | is_closed
---|------------------|-------------|-----------|------------|----------
1  | 1                | MONDAY      | 08:00     | 22:00      | false
2  | 1                | TUESDAY     | 08:00     | 22:00      | false
3  | 1                | SUNDAY      | NULL      | NULL       | true
```

## ⚠️ Problema del Diseño Actual

### El diseño actual tiene DUPLICACIÓN de información:

1. **`system_work_days`**: Dice "trabajamos Lunes, Martes, Miércoles..."
2. **`business_hours`**: También tiene los días con horarios

**Esto causa inconsistencias:**
- Un día puede estar en `work_days` pero NO en `business_hours`
- Un día puede estar en `business_hours` pero NO en `work_days`

## ✅ Diseño Correcto - DOS OPCIONES

### **Opción A: Diseño Simple (RECOMENDADO)**

**Una configuración tiene varios BusinessHours directamente**

```
SystemConfiguration (1) ──→ (*) BusinessHours
```

- **ELIMINAR** `system_work_days`
- Un día es "día de trabajo" si existe un `BusinessHours` con `is_closed = false`
- Más simple, sin duplicación

**Ventajas:**
- Una sola fuente de verdad
- No hay inconsistencias
- Más fácil de mantener

### **Opción B: Diseño con Entidad Intermedia**

**Una configuración tiene WorkDays, cada WorkDay tiene BusinessHours**

```
SystemConfiguration (1) ──→ (*) WorkDay (1) ──→ (1) BusinessHours
```

Crear nueva entidad `WorkDay`:
```java
@Entity
public class WorkDay {
    @Id
    private Long id;
    
    @ManyToOne
    private SystemConfiguration systemConfiguration;
    
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;
    
    @OneToOne(cascade = CascadeType.ALL)
    private BusinessHours businessHours; // Puede ser null si está cerrado
}
```

**Ventajas:**
- Separación clara de conceptos
- Más flexible para futuras features

**Desventajas:**
- Más complejo
- Probablemente innecesario para este caso

## 🚀 Flujo de Creación Actual

### ¿Qué se crea automáticamente al iniciar la app?

**Cuando la app inicia:**
1. JPA crea las tablas si no existen
2. `SystemConfigurationServiceImpl.getConfiguration()` se llama
3. Si no hay configuración, crea una por defecto:
   - Crea `SystemConfiguration`
   - Agrega días a `workDays` (se insertan en `system_work_days`)
   - Agrega métodos de pago a `paymentMethods` (se insertan en `system_payment_methods`)
4. **NO se crean automáticamente los `BusinessHours`**

### ¿Qué debes crear manualmente?

**Actualmente:**
1. ✅ `SystemConfiguration` - Se crea automáticamente
2. ✅ `workDays` - Se crean automáticamente
3. ❌ `BusinessHours` - **Debes crearlos manualmente**

Por eso tienes el error cuando la BD está vacía!

## 🛠️ Solución Recomendada

### Modificar el código para crear `BusinessHours` automáticamente:

```java
// En SystemConfigurationServiceImpl
private SystemConfiguration createDefaultConfiguration() {
    log.info("Creating default system configuration");
    
    Set<DayOfWeek> defaultWorkDays = new HashSet<>(Arrays.asList(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
    ));
    
    Map<PaymentMethodType, Boolean> defaultPaymentMethods = new HashMap<>();
    defaultPaymentMethods.put(PaymentMethodType.CASH, true);
    defaultPaymentMethods.put(PaymentMethodType.CREDIT_CARD, true);
    defaultPaymentMethods.put(PaymentMethodType.DEBIT_CARD, true);
    
    SystemConfiguration defaultConfig = SystemConfiguration.builder()
            .restaurantName("Mi Restaurante")
            .slogan("El mejor sabor de la ciudad")
            .address("Dirección no configurada")
            .phone("0000-0000")
            .email("contacto@restaurant.com")
            .taxRate(new BigDecimal("16.00"))
            .workDays(defaultWorkDays)
            .paymentMethods(defaultPaymentMethods)
            .build();
    
    SystemConfiguration saved = configurationRepository.save(defaultConfig);
    
    // ⭐ AGREGAR ESTO: Crear BusinessHours por defecto
    for (DayOfWeek day : DayOfWeek.values()) {
        BusinessHours hours = BusinessHours.builder()
                .dayOfWeek(day)
                .openTime(LocalTime.of(8, 0))   // 8:00 AM
                .closeTime(LocalTime.of(22, 0))  // 10:00 PM
                .isClosed(!defaultWorkDays.contains(day)) // Cerrado si no es día de trabajo
                .systemConfiguration(saved)
                .build();
        saved.addBusinessHours(hours);
    }
    
    return configurationRepository.save(saved);
}
```

## 📝 Resumen de Respuestas a tus Preguntas

### 1. **¿Cuál es la relación?**
```
SystemConfiguration
├── Set<DayOfWeek> workDays (@ElementCollection → tabla system_work_days)
│   └── Solo valores ENUM, sin entidad
│
└── List<BusinessHours> businessHours (@OneToMany)
    └── Entidades completas con horarios
```

### 2. **¿Qué debería crear primero?**
1. `SystemConfiguration` (se crea automáticamente)
2. `BusinessHours` (actualmente manual, debería ser automático)

### 3. **¿Por qué pide `system_configuration_id` en BusinessHours?**
Porque `BusinessHours` es una entidad con relación `@ManyToOne` a `SystemConfiguration`.
NO hay entidad `WorkDay` intermedia actualmente.

### 4. **¿Debería ser diferente?**
**SÍ**, recomiendo:
- **Eliminar** `system_work_days` (@ElementCollection workDays)
- **Usar solo** `BusinessHours` como fuente única de verdad
- Un día es "de trabajo" si `BusinessHours.isClosed = false`

### 5. **¿Cómo está actualmente?**
```
1 SystemConfiguration → N WorkDays (valores ENUM)
1 SystemConfiguration → N BusinessHours (entidades completas)
```
**No hay relación directa entre WorkDays y BusinessHours** ⚠️

### 6. **¿Cómo debería estar?**
```
Opción Simple (recomendada):
1 SystemConfiguration → N BusinessHours
```

## 🎯 Acción Inmediata

Para solucionar tu problema actual sin refactorizar todo:
- Agrega el código que te mostré arriba para crear `BusinessHours` automáticamente
- Así cuando la BD esté vacía, se crearán los horarios por defecto
- Ya no tendrás el error del `businessHoursMap` null
