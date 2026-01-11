# 🔐 PATRÓN SINGLETON PARA LICENCIAS

## 📋 Resumen

El sistema de licencias ahora utiliza el **patrón Singleton** para garantizar que solo exista **UNA ÚNICA LICENCIA** en todo el sistema, similar a cómo funciona `SystemConfiguration`.

---

## ✅ Implementación Completa

### 1. **LicenseInitializer.java** - Inicialización Automática

**Ubicación:** `infrastructure/init/LicenseInitializer.java`

```java
@Component
@Order(1)
public class LicenseInitializer implements CommandLineRunner {
    // Se ejecuta al iniciar la aplicación
    // Verifica si existe una licencia
    // Si NO existe, crea una licencia predeterminada
}
```

**Características de la Licencia Predeterminada:**

- ✅ Clave única generada: `ELGS-2026-DEMO-XXXXXXXXXXXX`
- ✅ Paquete: **ECOMMERCE** (completo, con todos los módulos)
- ✅ Ciclo: **ANUAL**
- ✅ Validez: **1 año** desde la instalación
- ✅ Estado: **ACTIVA**
- ✅ Usuarios: **Sin límite** (null = ilimitado)
- ✅ Sucursales: **5**
- ✅ Propietario Demo: "El Gran Sazón - Propietario Demo"
- ✅ Email: admin@elgransazon.com
- ✅ Teléfono: +52 33 1234 5678

### 2. **LicenseService.java** - Métodos Singleton

**Métodos clave:**

```java
// Obtener LA licencia (solo hay una)
public SystemLicense getLicense() {
    return licenseRepository.findFirstByOrderByIdAsc().orElse(null);
}

// Obtener o crear (asegura que siempre haya una)
public SystemLicense getOrCreateLicense() {
    // Retorna la licencia existente
    // Si no existe, el inicializador la crea
}

// Aplicar Singleton: Prevenir múltiples licencias
private void enforceSingleton() {
    long count = licenseRepository.count();
    if (count > 0) {
        throw new IllegalStateException(
            "License already exists. Only one license allowed."
        );
    }
}
```

### 3. **Protección contra Duplicados**

Todos los métodos de creación de licencia están protegidos:

```java
@Transactional
public SystemLicense createLicense(...) {
    enforceSingleton(); // ❌ Lanza excepción si ya existe una
    // ... crear licencia
}

@Transactional
public SystemLicense createInitialLicense(...) {
    enforceSingleton(); // ❌ Lanza excepción si ya existe una
    // ... crear licencia
}
```

---

## 🎯 Flujo de Inicialización

```
┌─────────────────────────────────────────────────────┐
│ 1. Aplicación inicia (Spring Boot)                 │
└────────────┬────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────┐
│ 2. LicenseInitializer se ejecuta (@Order(1))       │
│    - Verifica: licenseRepository.count() > 0?      │
└────────────┬────────────────────────────────────────┘
             │
      ┌──────┴──────┐
      │             │
      ▼             ▼
 ┌─────────┐  ┌──────────────────────────────────┐
 │ SÍ      │  │ NO                               │
 │ existe  │  │ - Genera clave única             │
 └─────────┘  │ - Crea licencia predeterminada   │
      │       │ - Guarda en BD                   │
      │       │ - Crea evento inicial            │
      │       └──────────────────────────────────┘
      │                    │
      └────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│ 3. Sistema listo con UNA licencia activa           │
└─────────────────────────────────────────────────────┘
```

---

## 📊 Comparación con SystemConfiguration

| Aspecto           | SystemConfiguration              | SystemLicense        |
| ----------------- | -------------------------------- | -------------------- |
| **Patrón**        | Singleton                        | Singleton            |
| **Inicializador** | `SystemConfigurationInitializer` | `LicenseInitializer` |
| **Cantidad**      | 1 configuración                  | 1 licencia           |
| **Tabla BD**      | `system_configuration`           | `system_license`     |
| **Orden**         | `@Order(2)`                      | `@Order(1)`          |
| **Método Get**    | `getConfiguration()`             | `getLicense()`       |
| **Auto-creación** | ✅ Sí                            | ✅ Sí                |

---

## 🚀 Ventajas del Patrón Singleton

### ✅ Consistencia Total

- Solo una fuente de verdad para la licencia
- No hay ambigüedad sobre qué licencia usar
- Evita conflictos y duplicados

### ✅ Inicialización Automática

- No requiere intervención manual
- Sistema funcional desde el primer arranque
- Licencia predeterminada con datos reales

### ✅ Protección contra Errores

- Imposible crear múltiples licencias accidentalmente
- Métodos `enforceSingleton()` previenen duplicados
- Excepciones claras si se intenta violar el patrón

### ✅ Facilita el Desarrollo

- No necesitas crear licencia manualmente
- Dashboard del programador siempre tiene datos
- Testing más sencillo

---

## 🔧 Uso en el Código

### En Servicios:

```java
@Service
public class MyService {
    @Autowired
    private LicenseService licenseService;

    public void myMethod() {
        SystemLicense license = licenseService.getLicense();
        if (license != null) {
            // Usar la licencia
        }
    }
}
```

### En Controladores:

```java
@GetMapping("/dashboard")
public String dashboard(Model model) {
    SystemLicense license = licenseService.getLicense();
    model.addAttribute("license", license);
    model.addAttribute("noLicense", license == null);
    return "dashboard";
}
```

---

## ⚠️ Restricciones Importantes

### ❌ NO se puede:

- Crear múltiples licencias
- Eliminar la licencia sin recrearla
- Tener el sistema sin licencia (se auto-crea)

### ✅ SÍ se puede:

- Renovar la licencia existente
- Cambiar el paquete de la licencia
- Suspender/Reactivar la licencia
- Actualizar información del cliente
- Modificar notas internas

---

## 🗄️ Base de Datos

### Tabla: `system_license`

```sql
-- Solo debe haber 1 fila en esta tabla en todo momento
SELECT COUNT(*) FROM system_license; -- Resultado: 1

-- Licencia predeterminada creada automáticamente
SELECT
    license_key,
    package_type,
    status,
    expiration_date,
    max_users,
    restaurant_name
FROM system_license;
```

**Resultado esperado:**

```
license_key: ELGS-2026-DEMO-XXXXXXXXXXXX
package_type: ECOMMERCE
status: ACTIVE
expiration_date: 2027-01-10
max_users: NULL (ilimitado)
restaurant_name: El Gran Sazón - Restaurante Demo
```

---

## 📝 Eventos de Licencia

Al crear la licencia predeterminada, se registra un evento inicial:

```sql
SELECT * FROM license_events WHERE event_type = 'CREATED';
```

**Contenido:**

- **Tipo:** CREATED
- **Descripción:** "Licencia predeterminada creada automáticamente..."
- **Realizado por:** SYSTEM
- **Meses:** 12

---

## 🧪 Testing

### Verificar que funciona:

1. **Detener la aplicación**
2. **Eliminar la licencia** (si existe):
   ```sql
   DELETE FROM license_events;
   DELETE FROM system_license;
   ```
3. **Iniciar la aplicación**
4. **Verificar en logs:**
   ```
   Checking if default license needs to be created...
   No license found. Creating default license...
   ✅ Default license created successfully: ELGS-2026-DEMO-XXXXXXXXXXXX
      - Package: ECOMMERCE (E-Commerce Total)
      - Valid until: 2027-01-10
      - Users: Sin límite
      - Branches: 5
   ```
5. **Acceder a `/programmer/dashboard`**
6. **Verificar que se muestran todos los datos**

---

## 🎓 Documentos Relacionados

- `GUIA_SISTEMA_LICENCIAS.md` - Guía completa del sistema de licencias
- `SystemConfigurationInitializer.java` - Patrón similar para configuración
- `LicenseService.java` - Lógica de negocio de licencias

---

## ✨ Resumen

El patrón Singleton para licencias garantiza:

✅ **UNA y solo UNA licencia en el sistema**  
✅ **Creación automática al iniciar**  
✅ **Licencia predeterminada con datos reales**  
✅ **Protección contra duplicados**  
✅ **Funcionalidad completa desde el arranque**

🎯 **Resultado:** Sistema consistente, predecible y fácil de usar.
