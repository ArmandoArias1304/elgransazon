# Validación de Descuento Fijo en Promociones

## 📋 Descripción de los Cambios

Se ha implementado una validación completa para asegurar que las promociones de tipo **"Descuento Valor Fijo"** no tengan un monto de descuento mayor que el precio de los ítems a los que se aplican.

### Problema Original
- Si un ítem costaba $50 y se aplicaba un descuento fijo de $100, el precio final era -$50 (negativo)
- Esto no tenía sentido desde el punto de vista del negocio

### Solución Implementada
Validación en múltiples capas para prevenir este escenario:

## 🔧 Cambios Implementados

### 1. **Nueva Método en PromotionService** (`PromotionService.java`)
```java
/**
 * Validate that fixed discount amount is not greater than item prices
 * @param promotion The promotion to validate
 * @return Map with validation results: "valid" (boolean) and "invalidItems" (list of item names)
 */
java.util.Map<String, Object> validateFixedDiscountAmount(Promotion promotion);
```

### 2. **Implementación en PromotionServiceImpl** (`PromotionServiceImpl.java`)
- Verifica cada ítem en la promoción
- Compara el precio del ítem con el monto de descuento
- Retorna una lista de ítems inválidos (donde el descuento es mayor al precio)
- Registra warnings en el log

### 3. **Validación en PromotionController** - Método `createPromotion()`
- Antes de guardar una nueva promoción, valida si es de tipo `FIXED_AMOUNT_DISCOUNT`
- Si hay ítems inválidos, muestra un mensaje de error detallado
- El formulario no se envía y se mantienen los datos ingresados

### 4. **Validación en PromotionController** - Método `updatePromotion()`
- Misma validación al actualizar una promoción existente
- Previene que se modifique una promoción haciéndola inválida

### 5. **Validación en Promotion Entity** - Método `calculateFixedDiscount()`
- Añade una validación defensiva al calcular el precio con descuento
- Lanza `IllegalArgumentException` si el descuento es mayor que el precio
- Esto funciona como última línea de defensa

## 📝 Mensaje de Error

Cuando se detecta un descuento inválido, el sistema muestra:

```
El descuento de $100.00 es mayor que el precio de los siguientes items: Coca-Cola ($50.00), Agua ($30.00). 
El descuento fijo no puede ser mayor al precio del item.
```

## 🎯 Casos de Uso

### ✅ Caso Válido
- **Ítem:** Hamburguesa - $50.00
- **Descuento Fijo:** $10.00
- **Resultado:** Precio final = $40.00 ✓

### ❌ Caso Inválido (Bloqueado)
- **Ítem:** Coca-Cola - $30.00
- **Descuento Fijo:** $50.00
- **Resultado:** ERROR - No se puede crear/actualizar la promoción

### ✅ Caso Múltiples Ítems
- **Ítem 1:** Pizza - $80.00
- **Ítem 2:** Ensalada - $45.00
- **Descuento Fijo:** $20.00
- **Resultado:** Ambos ítems válidos ✓

### ❌ Caso Múltiples Ítems (Parcialmente Inválido)
- **Ítem 1:** Pizza - $80.00 ✓
- **Ítem 2:** Coca-Cola - $15.00 ❌
- **Descuento Fijo:** $20.00
- **Resultado:** ERROR - Coca-Cola ($15.00) tiene precio menor al descuento

## 🧪 Cómo Probar

### Prueba 1: Crear Promoción Inválida
1. Ir a **Admin → Promociones → Nueva Promoción**
2. Llenar el formulario:
   - Nombre: "Test Descuento Mayor"
   - Tipo: "Descuento Valor Fijo"
   - Monto de Descuento: $100.00
   - Seleccionar un ítem que cueste menos de $100 (ej: Coca-Cola $50)
3. Intentar guardar
4. **Resultado Esperado:** Mensaje de error indicando que el descuento es mayor que el precio

### Prueba 2: Crear Promoción Válida
1. Ir a **Admin → Promociones → Nueva Promoción**
2. Llenar el formulario:
   - Nombre: "Descuento $5"
   - Tipo: "Descuento Valor Fijo"
   - Monto de Descuento: $5.00
   - Seleccionar ítems que cuesten más de $5
3. Guardar
4. **Resultado Esperado:** Promoción creada exitosamente

### Prueba 3: Actualizar Promoción Existente
1. Editar una promoción de descuento fijo existente
2. Cambiar el monto de descuento a un valor mayor que alguno de los ítems
3. Intentar guardar
4. **Resultado Esperado:** Mensaje de error

## 🔍 Logs Generados

Cuando se detecta un descuento inválido, se registran logs como:
```
WARN  - Invalid discount amount for item 'Coca-Cola': discount $100.00 > price $50.00
WARN  - Fixed discount validation failed: El descuento de $100.00 es mayor que el precio...
```

## 🛡️ Capas de Validación

1. **Frontend:** (Recomendado agregar) - Validación JavaScript en tiempo real
2. **Controller:** Validación antes de guardar (✅ Implementado)
3. **Service:** Método de validación reutilizable (✅ Implementado)
4. **Entity:** Validación al calcular precio (✅ Implementado)

## 📊 Estructura del Resultado de Validación

El método `validateFixedDiscountAmount()` retorna un Map con:
```json
{
  "valid": false,
  "invalidItems": ["Coca-Cola ($50.00)", "Agua ($30.00)"],
  "discountAmount": 100.00
}
```

## ✅ Beneficios

1. **Integridad de Datos:** Previene precios negativos en el sistema
2. **Experiencia de Usuario:** Mensaje de error claro y específico
3. **Mantenibilidad:** Validación centralizada y reutilizable
4. **Auditoría:** Logs detallados de intentos de configuración inválida
5. **Seguridad:** Múltiples capas de validación

## 🚀 Próximos Pasos (Opcional)

1. **Validación Frontend:** Agregar validación en tiempo real en el formulario HTML
2. **Test Unitarios:** Crear tests automatizados para la validación
3. **Notificaciones:** Alert visual cuando se intenta agregar un ítem inválido
4. **Reporte:** Dashboard mostrando promociones con configuraciones problemáticas

## 📝 Notas Técnicas

- La validación se ejecuta solo para promociones de tipo `FIXED_AMOUNT_DISCOUNT`
- Los otros tipos de promoción (`BUY_X_PAY_Y`, `PERCENTAGE_DISCOUNT`) no se ven afectados
- El descuento de porcentaje ya tiene su propia validación (0-100%)
- La comparación de precios usa `BigDecimal.compareTo()` para precisión

---

**Fecha de Implementación:** 17 de Enero, 2026
**Archivos Modificados:**
- `PromotionService.java`
- `PromotionServiceImpl.java`
- `PromotionController.java`
- `Promotion.java`
