# 🔐 SISTEMA DE LICENCIAS - GUÍA DE IMPLEMENTACIÓN

## ✅ Archivos Creados

### Backend (Java)
1. ✅ `SystemLicense.java` - Entidad principal de licencia
2. ✅ `LicenseEvent.java` - Entidad de eventos de licencia
3. ✅ `SystemError.java` - Entidad de errores del sistema
4. ✅ `SystemLicenseRepository.java` - Repositorio de licencias
5. ✅ `LicenseEventRepository.java` - Repositorio de eventos
6. ✅ `SystemErrorRepository.java` - Repositorio de errores
7. ✅ `LicenseService.java` - Servicio de lógica de negocio
8. ✅ `LicenseCheckJob.java` - Job programado (verificación diaria 9 AM)
9. ✅ `LicenseInterceptor.java` - Interceptor de validación
10. ✅ `ProgrammerController.java` - Controller del dashboard
11. ✅ `LicenseExpiredController.java` - Controller de página expirada
12. ✅ `Role.java` - Actualizado con rol PROGRAMMER
13. ✅ `SecurityConfig.java` - Actualizado con rutas del programador
14. ✅ `CustomAuthenticationSuccessHandler.java` - Actualizado para programador
15. ✅ `ElgransazonApplication.java` - Agregado @EnableScheduling

### Frontend (HTML)
1. ✅ `programmer/dashboard.html` - Dashboard completo del programador
2. ✅ `license-expired.html` - Página de licencia expirada

### Base de Datos
1. ✅ `CREATE_LICENSE_SYSTEM.sql` - Script completo de creación

---

## 📋 PASOS PARA ACTIVAR EL SISTEMA

### 1. Ejecutar el Script SQL

```bash
# Conéctate a tu base de datos MySQL
mysql -u root -p elgransazon

# Ejecuta el script
source CREATE_LICENSE_SYSTEM.sql
```

O desde tu IDE/cliente MySQL, ejecuta el contenido de `CREATE_LICENSE_SYSTEM.sql`

### 2. Actualizar Role en la Base de Datos

**Opción A: Cambiar a VARCHAR (Recomendado)**
```sql
-- Si tu columna role es ENUM, cámbiala a VARCHAR
ALTER TABLE employees MODIFY COLUMN role VARCHAR(50) NOT NULL;
```

**Opción B: Si es VARCHAR, ya está listo**
```sql
-- Verificar tipo de columna
DESCRIBE employees;
```

### 3. Crear Usuario Programador

```sql
INSERT INTO employees (
    nombre,
    apellido,
    username,
    email,
    password,
    telefono,
    role,
    enabled,
    fecha_registro
) VALUES (
    'Programador',
    'Sistema',
    'programador',
    'tu_email@tudominio.com',
    '$2a$10$YourHashedPasswordHere',  -- Ver abajo cómo generar
    '555-0000',
    'ROLE_PROGRAMMER',
    TRUE,
    NOW()
);
```

**Generar contraseña BCrypt:**
1. Usa [https://bcrypt-generator.com/](https://bcrypt-generator.com/)
2. O ejecuta este código Java:
```java
System.out.println(new BCryptPasswordEncoder().encode("tu_contraseña_aqui"));
```

### 4. Crear Licencia para el Restaurante

```sql
INSERT INTO system_license (
    license_key,
    package_type,
    billing_cycle,
    purchase_date,
    expiration_date,
    installation_date,
    status,
    owner_name,
    owner_email,
    owner_phone,
    owner_rfc,
    restaurant_name,
    max_users,
    max_branches,
    version
) VALUES (
    'ELGS-2026-DEMO-ABC123DEF456',  -- Genera una única
    'ECOMMERCE',  -- BASIC, WEB, o ECOMMERCE
    'MONTHLY',  -- MONTHLY o ANNUAL
    CURDATE(),
    DATE_ADD(CURDATE(), INTERVAL 30 DAY),  -- 30 días desde hoy
    CURDATE(),
    'ACTIVE',
    'Juan Pérez González',
    'cliente@restaurante.com',
    '+52 555-123-4567',
    'PEGJ850101XXX',
    'Restaurante Demo',
    5,  -- Máximo usuarios
    1,  -- Máximo sucursales
    '1.0.0'
);
```

### 5. Crear Rol PROGRAMMER en la tabla roles

```sql
-- Verificar si el rol existe
SELECT * FROM roles WHERE nombre_rol = 'ROLE_PROGRAMMER';

-- Si no existe, crearlo
INSERT INTO roles (nombre_rol) VALUES ('ROLE_PROGRAMMER');

-- Asignar el rol al usuario programador
INSERT INTO employee_roles (employee_id, role_id)
SELECT e.id_empleado, r.id_rol
FROM employees e, roles r
WHERE e.username = 'programador'
  AND r.nombre_rol = 'ROLE_PROGRAMMER';
```

### 6. Reiniciar la Aplicación

```bash
# Detener la aplicación si está corriendo
# Compilar y ejecutar
./mvnw clean install
./mvnw spring-boot:run
```

---

## 🎯 CÓMO USAR EL SISTEMA

### Como Programador (TÚ)

1. **Iniciar sesión:**
   - URL: `http://localhost:8080/login`
   - Usuario: `programador`
   - Contraseña: la que configuraste

2. **Dashboard del Programador:**
   - Ves automáticamente redirigido a `/programmer/dashboard`
   - Información completa de la licencia
   - Estadísticas del sistema
   - Errores registrados
   - Historial de eventos

3. **Acciones disponibles:**
   - ✅ Renovar licencia (1, 3, 6, 12 meses)
   - ⏸️ Suspender licencia
   - ▶️ Reactivar licencia
   - 📝 Agregar notas internas
   - 📊 Ver estadísticas del cliente

### Como Admin del Restaurante (Cliente)

1. **Login normal:**
   - URL: `http://localhost:8080/login`
   - Usuario admin del restaurante

2. **Notificaciones automáticas:**
   - **Mensual:** 5 días antes aparece SweetAlert
   - **Anual:** 30 días antes aparece SweetAlert
   - **Banner:** Cuando faltan 3 días o menos

3. **Si expira:**
   - Sistema bloquea acceso automáticamente
   - Redirige a página de "Licencia Expirada"
   - Debe contactar al programador para renovar

---

## 🔔 SISTEMA DE NOTIFICACIONES

### Job Programado (LicenseCheckJob)
- Se ejecuta diariamente a las **9:00 AM**
- Verifica estado de licencia
- Marca como expirada si corresponde
- Registra eventos en la base de datos

### Notificaciones al Cliente (Admin)
- **SweetAlert** al hacer login si está próxima a vencer
- **Badge en navbar** mostrando días restantes
- **Banner sticky** cuando faltan 3 días o menos
- **Bloqueo total** si está expirada

### Futuro: Email Notifications
En `LicenseCheckJob.java` están los TODO para agregar:
```java
// TODO: Send warning notification email
// TODO: Send expiration notification email
```

Puedes integrar con tu servicio de email existente.

---

## 🛠️ PERSONALIZACIÓN

### Cambiar horario del Job

En `LicenseCheckJob.java`:
```java
@Scheduled(cron = "0 0 9 * * *")  // Cambiar hora aquí
// Formato: segundo minuto hora día mes día-semana
// Ejemplo: "0 30 8 * * *" = 8:30 AM diario
```

### Cambiar días de notificación

En `SystemLicense.java`, método `needsNotification()`:
```java
// Mensual: actualmente 5 días
if (billingCycle == BillingCycle.MONTHLY && daysLeft <= 5)

// Anual: actualmente 30 días  
if (billingCycle == BillingCycle.ANNUAL && daysLeft <= 30)
```

### Modificar información de contacto

En `license-expired.html`:
```html
<a href="mailto:tu_email@tudominio.com">tu_email@tudominio.com</a>
<a href="tel:+525551234567">+52 555-123-4567</a>
```

---

## 🧪 PRUEBAS

### 1. Probar que el sistema bloquea cuando expira

```sql
-- Cambiar fecha de vencimiento a ayer
UPDATE system_license 
SET expiration_date = DATE_SUB(CURDATE(), INTERVAL 1 DAY),
    status = 'ACTIVE'
WHERE id = 1;
```

Ahora intenta acceder como admin → debe redirigir a `/license-expired`

### 2. Probar notificaciones

```sql
-- Cambiar a 3 días restantes
UPDATE system_license 
SET expiration_date = DATE_ADD(CURDATE(), INTERVAL 3 DAY)
WHERE id = 1;
```

Login como admin → debe aparecer SweetAlert

### 3. Ejecutar job manualmente

Desde el código, llama:
```java
@Autowired
private LicenseCheckJob licenseCheckJob;

// En algún método de prueba
licenseCheckJob.manualCheck();
```

---

## 📊 ESTRUCTURA DE LA BASE DE DATOS

```
system_license (1 fila por instalación)
├── Información básica
│   ├── license_key (único)
│   ├── package_type (BASIC/WEB/ECOMMERCE)
│   ├── billing_cycle (MONTHLY/ANNUAL)
│   └── status (ACTIVE/EXPIRED/TRIAL/SUSPENDED)
├── Fechas
│   ├── purchase_date
│   ├── expiration_date
│   ├── installation_date
│   ├── last_check_date
│   └── last_notification_sent
├── Información del cliente
│   ├── owner_name
│   ├── owner_email
│   ├── owner_phone
│   ├── owner_rfc
│   └── restaurant_name
├── Límites
│   ├── max_users
│   └── max_branches
└── Técnico
    ├── version
    └── notes

license_events (múltiples eventos)
├── event_type (CREATED/RENEWED/EXPIRED/etc.)
├── event_date
├── description
└── performed_by

system_errors (errores del sistema)
├── error_type
├── error_message
├── severity (LOW/MEDIUM/HIGH/CRITICAL)
├── resolved
└── occurred_at
```

---

## 🚀 PRÓXIMOS PASOS (Opcional)

1. **Integrar Email Service:**
   - Enviar email cuando falten X días
   - Enviar email cuando expire
   - Recordatorio semanal si está expirada

2. **Sistema de Pagos:**
   - Webhook de Stripe/PayPal
   - Renovación automática

3. **Múltiples Restaurantes (Futuro):**
   - Si vendes a muchos clientes
   - Dashboard centralizado
   - Panel multi-tenant

4. **Métricas Avanzadas:**
   - Tracking de uso por módulo
   - Estadísticas de rendimiento
   - Reportes automáticos al cliente

---

## ❓ SOLUCIÓN DE PROBLEMAS

### El job no se ejecuta
- Verifica que `@EnableScheduling` esté en `ElgransazonApplication`
- Revisa logs en consola
- Prueba con `licenseCheckJob.manualCheck()`

### No redirige a /license-expired
- Verifica que `LicenseInterceptor` esté registrado en `SecurityConfig`
- Revisa que la licencia esté realmente expirada en BD
- Checa los logs del interceptor

### No aparecen notificaciones al admin
- Verifica que `showLicenseWarning` esté en el modelo
- Checa que el template del admin tenga el código de SweetAlert
- Revisa si `daysLeft` es correcto en la sesión

### Usuario programador no puede acceder
- Verifica que el rol sea exactamente `ROLE_PROGRAMMER`
- Checa que esté en la tabla `employee_roles`
- Revisa `SecurityConfig` para la ruta `/programmer/**`

---

## 📞 SOPORTE

Sistema desarrollado por **AATech Solutions**

Para dudas o soporte:
- Email: soporte@elgransazon.com
- Tel: +52 555-123-4567

---

**¡Sistema de Licencias Implementado Exitosamente! 🎉**
