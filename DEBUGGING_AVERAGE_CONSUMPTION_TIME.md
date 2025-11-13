# Debugging: averageConsumptionTimeMinutes no se actualiza en BD

## 🔍 Problema Reportado

El campo `averageConsumptionTimeMinutes` no se actualiza en la base de datos cuando se edita desde la vista Thymeleaf de configuración del sistema.

---

## ✅ Verificación del Código

### 1. Template HTML (form.html) - ✅ CORRECTO

```html
<!-- Línea 521 -->
<input
  type="number"
  id="averageConsumptionTimeMinutes"
  th:field="*{averageConsumptionTimeMinutes}"
  min="30"
  max="480"
  required
  class="..."
/>
```

**Estado:** ✅ El campo está correctamente enlazado con `th:field="*{averageConsumptionTimeMinutes}"`

---

### 2. Entidad SystemConfiguration.java - ✅ CORRECTO

```java
@NotNull(message = "Average consumption time is required")
@Min(value = 30, message = "Average consumption time must be at least 30 minutes")
@Max(value = 480, message = "Average consumption time cannot exceed 480 minutes (8 hours)")
@Column(name = "average_consumption_time_minutes", nullable = false)
@Builder.Default
private Integer averageConsumptionTimeMinutes = 120; // Default: 2 hours
```

**Estado:** ✅ Campo definido correctamente con validaciones

---

### 3. Controller - ✅ CORRECTO

```java
@PostMapping("/update")
public String updateConfiguration(
        @Valid @ModelAttribute("configuration") SystemConfiguration configuration,
        BindingResult bindingResult,
        // ...
```

**Estado:** ✅ Usa `@ModelAttribute` que debería enlazar automáticamente todos los campos del formulario

**Agregado logging:**
```java
log.debug("Received averageConsumptionTimeMinutes: {}", configuration.getAverageConsumptionTimeMinutes());
```

---

### 4. Service Implementation - ✅ CORRECTO

```java
@Override
public SystemConfiguration updateConfiguration(SystemConfiguration configuration) {
    log.info("Updating system configuration");
    
    SystemConfiguration existingConfig = configurationRepository.findFirstConfiguration()
            .orElseThrow(() -> new IllegalStateException("System configuration not found"));
    
    // Update fields
    existingConfig.setRestaurantName(configuration.getRestaurantName());
    existingConfig.setSlogan(configuration.getSlogan());
    existingConfig.setLogoUrl(configuration.getLogoUrl());
    existingConfig.setAddress(configuration.getAddress());
    existingConfig.setPhone(configuration.getPhone());
    existingConfig.setEmail(configuration.getEmail());
    existingConfig.setTaxRate(configuration.getTaxRate());
    existingConfig.setAverageConsumptionTimeMinutes(configuration.getAverageConsumptionTimeMinutes()); // ✅ SÍ se copia
    
    if (configuration.getPaymentMethods() != null) {
        existingConfig.setPaymentMethods(configuration.getPaymentMethods());
    }
    
    SystemConfiguration saved = configurationRepository.save(existingConfig);
    log.info("System configuration updated successfully");
    return saved;
}
```

**Estado:** ✅ El valor SÍ se copia de `configuration` a `existingConfig`

**Agregado logging:**
```java
log.debug("Input averageConsumptionTimeMinutes: {}", configuration.getAverageConsumptionTimeMinutes());
log.debug("Existing averageConsumptionTimeMinutes before update: {}", existingConfig.getAverageConsumptionTimeMinutes());
log.debug("Existing averageConsumptionTimeMinutes after update: {}", existingConfig.getAverageConsumptionTimeMinutes());
```

---

## 🐛 Posibles Causas del Problema

### Hipótesis 1: El formulario no envía el valor
**Síntoma:** El input no se incluye en el POST request
**Causas posibles:**
- Campo deshabilitado (`disabled`)
- JavaScript que interfiere con el formulario
- Error de binding en Thymeleaf

**Cómo verificar:**
1. Abrir DevTools del navegador → Network
2. Enviar el formulario
3. Ver el payload del request POST
4. Verificar que `averageConsumptionTimeMinutes` esté presente

**Solución si aplica:**
```html
<!-- Asegurar que el campo NO tenga disabled -->
<input
  type="number"
  id="averageConsumptionTimeMinutes"
  th:field="*{averageConsumptionTimeMinutes}"
  min="30"
  max="480"
  required
  <!-- NO debe tener: disabled -->
/>
```

---

### Hipótesis 2: Validación falla silenciosamente
**Síntoma:** Bean validation rechaza el valor pero no se muestra error
**Causas posibles:**
- Valor fuera del rango (min=30, max=480)
- Valor null cuando es @NotNull
- Error en `BindingResult` que no se muestra en vista

**Cómo verificar:**
```java
// En el controller
if (bindingResult.hasErrors()) {
    log.warn("Validation errors on configuration update");
    log.warn("Field errors: {}", bindingResult.getFieldErrors()); // Ver errores específicos
    // ...
}
```

**Solución si aplica:**
Mostrar error en el template:
```html
<p
  class="mt-1 text-sm text-red-600 dark:text-red-400"
  th:if="${#fields.hasErrors('averageConsumptionTimeMinutes')}"
  th:errors="*{averageConsumptionTimeMinutes}"
></p>
```
**Nota:** ✅ Ya existe en el template (línea 530-534)

---

### Hipótesis 3: Transacción no se commitea
**Síntoma:** El save() se ejecuta pero no persiste en BD
**Causas posibles:**
- Excepción después del save() que hace rollback
- Falta `@Transactional` en el método
- Configuración incorrecta de JPA/Hibernate

**Cómo verificar:**
```java
// Ver logs de Hibernate
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

**Solución si aplica:**
```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional  // ✅ Ya está presente
public class SystemConfigurationServiceImpl implements SystemConfigurationService {
    // ...
}
```

---

### Hipótesis 4: Spring usa constructor o setter incorrectamente
**Síntoma:** @ModelAttribute no enlaza el campo correctamente
**Causas posibles:**
- Falta getter/setter en la entidad
- Lombok no genera métodos correctamente
- Conflicto con @Builder

**Cómo verificar:**
```java
// Verificar que existan:
configuration.getAverageConsumptionTimeMinutes() // getter
configuration.setAverageConsumptionTimeMinutes(120) // setter
```

**Estado actual:**
```java
@Getter  // ✅ Genera getters
@Setter  // ✅ Genera setters
@Builder // ⚠️ Puede causar problemas con @ModelAttribute
public class SystemConfiguration {
    // ...
}
```

**Solución si aplica:**
Agregar `@AllArgsConstructor` y `@NoArgsConstructor`:
```java
@Entity
@Getter
@Setter
@NoArgsConstructor  // ✅ Ya está
@AllArgsConstructor // ✅ Ya está
@Builder
public class SystemConfiguration {
    // ...
}
```

---

## 🔧 Pasos para Depurar

### Paso 1: Verificar que el formulario envía el valor

1. **Abrir la aplicación en el navegador**
2. **Ir a Configuración del Sistema**
3. **Abrir DevTools (F12) → Pestaña Network**
4. **Cambiar el valor de "Tiempo Promedio de Consumo"** (ej: de 120 a 150)
5. **Hacer clic en "Guardar Configuración General"**
6. **Ver el request POST en Network:**
   - Click en el request `/admin/system-configuration/update`
   - Ir a la pestaña "Payload" o "Request"
   - Buscar `averageConsumptionTimeMinutes`
   - **¿Está presente?** → Sí = Continuar con Paso 2
   - **¿NO está presente?** → Problema en el formulario HTML

**Si NO está presente:**
```html
<!-- Verificar que el input NO tenga atributo 'disabled' -->
<!-- Verificar que esté DENTRO del <form> -->
<!-- Verificar que el nombre sea exactamente 'averageConsumptionTimeMinutes' -->
```

---

### Paso 2: Verificar logs del controller

**Con el logging agregado, revisar logs:**

```
INFO  - Processing system configuration update
DEBUG - Received averageConsumptionTimeMinutes: <VALOR>
```

**Análisis:**
- Si `<VALOR>` es el correcto → Controller recibe bien el dato, continuar con Paso 3
- Si `<VALOR>` es null → Problema de binding en Spring MVC
- Si `<VALOR>` es incorrecto → Problema en formulario o parsing

---

### Paso 3: Verificar logs del service

```
INFO  - Updating system configuration
DEBUG - Input averageConsumptionTimeMinutes: <VALOR_INPUT>
DEBUG - Existing averageConsumptionTimeMinutes before update: <VALOR_ANTES>
DEBUG - Existing averageConsumptionTimeMinutes after update: <VALOR_DESPUES>
INFO  - System configuration updated successfully
```

**Análisis:**
- Si `VALOR_DESPUES` = `VALOR_INPUT` → Service actualiza correctamente
- Si `VALOR_DESPUES` ≠ `VALOR_INPUT` → Problema en el setAverageConsumptionTimeMinutes()
- Si no aparece "System configuration updated successfully" → Excepción en el save()

---

### Paso 4: Verificar persistencia en BD

**Ejecutar query SQL directamente:**
```sql
SELECT id, restaurant_name, average_consumption_time_minutes, updated_at
FROM system_configuration
ORDER BY id DESC
LIMIT 1;
```

**Análisis:**
- Si `average_consumption_time_minutes` cambió → Problema resuelto
- Si NO cambió pero `updated_at` cambió → Solo ese campo no se persiste (problema de JPA)
- Si nada cambió → Transacción no se commitea

---

## 🎯 Soluciones Potenciales

### Solución 1: Verificar campo no esté disabled

```html
<input
  type="number"
  id="averageConsumptionTimeMinutes"
  th:field="*{averageConsumptionTimeMinutes}"
  min="30"
  max="480"
  required
  <!-- Asegurar que NO tenga: disabled, readonly -->
  class="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 text-gray-900 dark:text-white focus:border-primary focus:ring-2 focus:ring-primary focus:outline-none"
/>
```

---

### Solución 2: Usar @RequestParam explícito (alternativa)

Si el binding automático falla, usar:

```java
@PostMapping("/update")
public String updateConfiguration(
        @Valid @ModelAttribute("configuration") SystemConfiguration configuration,
        BindingResult bindingResult,
        @RequestParam(value = "paymentCash", required = false) Boolean paymentCash,
        @RequestParam(value = "paymentCreditCard", required = false) Boolean paymentCreditCard,
        @RequestParam(value = "paymentDebitCard", required = false) Boolean paymentDebitCard,
        @RequestParam(value = "averageConsumptionTimeMinutes", required = false) Integer avgTime, // Nuevo
        RedirectAttributes redirectAttributes,
        Model model) {

    log.info("Processing system configuration update");
    log.debug("Received averageConsumptionTimeMinutes from @RequestParam: {}", avgTime);
    
    // Si el @ModelAttribute no enlaza, usar el @RequestParam
    if (avgTime != null && avgTime > 0) {
        configuration.setAverageConsumptionTimeMinutes(avgTime);
    }
    
    // ... resto del código
```

---

### Solución 3: Forzar flush de JPA

```java
@Override
public SystemConfiguration updateConfiguration(SystemConfiguration configuration) {
    log.info("Updating system configuration");
    
    SystemConfiguration existingConfig = configurationRepository.findFirstConfiguration()
            .orElseThrow(() -> new IllegalStateException("System configuration not found"));
    
    // Update fields
    existingConfig.setRestaurantName(configuration.getRestaurantName());
    // ... otros campos ...
    existingConfig.setAverageConsumptionTimeMinutes(configuration.getAverageConsumptionTimeMinutes());
    
    if (configuration.getPaymentMethods() != null) {
        existingConfig.setPaymentMethods(configuration.getPaymentMethods());
    }
    
    SystemConfiguration saved = configurationRepository.save(existingConfig);
    configurationRepository.flush(); // Forzar escritura a BD
    
    log.info("System configuration updated successfully");
    return saved;
}
```

---

### Solución 4: Verificar mapeo JPA

Verificar que la columna exista en BD:

```sql
-- Verificar estructura de tabla
DESCRIBE system_configuration;

-- O en PostgreSQL:
\d system_configuration;

-- Verificar que exista la columna
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'system_configuration' 
AND column_name = 'average_consumption_time_minutes';
```

Si no existe:
```sql
ALTER TABLE system_configuration
ADD COLUMN average_consumption_time_minutes INTEGER NOT NULL DEFAULT 120;
```

---

## 📝 Checklist de Verificación

- [ ] El input en HTML tiene `th:field="*{averageConsumptionTimeMinutes}"`
- [ ] El input NO tiene atributo `disabled` o `readonly`
- [ ] El input está DENTRO del `<form>` correcto
- [ ] El request POST incluye el parámetro `averageConsumptionTimeMinutes`
- [ ] El controller recibe el valor correctamente (ver logs)
- [ ] El service copia el valor a `existingConfig` (ver logs)
- [ ] El `save()` se ejecuta sin errores
- [ ] La columna `average_consumption_time_minutes` existe en la BD
- [ ] La transacción se commitea correctamente
- [ ] No hay excepciones después del `save()`

---

## 🚀 Acciones Inmediatas

1. **Ejecutar la aplicación** y realizar una actualización
2. **Revisar los logs** en consola para ver los valores de `averageConsumptionTimeMinutes`
3. **Verificar en DevTools** que el formulario envía el parámetro
4. **Consultar la BD** para confirmar si el valor se actualiza

**Ejecutar:**
```bash
# Limpiar y compilar
mvn clean install

# Ejecutar con logs de debug
mvn spring-boot:run
```

**En los logs buscar:**
```
DEBUG - Received averageConsumptionTimeMinutes: <valor>
DEBUG - Input averageConsumptionTimeMinutes: <valor>
DEBUG - Existing averageConsumptionTimeMinutes after update: <valor>
```

---

## ✅ Resultado Esperado

Si todo funciona correctamente, deberías ver:

```
INFO  - Processing system configuration update
DEBUG - Received averageConsumptionTimeMinutes: 150
INFO  - Updating system configuration
DEBUG - Input averageConsumptionTimeMinutes: 150
DEBUG - Existing averageConsumptionTimeMinutes before update: 120
DEBUG - Existing averageConsumptionTimeMinutes after update: 150
INFO  - System configuration updated successfully
```

Y en la BD:
```sql
SELECT average_consumption_time_minutes FROM system_configuration;
-- Resultado: 150
```

---

## 📚 Referencias

- **Archivo Template:** `src/main/resources/templates/admin/system-configuration/form.html`
- **Controller:** `SystemConfigurationController.java`
- **Service:** `SystemConfigurationServiceImpl.java`
- **Entity:** `SystemConfiguration.java`
