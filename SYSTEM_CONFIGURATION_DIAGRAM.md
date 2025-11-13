# Diagrama de Relaciones - SystemConfiguration

## 🏗️ Estructura Actual de Base de Datos

```
┌─────────────────────────────────────────────────────────────────┐
│                    SYSTEM_CONFIGURATION                         │
│─────────────────────────────────────────────────────────────────│
│ 🔑 id (PK)                    | BIGINT                          │
│ 📛 restaurant_name            | VARCHAR(100)                    │
│ 💬 slogan                     | VARCHAR(255)                    │
│ 🖼️  logo_url                   | VARCHAR(500)                    │
│ 📍 address                    | VARCHAR(500)                    │
│ 📞 phone                      | VARCHAR(20)                     │
│ 📧 email                      | VARCHAR(100)                    │
│ 💰 tax_rate                   | DECIMAL(5,2)                    │
│ ⏱️  avg_consumption_time      | INTEGER                         │
│ 📅 created_at                 | TIMESTAMP                       │
│ 📅 updated_at                 | TIMESTAMP                       │
└─────────────────────────────────────────────────────────────────┘
                           │
                           │ 1
                           │
        ┌──────────────────┼──────────────────────┬──────────────────┐
        │                  │                      │                  │
        │ *                │ *                    │ *                │ *
┌───────▼────────┐ ┌──────▼───────────┐ ┌────────▼──────────┐ ┌────▼────────────┐
│ WORK_DAYS      │ │ PAYMENT_METHODS  │ │ BUSINESS_HOURS    │ │ SOCIAL_NETWORKS │
│ (@Element)     │ │ (@Element)       │ │ (Entidad)         │ │ (Entidad)       │
└────────────────┘ └──────────────────┘ └───────────────────┘ └─────────────────┘
```

## 📋 Detalle de Tablas y Relaciones

### 1️⃣ SYSTEM_WORK_DAYS (Tabla automática - @ElementCollection)

```sql
CREATE TABLE system_work_days (
    system_configuration_id BIGINT NOT NULL,  -- FK
    day_of_week VARCHAR(20) NOT NULL,         -- ENUM
    PRIMARY KEY (system_configuration_id, day_of_week),
    FOREIGN KEY (system_configuration_id) REFERENCES system_configuration(id)
);
```

**Datos Ejemplo:**
```
┌─────────────────────────┬─────────────┐
│ system_configuration_id │ day_of_week │
├─────────────────────────┼─────────────┤
│ 1                       │ MONDAY      │
│ 1                       │ TUESDAY     │
│ 1                       │ WEDNESDAY   │
│ 1                       │ THURSDAY    │
│ 1                       │ FRIDAY      │
│ 1                       │ SATURDAY    │
└─────────────────────────┴─────────────┘
```

**Características:**
- ❌ NO tiene entidad Java
- ✅ Se genera automáticamente por JPA
- 📦 Es un `Set<DayOfWeek>` en Java
- 🎯 Representa: "¿Qué días opera el restaurante?"

---

### 2️⃣ SYSTEM_PAYMENT_METHODS (Tabla automática - @ElementCollection)

```sql
CREATE TABLE system_payment_methods (
    system_configuration_id BIGINT NOT NULL,     -- FK
    payment_method_type VARCHAR(20) NOT NULL,    -- ENUM (Key)
    enabled BOOLEAN NOT NULL,                    -- Value
    PRIMARY KEY (system_configuration_id, payment_method_type),
    FOREIGN KEY (system_configuration_id) REFERENCES system_configuration(id)
);
```

**Datos Ejemplo:**
```
┌─────────────────────────┬─────────────────────┬─────────┐
│ system_configuration_id │ payment_method_type │ enabled │
├─────────────────────────┼─────────────────────┼─────────┤
│ 1                       │ CASH                │ true    │
│ 1                       │ CREDIT_CARD         │ true    │
│ 1                       │ DEBIT_CARD          │ true    │
└─────────────────────────┴─────────────────────┴─────────┘
```

**Características:**
- ❌ NO tiene entidad Java
- ✅ Se genera automáticamente por JPA
- 📦 Es un `Map<PaymentMethodType, Boolean>` en Java
- 🎯 Representa: "¿Qué métodos de pago acepta?"

---

### 3️⃣ BUSINESS_HOURS (Entidad completa)

```sql
CREATE TABLE business_hours (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    system_configuration_id BIGINT NOT NULL,     -- FK
    day_of_week VARCHAR(20) NOT NULL,            -- ENUM
    open_time TIME NOT NULL,
    close_time TIME NOT NULL,
    is_closed BOOLEAN NOT NULL DEFAULT false,
    UNIQUE (system_configuration_id, day_of_week),
    FOREIGN KEY (system_configuration_id) REFERENCES system_configuration(id)
);
```

**Datos Ejemplo:**
```
┌────┬─────────────────────────┬─────────────┬───────────┬────────────┬───────────┐
│ id │ system_configuration_id │ day_of_week │ open_time │ close_time │ is_closed │
├────┼─────────────────────────┼─────────────┼───────────┼────────────┼───────────┤
│ 1  │ 1                       │ MONDAY      │ 08:00:00  │ 22:00:00   │ false     │
│ 2  │ 1                       │ TUESDAY     │ 08:00:00  │ 22:00:00   │ false     │
│ 3  │ 1                       │ WEDNESDAY   │ 08:00:00  │ 22:00:00   │ false     │
│ 4  │ 1                       │ THURSDAY    │ 08:00:00  │ 22:00:00   │ false     │
│ 5  │ 1                       │ FRIDAY      │ 08:00:00  │ 22:00:00   │ false     │
│ 6  │ 1                       │ SATURDAY    │ 08:00:00  │ 22:00:00   │ false     │
│ 7  │ 1                       │ SUNDAY      │ NULL      │ NULL       │ true      │
└────┴─────────────────────────┴─────────────┴───────────┴────────────┴───────────┘
```

**Características:**
- ✅ Tiene entidad Java: `BusinessHours.java`
- 📦 Es un `List<BusinessHours>` en Java
- 🔗 Relación `@ManyToOne` con SystemConfiguration
- 🎯 Representa: "¿A qué hora abre/cierra cada día?"

---

## 🔄 Flujo de Creación de Datos

### Cuando inicias la aplicación por primera vez:

```
┌────────────────────────────────────────────────────────────────────┐
│                      INICIO DE APLICACIÓN                          │
└────────────────────────┬───────────────────────────────────────────┘
                         │
                         ▼
         ┌───────────────────────────────────┐
         │ ¿Existe SystemConfiguration?     │
         └───────────┬───────────────────────┘
                     │
        ┌────────────┴────────────┐
        │ NO                      │ SÍ
        ▼                         ▼
┌───────────────────┐    ┌────────────────┐
│ Crear Config      │    │ Usar existente │
│ por defecto       │    └────────────────┘
└───────┬───────────┘
        │
        ├─► 1. Crear SystemConfiguration
        │      - restaurant_name = "Mi Restaurante"
        │      - tax_rate = 16.00
        │      - avg_consumption = 120 min
        │
        ├─► 2. Crear WorkDays (automático)
        │      INSERT INTO system_work_days:
        │      - MONDAY, TUESDAY, WEDNESDAY
        │      - THURSDAY, FRIDAY, SATURDAY
        │
        ├─► 3. Crear PaymentMethods (automático)
        │      INSERT INTO system_payment_methods:
        │      - CASH = true
        │      - CREDIT_CARD = true
        │      - DEBIT_CARD = true
        │
        └─► 4. Crear BusinessHours (NUEVO - ahora automático)
               INSERT INTO business_hours:
               - MONDAY    8:00-22:00 (abierto)
               - TUESDAY   8:00-22:00 (abierto)
               - WEDNESDAY 8:00-22:00 (abierto)
               - THURSDAY  8:00-22:00 (abierto)
               - FRIDAY    8:00-22:00 (abierto)
               - SATURDAY  8:00-22:00 (abierto)
               - SUNDAY    NULL-NULL  (cerrado)
```

---

## ⚠️ Problema de Diseño Actual

### Duplicación de información entre `system_work_days` y `business_hours`:

```
❌ PROBLEMA: Dos fuentes de verdad

┌──────────────────────┐        ┌──────────────────────┐
│  system_work_days    │        │  business_hours      │
│──────────────────────│        │──────────────────────│
│  MONDAY              │        │  MONDAY (abierto)    │
│  TUESDAY             │   ≠?   │  TUESDAY (abierto)   │
│  WEDNESDAY           │        │  WEDNESDAY (cerrado) │
│  THURSDAY            │        │  THURSDAY (abierto)  │
│  FRIDAY              │        │  ...                 │
└──────────────────────┘        └──────────────────────┘

Pueden estar desincronizados:
- Un día en work_days pero cerrado en business_hours
- Un día abierto en business_hours pero no en work_days
```

---

## ✅ Diseño Correcto Propuesto

### Opción A: Eliminar `system_work_days` (RECOMENDADO)

```
SystemConfiguration (1) ────→ (*) BusinessHours

Un día es "de trabajo" si:
  business_hours.is_closed = false
```

**Ventajas:**
- ✅ Una sola fuente de verdad
- ✅ No hay inconsistencias
- ✅ Más simple de mantener
- ✅ Menos tablas

**Cambios necesarios:**
1. Eliminar `Set<DayOfWeek> workDays` de `SystemConfiguration`
2. Usar `businessHours.stream().filter(h -> !h.getIsClosed())`
3. Eliminar tabla `system_work_days`

---

### Opción B: Crear entidad intermedia WorkDay

```
SystemConfiguration (1) ────→ (*) WorkDay (1) ────→ (1) BusinessHours
```

```java
@Entity
@Table(name = "work_days")
public class WorkDay {
    @Id
    @GeneratedValue
    private Long id;
    
    @ManyToOne
    private SystemConfiguration systemConfiguration;
    
    @Enumerated(EnumType.STRING)
    @Column(unique = true)
    private DayOfWeek dayOfWeek;
    
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "business_hours_id")
    private BusinessHours businessHours;  // puede ser null si cerrado
    
    private Boolean isActive = true;
}
```

**Ventajas:**
- ✅ Separación clara de conceptos
- ✅ Más flexible para futuras features
- ✅ Relación clara: WorkDay → BusinessHours

**Desventajas:**
- ❌ Más complejo
- ❌ Una tabla adicional
- ❌ Probablemente innecesario para este caso

---

## 🎯 Recomendación Final

### Para tu caso de uso actual: **Opción A**

1. **Mantén el diseño actual** (por ahora)
2. **Agrega creación automática** de BusinessHours (✅ Ya implementado)
3. **Considera refactorizar** en el futuro si crece la complejidad

### Cuando refactorizar:
- Si necesitas diferentes conceptos de "día de trabajo" vs "horario"
- Si necesitas validaciones complejas entre ambos
- Si el equipo crece y la duplicación causa bugs

---

## 📊 Comparación Visual

```
┌────────────────────────────────────────────────────────────────────────┐
│                        DISEÑO ACTUAL                                   │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  SystemConfiguration                                                   │
│    ├─► Set<DayOfWeek> workDays          (tabla: system_work_days)    │
│    │     └─► MONDAY, TUESDAY, WEDNESDAY... ❌ Solo días               │
│    │                                                                   │
│    └─► List<BusinessHours> businessHours (tabla: business_hours)     │
│          └─► MONDAY 8:00-22:00           ✅ Días + Horarios           │
│               TUESDAY 8:00-22:00                                       │
│               WEDNESDAY 8:00-22:00                                     │
│                                                                        │
│  ⚠️  Duplicación: ambos tienen información de días                     │
└────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────┐
│                    DISEÑO PROPUESTO (Simple)                           │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  SystemConfiguration                                                   │
│    └─► List<BusinessHours> businessHours (tabla: business_hours)     │
│          ├─► MONDAY 8:00-22:00 (is_closed=false) ✅ Día de trabajo    │
│          ├─► TUESDAY 8:00-22:00 (is_closed=false)                     │
│          ├─► WEDNESDAY 8:00-22:00 (is_closed=false)                   │
│          └─► SUNDAY NULL-NULL (is_closed=true)   ❌ No es día trabajo │
│                                                                        │
│  ✅ Una sola fuente de verdad                                          │
│  ✅ is_closed = false → Es día de trabajo                              │
└────────────────────────────────────────────────────────────────────────┘
```
