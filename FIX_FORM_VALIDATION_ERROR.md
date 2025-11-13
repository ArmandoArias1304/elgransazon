# Fix: Error "Invalid form control is not focusable"

## 🐛 Error Original

```
An invalid form control with name='ingredientIds' is not focusable
An invalid form control with name='quantities' is not focusable
```

## 🔍 Causa Raíz

El **template oculto** (`#ingredientTemplate`) tenía campos con atributo `required`, pero estaba oculto con `class="hidden"`. 

Cuando el formulario intentaba validarse:
1. El navegador detectaba campos `required` vacíos en el template
2. Intentaba enfocar esos campos para mostrar el error
3. **PERO** no podía enfocarlos porque estaban ocultos (`display: none`)
4. Resultado: Error en consola y bloqueo del submit

## ✅ Solución Implementada

### Cambio 1: Remover `required` del Template

**Antes:**
```html
<div id="ingredientTemplate" class="hidden">
    <select name="ingredientIds" required> <!-- ❌ -->
    <input name="quantities" required>     <!-- ❌ -->
    <input name="units" required>          <!-- ❌ -->
</div>
```

**Después:**
```html
<div id="ingredientTemplate" class="hidden">
    <select name="ingredientIds">  <!-- ✅ Sin required -->
    <input name="quantities">      <!-- ✅ Sin required -->
    <input name="units">           <!-- ✅ Sin required -->
</div>
```

### Cambio 2: Agregar `required` Dinámicamente en JavaScript

**Función `addIngredientRow()` Actualizada:**

```javascript
function addIngredientRow() {
    // ... código de clonación ...
    
    // ⭐ Agregar atributo required a los campos clonados
    const ingredientSelect = newRow.querySelector('select[name="ingredientIds"]');
    const quantityInput = newRow.querySelector('input[name="quantities"]');
    const unitInput = newRow.querySelector('input[name="units"]');
    
    if (ingredientSelect) {
        ingredientSelect.setAttribute('required', 'required');  // ✅
        ingredientSelect.addEventListener('change', function() {
            loadIngredientUnit(this);
        });
    }
    
    if (quantityInput) {
        quantityInput.setAttribute('required', 'required');  // ✅
    }
    
    if (unitInput) {
        unitInput.setAttribute('required', 'required');  // ✅
    }
    
    container.appendChild(newRow);
}
```

## 🎯 Resultado

- ✅ El template oculto ya no interfiere con la validación
- ✅ Las filas clonadas SÍ tienen validación `required`
- ✅ El botón "Guardar Item" funciona correctamente
- ✅ No más errores en consola del navegador
- ✅ La validación HTML5 funciona como esperado

## 📝 Archivos Modificados

- ✅ `admin/menu-items/form.html`
  - Template: Removidos atributos `required` (3 campos)
  - JavaScript: Agregados `setAttribute('required', 'required')` en `addIngredientRow()`

## 🧪 Prueba

1. Ir a "Nuevo Item del Menú"
2. Se carga con 1 fila automática (con `required` correcto)
3. Completar el formulario
4. Click en "Guardar Item"
5. ✅ **Funciona sin errores** 🎉

---

**Estado:** ✅ Resuelto  
**Fecha:** 2025-01-22
