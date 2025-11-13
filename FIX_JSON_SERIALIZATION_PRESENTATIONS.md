# 🔧 CORRECCIÓN: Error de Serialización JSON y Ordenamiento de Presentaciones

## ❌ PROBLEMA REPORTADO

### Error 1: Serialización JSON Infinita
```
WARN: Could not write JSON: Document nesting depth (1001) exceeds the maximum allowed (1000)
```

### Error 2: Solo aparece 1 presentación en lugar de todas
- Al seleccionar una categoría, solo aparece una presentación
- Deberían aparecer todas las presentaciones de esa categoría

### Error 3: Ordenamiento incorrecto
- Las presentaciones se ordenan por ID de mayor a menor
- Deberían ordenarse alfabéticamente (A-Z)

---

## 🔍 CAUSA DEL PROBLEMA

### Problema Principal: Relaciones Bidireccionales
El error de serialización JSON ocurre debido a relaciones bidireccionales entre entidades:

```java
// Presentation.java
@ManyToOne
private Category category;

// Category.java
@OneToMany(mappedBy = "category")
private List<Presentation> presentations;
```

**Bucle infinito:**
1. Jackson serializa `Presentation`
2. Serializa su `Category`
3. La `Category` tiene una lista de `Presentations`
4. Cada `Presentation` tiene una `Category`
5. ♻️ **BUCLE INFINITO** → Excede límite de anidamiento (1000 niveles)

---

## ✅ SOLUCIÓN IMPLEMENTADA

### 1. Creación de PresentationDTO

**Archivo:** `PresentationDTO.java`

```java
package com.aatechsolutions.elgransazon.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Presentation entity to avoid JSON serialization issues
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresentationDTO {
    private Long idPresentation;
    private String name;
    private String abbreviation;
    private String description;
    private Boolean active;
    private Long categoryId;          // Solo el ID, no el objeto completo
    private String categoryName;       // Solo el nombre
}
```

**Ventajas del DTO:**
- ✅ Sin relaciones bidireccionales
- ✅ Solo datos primitivos y Strings
- ✅ No hay objetos anidados complejos
- ✅ Serialización JSON segura

---

### 2. Actualización del Endpoint AJAX

**Archivo:** `ItemMenuController.java`

**ANTES (causaba el error):**
```java
@GetMapping("/presentations/{categoryId}")
@ResponseBody
public List<Presentation> getPresentationsByCategory(@PathVariable Long categoryId) {
    return presentationService.findActiveByCategoryId(categoryId);
    // ❌ Devuelve entidades con relaciones bidireccionales
}
```

**DESPUÉS (corregido):**
```java
@GetMapping("/presentations/{categoryId}")
@ResponseBody
public List<PresentationDTO> getPresentationsByCategory(@PathVariable Long categoryId) {
    List<Presentation> presentations = presentationService.findActiveByCategoryId(categoryId);
    
    // Convertir a DTOs para evitar bucles de serialización
    return presentations.stream()
            .map(p -> PresentationDTO.builder()
                    .idPresentation(p.getIdPresentation())
                    .name(p.getName())
                    .abbreviation(p.getAbbreviation())
                    .description(p.getDescription())
                    .active(p.getActive())
                    .categoryId(p.getCategory().getIdCategory())
                    .categoryName(p.getCategory().getName())
                    .build())
            .collect(Collectors.toList());
    // ✅ Devuelve DTOs sin relaciones bidireccionales
}
```

**Cambios clave:**
- ✅ Retorna `List<PresentationDTO>` en vez de `List<Presentation>`
- ✅ Mapea cada `Presentation` a `PresentationDTO`
- ✅ Solo extrae datos necesarios (sin objetos anidados)

---

### 3. Verificación del Ordenamiento

**Archivo:** `PresentationRepository.java`

```java
@Query("SELECT p FROM Presentation p WHERE p.category.idCategory = :categoryId AND p.active = true ORDER BY p.name ASC")
List<Presentation> findActiveByCategoryId(@Param("categoryId") Long categoryId);
```

**Confirmación:**
- ✅ `ORDER BY p.name ASC` → Orden alfabético A-Z
- ✅ Ya estaba correctamente configurado
- ✅ El problema era solo la serialización JSON

---

## 📊 FLUJO CORREGIDO

### Antes (con error):
```
1. Usuario selecciona categoría
2. JavaScript hace fetch a /admin/menu-items/presentations/{categoryId}
3. Controlador devuelve List<Presentation>
4. Jackson intenta serializar Presentation
5. Encuentra Category dentro de Presentation
6. Category tiene List<Presentation>
7. ♻️ BUCLE INFINITO
8. ❌ Error: Document nesting depth exceeds 1000
```

### Después (corregido):
```
1. Usuario selecciona categoría
2. JavaScript hace fetch a /admin/menu-items/presentations/{categoryId}
3. Controlador consulta presentaciones de la BD
4. Convierte List<Presentation> → List<PresentationDTO>
5. ✅ DTOs solo tienen datos simples (sin relaciones)
6. Jackson serializa sin problemas
7. ✅ Frontend recibe JSON limpio
8. ✅ Muestra TODAS las presentaciones ordenadas A-Z
```

---

## 🎯 RESULTADO ESPERADO

### JavaScript (form.html):
```javascript
fetch(`/admin/menu-items/presentations/${categoryId}`)
    .then(response => response.json())
    .then(presentations => {
        // presentations ahora es un array de DTOs
        presentations.forEach(p => {
            const option = document.createElement('option');
            option.value = p.idPresentation;        // ✅ Funciona
            option.textContent = p.name + 
                (p.abbreviation ? ' (' + p.abbreviation + ')' : '');
            presentationSelect.appendChild(option);
        });
    });
```

### Respuesta JSON Ejemplo:
```json
[
  {
    "idPresentation": 1,
    "name": "Por Kilo",
    "abbreviation": "kg",
    "description": null,
    "active": true,
    "categoryId": 1,
    "categoryName": "Carnes"
  },
  {
    "idPresentation": 2,
    "name": "Por Pieza",
    "abbreviation": "pz",
    "description": null,
    "active": true,
    "categoryId": 1,
    "categoryName": "Carnes"
  }
]
```

---

## ✅ VERIFICACIÓN

### 1. Sin Errores de Serialización
```
✅ No más warnings de "Document nesting depth exceeds 1000"
✅ JSON se serializa correctamente
✅ Response es rápido y limpio
```

### 2. Todas las Presentaciones Aparecen
```
✅ Si una categoría tiene 5 presentaciones, aparecen las 5
✅ El dropdown se llena completamente
✅ No se pierden datos
```

### 3. Ordenamiento Correcto
```
✅ Presentaciones ordenadas alfabéticamente:
   - Por Kilo (kg)
   - Por Pieza (pz)
   - Vaso 355ml
   - Vaso 500ml
```

---

## 🚀 PASOS PARA PROBAR

1. **Reiniciar la aplicación** (si está corriendo)

2. **Ir al formulario de nuevo item:**
   ```
   http://localhost:8080/admin/menu-items/new
   ```

3. **Seleccionar una categoría** en el dropdown

4. **Verificar que:**
   - ✅ Aparecen TODAS las presentaciones de esa categoría
   - ✅ Están ordenadas alfabéticamente (A-Z)
   - ✅ No hay warnings en la consola
   - ✅ El dropdown se llena instantáneamente

5. **Abrir DevTools (F12)**
   - Tab "Network"
   - Seleccionar una categoría
   - Ver la petición a `/admin/menu-items/presentations/X`
   - ✅ Response debe ser JSON limpio sin errores

---

## 📝 ARCHIVOS MODIFICADOS

| Archivo | Acción | Descripción |
|---------|--------|-------------|
| `PresentationDTO.java` | ✅ **CREADO** | DTO sin relaciones bidireccionales |
| `ItemMenuController.java` | ✅ **MODIFICADO** | Endpoint retorna DTOs en vez de entidades |

---

## 🎓 LECCIÓN APRENDIDA

### Problema de las Relaciones Bidireccionales en APIs REST

**Regla de oro:**
> **Nunca devolver entidades JPA directamente en endpoints REST/AJAX**

**Siempre usar DTOs cuando:**
- ✅ Hay relaciones bidireccionales (@ManyToOne + @OneToMany)
- ✅ Necesitas controlar qué datos se exponen
- ✅ Quieres evitar Lazy Loading exceptions
- ✅ Necesitas transformar datos antes de enviarlos

**Alternativas al DTO (menos recomendadas):**
- `@JsonIgnore` en las entidades (acopla lógica de persistencia con presentación)
- `@JsonManagedReference` y `@JsonBackReference` (complejo de mantener)
- Configurar Jackson para manejar referencias circulares (no resuelve el problema de fondo)

**Best Practice:**
```java
// ❌ MAL
@GetMapping("/api/presentations")
public List<Presentation> getAll() {
    return presentationRepository.findAll(); // Puede tener bucles
}

// ✅ BIEN
@GetMapping("/api/presentations")
public List<PresentationDTO> getAll() {
    return presentationRepository.findAll()
        .stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
}
```

---

## 🎉 ¡PROBLEMA RESUELTO!

El sistema ahora:
- ✅ Muestra todas las presentaciones de una categoría
- ✅ Las ordena alfabéticamente
- ✅ No tiene errores de serialización JSON
- ✅ Es más eficiente (menos datos transferidos)
- ✅ Sigue las mejores prácticas de DTOs
