# Fix: Error al Actualizar ItemMenu - NullPointerException

## 🐛 Error Original

```
2025-10-22T13:30:53.176-06:00 ERROR 25248 --- [elgransazon] [nio-8080-exec-7] 
c.a.e.p.controller.ItemMenuController : Validation error updating menu item: null
```

## 🔍 Causa Raíz

El método `updateMenuItem` tenía un manejo de conversión de datos inseguro:

```java
// ❌ CÓDIGO PROBLEMÁTICO
List<BigDecimal> quantitiesBD = quantities.stream()
        .map(BigDecimal::new)
        .toList();
```

**Problemas:**
1. Si `quantities` es `null`, falla con `NullPointerException`
2. Si algún elemento de `quantities` es `null` o string vacío, falla al crear `BigDecimal`
3. Si algún string no es un número válido, lanza `NumberFormatException`
4. El mensaje de error era `null` porque la excepción no se capturaba correctamente

## ✅ Solución Implementada

### 1. Conversión Segura de Cantidades

**Antes:**
```java
List<BigDecimal> quantitiesBD = quantities.stream()
        .map(BigDecimal::new)
        .toList();
```

**Después:**
```java
List<BigDecimal> quantitiesBD = new ArrayList<>();
if (quantities != null) {
    for (String qty : quantities) {
        try {
            if (qty != null && !qty.trim().isEmpty()) {
                quantitiesBD.add(new BigDecimal(qty));
            } else {
                quantitiesBD.add(BigDecimal.ZERO);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid quantity format: {}", qty);
            quantitiesBD.add(BigDecimal.ZERO);
        }
    }
}
```

**Mejoras:**
- ✅ Verifica que `quantities` no sea `null`
- ✅ Verifica cada elemento individualmente antes de convertir
- ✅ Maneja strings vacíos o nulos
- ✅ Captura `NumberFormatException` para valores no numéricos
- ✅ Usa `BigDecimal.ZERO` como valor por defecto seguro
- ✅ Log de advertencia para valores inválidos

### 2. Validación de Receta en Update

Agregada la misma validación que existe en `createMenuItem`:

```java
// Validate at least one ingredient
if (recipe == null || recipe.isEmpty()) {
    model.addAttribute("errorMessage", "Debe agregar al menos un ingrediente a la receta");
    loadFormData(model, itemMenu, itemMenuService.getRecipe(id));
    model.addAttribute("formAction", "/admin/menu-items/" + id);
    return "admin/menu-items/form";
}
```

**Propósito:**
- ✅ Evita actualizar un item del menú sin ingredientes
- ✅ Muestra mensaje claro al usuario
- ✅ Mantiene consistencia entre create y update

### 3. Mejora en Logging de Errores

**Antes:**
```java
catch (IllegalArgumentException e) {
    log.error("Validation error updating menu item: {}", e.getMessage());
    // ...
}

catch (Exception e) {
    log.error("Error updating menu item", e);
    // ...
}
```

**Después:**
```java
catch (IllegalArgumentException e) {
    log.error("Validation error updating menu item: {}", e.getMessage(), e);
    // Ahora incluye stack trace completo
}

catch (Exception e) {
    log.error("Error updating menu item: {}", e.getMessage(), e);
    // Formato consistente con stack trace
}
```

**Mejoras:**
- ✅ Stack trace completo para debugging
- ✅ Formato consistente en todos los logs
- ✅ Más fácil identificar la causa del error

## 📊 Flujo Actualizado

### Escenario 1: Update Exitoso
```
Usuario → Form → Controller
  ↓
Validación campos básicos (BindingResult)
  ↓
Conversión segura quantities → BigDecimal
  ↓
Build recipe con datos convertidos
  ↓
Validación: ¿recipe no vacía? ✅
  ↓
itemMenuService.update(id, itemMenu, recipe)
  ↓
Redirect con mensaje de éxito ✅
```

### Escenario 2: Cantidad Inválida
```
Usuario ingresa "abc" en cantidad
  ↓
Controller recibe: quantities = ["abc"]
  ↓
Conversión intenta: new BigDecimal("abc")
  ↓
❌ NumberFormatException capturada
  ↓
Log warning: "Invalid quantity format: abc"
  ↓
Usa BigDecimal.ZERO como fallback
  ↓
Continúa procesamiento sin crash ✅
```

### Escenario 3: Sin Ingredientes
```
Usuario elimina todos los ingredientes
  ↓
ingredientIds = [] (lista vacía)
  ↓
recipe = [] después de buildRecipe
  ↓
Validación detecta: recipe.isEmpty() ❌
  ↓
Muestra error: "Debe agregar al menos un ingrediente a la receta"
  ↓
Usuario ve formulario con mensaje de error ✅
```

### Escenario 4: Quantities Null
```
Form submission sin ingredientes
  ↓
quantities = null
  ↓
Check: if (quantities != null) → false
  ↓
quantitiesBD queda como ArrayList vacía
  ↓
buildRecipe retorna lista vacía
  ↓
Validación detecta recipe vacía
  ↓
Muestra mensaje de error al usuario ✅
```

## 🎯 Casos de Prueba

### Caso 1: Update Normal
1. Editar item existente
2. Cambiar nombre/precio
3. Modificar cantidad de ingrediente (ej: "2.5")
4. Click "Actualizar Item"
5. ✅ Debe actualizarse correctamente

### Caso 2: Cantidad con Formato Inválido
1. Editar item existente
2. Escribir "abc" en cantidad
3. Click "Actualizar Item"
4. ✅ Debe usar 0 y mostrar warning en logs
5. ✅ Validación detectará ingrediente con cantidad 0 y lo ignorará

### Caso 3: Sin Ingredientes
1. Editar item existente
2. Eliminar todos los ingredientes (botón 🗑️)
3. Click "Actualizar Item"
4. ✅ Debe mostrar: "Debe agregar al menos un ingrediente a la receta"

### Caso 4: Cantidad Vacía
1. Editar item existente
2. Dejar campo cantidad vacío
3. Click "Actualizar Item"
4. ✅ Debe usar 0 como cantidad
5. ✅ Ingrediente se ignora en buildRecipe (cantidad = 0)

## 🔧 Archivos Modificados

### `ItemMenuController.java`

**Cambios en `updateMenuItem()`:**
1. ✅ Conversión segura de `quantities` (String → BigDecimal)
2. ✅ Validación de `recipe` no vacía
3. ✅ Logging mejorado con stack traces

**Líneas modificadas:**
- Líneas ~192-217: Conversión y validación
- Línea ~235: Log con stack trace
- Línea ~241: Log con stack trace

## 📝 Comparación Antes/Después

| Aspecto | ❌ Antes | ✅ Después |
|---------|---------|-----------|
| **Conversión quantities** | Stream directo (puede fallar) | Loop con try-catch individual |
| **Null handling** | Falla con NPE | Verifica null en cada paso |
| **Valores inválidos** | Crash con NumberFormatException | Usa BigDecimal.ZERO como fallback |
| **Validación recipe** | Solo en create | En create Y update |
| **Logging** | Mensaje "null" sin contexto | Stack trace completo |
| **Feedback usuario** | Error genérico | Mensajes específicos |

## 🚀 Beneficios

1. **Robustez**: No crash por datos inválidos
2. **Debugging**: Stack traces completos facilitan identificar errores
3. **UX**: Mensajes claros cuando falla validación
4. **Consistencia**: Misma validación en create y update
5. **Mantenibilidad**: Código más legible con manejo explícito de errores

## 🔍 Notas Técnicas

- Los ingredientes con cantidad 0 se ignoran en `buildRecipe()` (línea 453: `if (quantity.compareTo(BigDecimal.ZERO) > 0)`)
- El método `buildRecipe()` ya maneja valores nulos correctamente
- La validación de "al menos 1 ingrediente" se ejecuta DESPUÉS de buildRecipe, por lo que ingredientes inválidos ya fueron filtrados

---

**Estado:** ✅ Resuelto  
**Fecha:** 2025-01-22  
**Impacto:** Crítico → Solucionado
