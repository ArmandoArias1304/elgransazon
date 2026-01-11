# Mejoras al Sistema de Licencias: Renovación Mejorada y Cambio de Paquete

## Resumen de Mejoras

Se implementaron dos mejoras importantes al sistema de licencias del programador:

1. **Modal de renovación mejorado** con información detallada y cálculos dinámicos
2. **Funcionalidad de cambio de paquete** para permitir upgrades (mejoras)

---

## 1. Modal de Renovación Mejorado

### Cambios Realizados

**Archivo modificado:** `dashboard.html`

#### Funcionalidades Agregadas:

1. **Información actual de la licencia:**
   - Fecha de vencimiento actual
   - Días restantes antes de expirar

2. **Preview en tiempo real:**
   - Nueva fecha de vencimiento calculada dinámicamente
   - Total de días de vigencia después de renovar
   - Se actualiza automáticamente al cambiar el periodo de renovación

3. **JavaScript para cálculos:**
```javascript
function calculateRenewal() {
    const months = parseInt(document.getElementById('renewMonths').value);
    const newDate = new Date(currentExpiration);
    newDate.setMonth(newDate.getMonth() + months);
    
    // Calcula y muestra la nueva fecha y días totales
}
```

### Vista Previa del Modal

```
┌─────────────────────────────────────────┐
│  🔄 Renovar Licencia                    │
├─────────────────────────────────────────┤
│                                         │
│  ℹ️ Información Actual:                 │
│  📅 Vence: 15/12/2024                   │
│  ⏰ Días restantes: 30 días             │
│                                         │
│  Renovar por: [▼ 12 meses (1 año)]     │
│                                         │
│  📊 Después de renovar:                 │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━    │
│  Nueva fecha de vencimiento:            │
│  15/12/2025                             │
│                                         │
│  Total de días de vigencia:             │
│  395 días                               │
│                                         │
│  [Cancelar]  [✓ Confirmar Renovación]  │
└─────────────────────────────────────────┘
```

---

## 2. Sistema de Cambio de Paquete

### Nuevos Componentes

#### A. Método en LicenseService

**Archivo:** `LicenseService.java`

```java
@Transactional
public void changePackageType(SystemLicense.PackageType newPackageType, String performedBy)
```

**Características:**
- Valida que solo se permitan mejoras (upgrades)
- Previene downgrade de paquetes
- Previene cambiar al mismo paquete
- Crea evento en el historial

**Reglas de Upgrade:**
```
BASIC → WEB ✅
BASIC → ECOMMERCE ✅
WEB → ECOMMERCE ✅

WEB → BASIC ❌ (downgrade no permitido)
ECOMMERCE → WEB ❌ (downgrade no permitido)
ECOMMERCE → BASIC ❌ (downgrade no permitido)
```

#### B. Endpoint en ProgrammerController

**Archivo:** `ProgrammerController.java`

```java
@PostMapping("/change-package")
public String changePackage(
    @RequestParam String packageType,
    Authentication authentication,
    RedirectAttributes redirectAttributes
)
```

**Características:**
- Valida enum de tipo de paquete
- Captura errores de negocio (downgrade, mismo paquete)
- Muestra mensajes de éxito/error apropiados

#### C. Modal de Cambio de Paquete

**Archivo:** `dashboard.html`

**Funcionalidades:**

1. **Muestra paquete actual**
2. **Dropdown inteligente:**
   - Solo muestra opciones válidas de upgrade
   - Si estás en BASIC: muestra WEB y ECOMMERCE
   - Si estás en WEB: solo muestra ECOMMERCE
   - Si estás en ECOMMERCE: no muestra opciones (ya es el máximo)

3. **Comparación de características:**
   - Se muestra al seleccionar un paquete
   - Lista todas las funcionalidades del paquete elegido

### Vista Previa del Modal

```
┌─────────────────────────────────────────┐
│  📦 Cambiar Paquete                     │
├─────────────────────────────────────────┤
│                                         │
│  ℹ️ Paquete Actual: BASIC              │
│                                         │
│  Nuevo Paquete:                         │
│  [▼ Selecciona un paquete...]          │
│      🌐 WEB - Con Landing Page         │
│      🛒 ECOMMERCE - Con Módulo Cliente │
│                                         │
│  Solo se permiten mejoras              │
│                                         │
│  📊 Características del nuevo paquete: │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━    │
│  • Sistema POS completo                │
│  • Landing Page para menú online       │
│  • Gestión de inventario               │
│  • Sistema de reservas                 │
│  • Reportes y estadísticas             │
│                                         │
│  ⚠️ El cambio es inmediato             │
│                                         │
│  [Cancelar]  [✓ Cambiar Paquete]      │
└─────────────────────────────────────────┘
```

### Nuevo Botón en Dashboard

Se agregó el botón "Cambiar Paquete" junto a los otros botones de acción:

```html
<button class="btn btn-info" data-bs-toggle="modal" data-bs-target="#changePackageModal">
    <i class="fas fa-box"></i> Cambiar Paquete
</button>
```

---

## Flujo de Usuario

### Renovar Licencia

1. Usuario hace clic en "Renovar Licencia"
2. Se abre el modal mostrando información actual
3. Usuario selecciona periodo (1, 3, 6 o 12 meses)
4. **Automáticamente** se calculan:
   - Nueva fecha de vencimiento
   - Total de días de vigencia
5. Usuario confirma la renovación
6. Sistema actualiza la licencia y registra evento

### Cambiar Paquete

1. Usuario hace clic en "Cambiar Paquete"
2. Se abre el modal mostrando paquete actual
3. Usuario ve **solo** las opciones de upgrade válidas
4. Al seleccionar un paquete, se muestran sus características
5. Usuario confirma el cambio
6. Sistema valida que sea un upgrade
7. Si es válido:
   - Cambia el paquete inmediatamente
   - Registra evento en historial
   - Muestra mensaje de éxito
8. Si no es válido:
   - Muestra error descriptivo
   - No realiza ningún cambio

---

## Validaciones Implementadas

### En el Backend (LicenseService)

```java
private boolean isValidUpgrade(PackageType current, PackageType newType) {
    if (current == PackageType.BASIC) {
        return newType == PackageType.WEB || newType == PackageType.ECOMMERCE;
    } else if (current == PackageType.WEB) {
        return newType == PackageType.ECOMMERCE;
    }
    return false; // ECOMMERCE no puede mejorar
}
```

### Mensajes de Error

- **Downgrade intentado:** "Solo se permiten mejoras de paquete. No puedes cambiar de X a Y"
- **Mismo paquete:** "El paquete ya es X"
- **Sin licencia:** "No existe una licencia en el sistema"

---

## Eventos Registrados

Ambas operaciones registran eventos en `license_events`:

### Renovación:
```sql
event_type = 'RENEWED'
description = 'Licencia renovada por X mes(es)'
performed_by = 'username'
```

### Cambio de Paquete:
```sql
event_type = 'UPDATED'
description = 'Paquete cambiado de BASIC a WEB'
performed_by = 'username'
```

---

## Impacto en el Sistema

### Efectos del Cambio de Paquete

Cuando se cambia de paquete, **inmediatamente** se aplican las restricciones:

#### BASIC → WEB
- ✅ Se habilita el acceso a la landing page (`/`)
- ❌ Módulo de clientes sigue bloqueado

#### WEB → ECOMMERCE (o BASIC → ECOMMERCE)
- ✅ Se habilita el módulo de clientes (`/client/**`)
- ✅ Aparecen botones de "¿Eres cliente?" en login
- ✅ Clientes pueden registrarse y hacer pedidos

### Archivos Modificados

1. **LicenseService.java**
   - Método `changePackageType()`
   - Método privado `isValidUpgrade()`

2. **ProgrammerController.java**
   - Endpoint POST `/programmer/change-package`

3. **dashboard.html**
   - Modal de renovación mejorado con cálculos
   - Modal de cambio de paquete nuevo
   - JavaScript para cálculos dinámicos
   - Botón "Cambiar Paquete" agregado

---

## Pruebas Sugeridas

### Test 1: Renovación
1. Iniciar sesión como PROGRAMMER
2. Ir a `/programmer/dashboard`
3. Click en "Renovar Licencia"
4. Verificar que se muestra fecha actual y días restantes
5. Cambiar el dropdown de meses
6. Verificar que se actualizan los cálculos automáticamente
7. Confirmar renovación
8. Verificar mensaje de éxito

### Test 2: Upgrade BASIC → WEB
1. Crear licencia con paquete BASIC
2. Verificar que `/` redirige a `/login`
3. Click en "Cambiar Paquete"
4. Seleccionar WEB
5. Verificar que se muestran características
6. Confirmar cambio
7. Refrescar página
8. Verificar que ahora `/` muestra landing page

### Test 3: Upgrade WEB → ECOMMERCE
1. Tener licencia con paquete WEB
2. Click en "Cambiar Paquete"
3. Seleccionar ECOMMERCE
4. Confirmar cambio
5. Cerrar sesión
6. Verificar que en `/login` aparecen botones de cliente
7. Navegar a `/client/login`
8. Verificar acceso permitido

### Test 4: Intento de Downgrade
1. Tener licencia con paquete ECOMMERCE
2. Modificar manualmente HTML para agregar opción BASIC
3. Intentar cambiar a BASIC
4. Verificar mensaje de error: "Solo se permiten mejoras"
5. Verificar que paquete NO cambió

---

## Mejoras Futuras Sugeridas

1. **Precio diferencial:** Cobrar la diferencia al hacer upgrade
2. **Confirmación adicional:** Requerir contraseña para cambio de paquete
3. **Notificación por email:** Enviar correo al cliente notificando el cambio
4. **Histórico de cambios:** Vista dedicada para ver todos los cambios de paquete
5. **Preview de funcionalidades:** Mostrar qué se habilitará/deshabilitará antes de confirmar

---

## Conclusión

Estas mejoras proporcionan:

✅ Mayor transparencia en las renovaciones (usuario sabe exactamente qué está renovando)
✅ Flexibilidad para mejorar el paquete sin crear nueva licencia
✅ Validaciones robustas que previenen operaciones no permitidas
✅ Mejor experiencia de usuario con información en tiempo real
✅ Trazabilidad completa en el historial de eventos

El sistema ahora permite al programador gestionar el ciclo de vida completo de las licencias de manera más eficiente y con mejor control.
