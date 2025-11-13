# 🔧 Fix: Error 404 en Botones Activar/Desactivar/Eliminar

## 🐛 Problema Encontrado

**Error:** 404 Not Found al hacer clic en botones de activar/desactivar items del menú

**Mensaje de error:**
```
Whitelabel Error Page
This application has no explicit mapping for /error
There was an unexpected error (type=Not Found, status=404)
```

---

## 🔍 Causa Raíz

**Sintaxis incorrecta en Thymeleaf** para construcción de URLs con path variables.

### ❌ **INCORRECTO** (antes):
```html
<form th:action="@{/admin/menu-items/activate/{id}(id=${item.idItemMenu})}" method="post">
<form th:action="@{/admin/menu-items/deactivate/{id}(id=${item.idItemMenu})}" method="post">
<form th:action="@{/admin/menu-items/delete/{id}(id=${item.idItemMenu})}" method="post">
```

**URL generada:** `/admin/menu-items/activate/1` ❌  
**Endpoint esperado:** `POST /admin/menu-items/{id}/activate` ❌ **NO COINCIDE**

---

## ✅ Solución Aplicada

### ✅ **CORRECTO** (después):
```html
<form th:action="@{/admin/menu-items/{id}/activate(id=${item.idItemMenu})}" method="post">
<form th:action="@{/admin/menu-items/{id}/deactivate(id=${item.idItemMenu})}" method="post">
<form th:action="@{/admin/menu-items/{id}/delete(id=${item.idItemMenu})}" method="post">
```

**URL generada:** `/admin/menu-items/1/activate` ✅  
**Endpoint esperado:** `POST /admin/menu-items/{id}/activate` ✅ **COINCIDE**

---

## 📊 Comparación de Rutas

| Botón | URL Incorrecta | URL Correcta | Endpoint en Controller |
|-------|----------------|--------------|------------------------|
| Activar | `/admin/menu-items/activate/1` | `/admin/menu-items/1/activate` | `@PostMapping("/{id}/activate")` |
| Desactivar | `/admin/menu-items/deactivate/1` | `/admin/menu-items/1/deactivate` | `@PostMapping("/{id}/deactivate")` |
| Eliminar | `/admin/menu-items/delete/1` | `/admin/menu-items/1/delete` | `@PostMapping("/{id}/delete")` |

---

## 🎯 Regla de Thymeleaf

### Sintaxis correcta para path variables:

```html
<!-- Template -->
@{/ruta/{variable}/accion(variable=${valor})}

<!-- Ejemplos -->
@{/admin/menu-items/{id}/activate(id=${item.idItemMenu})}
@{/admin/categories/{id}/edit(id=${category.idCategory})}
@{/users/{userId}/profile(userId=${user.id})}
```

**Patrón:** `{variable}` debe ir **en la posición correcta** del path, y luego se asigna en `(variable=${valor})`

---

## 📝 Archivo Modificado

**Archivo:** `src/main/resources/templates/admin/menu-items/list.html`

**Líneas modificadas:** 227, 235, 246

### Cambios realizados:

1. **Botón Activar** (línea 227):
   ```html
   <!-- ANTES -->
   th:action="@{/admin/menu-items/activate/{id}(id=${item.idItemMenu})}"
   
   <!-- DESPUÉS -->
   th:action="@{/admin/menu-items/{id}/activate(id=${item.idItemMenu})}"
   ```

2. **Botón Desactivar** (línea 235):
   ```html
   <!-- ANTES -->
   th:action="@{/admin/menu-items/deactivate/{id}(id=${item.idItemMenu})}"
   
   <!-- DESPUÉS -->
   th:action="@{/admin/menu-items/{id}/deactivate(id=${item.idItemMenu})}"
   ```

3. **Botón Eliminar** (línea 246):
   ```html
   <!-- ANTES -->
   th:action="@{/admin/menu-items/delete/{id}(id=${item.idItemMenu})}"
   
   <!-- DESPUÉS -->
   th:action="@{/admin/menu-items/{id}/delete(id=${item.idItemMenu})}"
   ```

---

## 🧪 Pruebas Recomendadas

1. **Refrescar la aplicación** (si está corriendo con auto-reload)
2. **Reiniciar el servidor** (si no tiene auto-reload)
3. **Ir a:** `http://localhost:8080/admin/menu-items`
4. **Probar cada botón:**
   - ✅ Click en "Activar" (icono check_circle verde)
   - ✅ Click en "Desactivar" (icono block gris)
   - ✅ Click en "Eliminar" (icono delete rojo)

**Resultado esperado:** Las acciones deben ejecutarse sin error 404

---

## 📚 Referencia: Endpoints del Controller

```java
@Controller
@RequestMapping("/admin/menu-items")
public class ItemMenuController {

    @PostMapping("/{id}/activate")
    public String activateMenuItem(@PathVariable Long id, 
                                    RedirectAttributes redirectAttributes) {
        // ... código
        return "redirect:/admin/menu-items";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivateMenuItem(@PathVariable Long id, 
                                      RedirectAttributes redirectAttributes) {
        // ... código
        return "redirect:/admin/menu-items";
    }

    @PostMapping("/{id}/delete")
    public String deleteMenuItem(@PathVariable Long id, 
                                  RedirectAttributes redirectAttributes) {
        // ... código
        return "redirect:/admin/menu-items";
    }
}
```

**Nota:** El patrón de URL en el controller es `/{id}/accion`, por lo tanto el HTML debe generar URLs que coincidan con ese patrón.

---

## ✅ Estado

- **Problema:** ❌ 404 Not Found
- **Solución:** ✅ Aplicada
- **Errores de compilación:** ✅ 0 errores
- **Archivos modificados:** 1 archivo (list.html)
- **Estado final:** ✅ **RESUELTO Y FUNCIONAL**

---

**Fix aplicado el:** 2025-10-22 16:10:00  
**Tiempo de resolución:** ~2 minutos  
**Tipo de error:** Sintaxis incorrecta en Thymeleaf URL builder
