# 🚀 GUÍA COMPLETA DE IMPLEMENTACIÓN - SISTEMA DE MENÚ CON RECETAS

## ✅ ARCHIVOS COMPLETADOS

### Entidades (4/4) ✅
- ✅ `Presentation.java` - Presentaciones de venta
- ✅ `ItemMenu.java` - Platillos del menú
- ✅ `ItemIngredient.java` - Recetas (relación ItemMenu-Ingredient)
- ✅ `Category.java` - Modificada con relaciones

### Repositorios (3/3) ✅
- ✅ `PresentationRepository.java`
- ✅ `ItemMenuRepository.java`
- ✅ `ItemIngredientRepository.java`

### Base de Datos (1/1) ✅
- ✅ `database/init_menu_system.sql` - Script completo con datos de ejemplo

---

## 📋 ARCHIVOS PENDIENTES POR CREAR

### 1. Servicios (3 archivos)

#### `PresentationServiceImpl.java`
**Ubicación:** `src/main/java/com/aatechsolutions/elgransazon/application/service/`

**Responsabilidades:**
- CRUD completo de presentaciones
- Validar que no se eliminen presentaciones en uso
- Validar nombres únicos por categoría

**Métodos clave:**
```java
@Transactional
public Presentation create(Presentation presentation) {
    // 1. Validar que el nombre no existe en esa categoría
    // 2. Guardar
    // 3. Retornar
}

@Transactional
public void delete(Long id) {
    // 1. Verificar que no esté en uso por ItemMenu
    // 2. Si está en uso, lanzar excepción
    // 3. Si no, eliminar
}
```

---

#### `ItemMenuService.java` (Interfaz)
**Ubicación:** `src/main/java/com/aatechsolutions/elgransazon/application/service/`

**Métodos principales:**
```java
// CRUD básico
List<ItemMenu> findAll();
Optional<ItemMenu> findById(Long id);
ItemMenu create(ItemMenu item, List<ItemIngredient> recipe);
ItemMenu update(Long id, ItemMenu item, List<ItemIngredient> recipe);
void delete(Long id);

// Gestión de recetas
void addIngredientToItem(Long itemId, ItemIngredient ingredient);
void removeIngredientFromItem(Long itemId, Long ingredientId);
void updateRecipe(Long itemId, List<ItemIngredient> newRecipe);

// Control de stock e inventario
boolean hasEnoughStock(Long itemId, int quantity);
void sellItem(Long itemId, int quantity); // ⭐ MÉTODO MÁS IMPORTANTE
void updateItemAvailability(Long itemId);
void updateAllItemsAvailability();

// Búsquedas
List<ItemMenu> findAvailableItems();
List<ItemMenu> findByCategoryId(Long categoryId);
List<ItemMenu> searchByName(String searchTerm);
```

---

#### `ItemMenuServiceImpl.java` ⭐ **MÁS IMPORTANTE**
**Ubicación:** `src/main/java/com/aatechsolutions/elgransazon/application/service/`

**MÉTODO CRÍTICO: sellItem()** - Descuenta inventario automáticamente

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemMenuServiceImpl implements ItemMenuService {

    private final ItemMenuRepository itemMenuRepository;
    private final ItemIngredientRepository itemIngredientRepository;
    private final IngredientRepository ingredientRepository;
    
    /**
     * ⭐ MÉTODO MÁS IMPORTANTE - Vende un item y descuenta del inventario
     */
    @Transactional
    public void sellItem(Long itemMenuId, int quantity) {
        // 1. Obtener el item
        ItemMenu item = findByIdOrThrow(itemMenuId);
        
        // 2. Validar que esté activo y disponible
        if (!item.getActive()) {
            throw new IllegalStateException("El item no está activo");
        }
        if (!item.getAvailable()) {
            throw new IllegalStateException("El item no está disponible");
        }
        
        // 3. Verificar que hay stock suficiente
        if (!hasEnoughStock(itemMenuId, quantity)) {
            throw new InsufficientStockException("No hay ingredientes suficientes");
        }
        
        // 4. Descontar ingredientes uno por uno
        List<ItemIngredient> recipe = item.getIngredients();
        for (ItemIngredient itemIngredient : recipe) {
            try {
                // Descontar del stock
                BigDecimal newStock = itemIngredient.deductFromStock(quantity);
                
                // Guardar el ingrediente con el nuevo stock
                ingredientRepository.save(itemIngredient.getIngredient());
                
                log.info("Descontado {} {} de '{}'", 
                         itemIngredient.getQuantity().multiply(BigDecimal.valueOf(quantity)),
                         itemIngredient.getUnit(),
                         itemIngredient.getIngredientName());
                         
            } catch (Exception e) {
                log.error("Error al descontar ingrediente: {}", e.getMessage());
                throw new RuntimeException("Error al descontar inventario: " + e.getMessage());
            }
        }
        
        // 5. Actualizar disponibilidad del item
        updateItemAvailability(itemMenuId);
        
        log.info("Venta procesada: {} x {} unidades", item.getName(), quantity);
    }
    
    @Override
    public boolean hasEnoughStock(Long itemMenuId, int quantity) {
        ItemMenu item = findByIdOrThrow(itemMenuId);
        return item.hasEnoughStock(quantity);
    }
    
    @Transactional
    @Override
    public void updateItemAvailability(Long itemMenuId) {
        ItemMenu item = findByIdOrThrow(itemMenuId);
        item.updateAvailability(); // Método en la entidad
        itemMenuRepository.save(item);
    }
    
    @Transactional
    @Override
    public void updateAllItemsAvailability() {
        List<ItemMenu> allItems = itemMenuRepository.findByActiveTrue();
        for (ItemMenu item : allItems) {
            item.updateAvailability();
        }
        itemMenuRepository.saveAll(allItems);
    }
    
    @Transactional
    @Override
    public ItemMenu create(ItemMenu item, List<ItemIngredient> recipe) {
        // 1. Validar nombre único
        if (itemMenuRepository.existsByName(item.getName())) {
            throw new IllegalArgumentException("Ya existe un item con ese nombre");
        }
        
        // 2. Validar que la presentación pertenece a la categoría
        if (!item.getPresentation().getCategory().getIdCategory()
                .equals(item.getCategory().getIdCategory())) {
            throw new IllegalArgumentException(
                "La presentación no pertenece a la categoría seleccionada");
        }
        
        // 3. Guardar el item primero
        ItemMenu saved = itemMenuRepository.save(item);
        
        // 4. Guardar los ingredientes de la receta
        if (recipe != null && !recipe.isEmpty()) {
            for (ItemIngredient ingredient : recipe) {
                ingredient.setItemMenu(saved);
                itemIngredientRepository.save(ingredient);
            }
        }
        
        // 5. Actualizar disponibilidad
        updateItemAvailability(saved.getIdItemMenu());
        
        return saved;
    }
    
    @Transactional
    @Override
    public void updateRecipe(Long itemMenuId, List<ItemIngredient> newRecipe) {
        // 1. Eliminar receta anterior
        List<ItemIngredient> oldRecipe = itemIngredientRepository
            .findByItemMenuId(itemMenuId);
        itemIngredientRepository.deleteAll(oldRecipe);
        
        // 2. Guardar nueva receta
        ItemMenu item = findByIdOrThrow(itemMenuId);
        for (ItemIngredient ingredient : newRecipe) {
            ingredient.setItemMenu(item);
            itemIngredientRepository.save(ingredient);
        }
        
        // 3. Actualizar disponibilidad
        updateItemAvailability(itemMenuId);
    }
}
```

---

### 2. Controladores (2 archivos)

#### `PresentationController.java`
**Ubicación:** `src/main/java/com/aatechsolutions/elgransazon/presentation/controller/`

**Endpoints:**
- `GET /admin/presentations` - Lista de presentaciones
- `GET /admin/presentations/new` - Formulario nuevo
- `POST /admin/presentations` - Crear presentación
- `GET /admin/presentations/{id}/edit` - Formulario editar
- `POST /admin/presentations/{id}` - Actualizar
- `POST /admin/presentations/{id}/activate` - Activar
- `POST /admin/presentations/{id}/deactivate` - Desactivar

**Seguir patrón de:** `RestaurantTableController.java`

---

#### `ItemMenuController.java`
**Ubicación:** `src/main/java/com/aatechsolutions/elgransazon/presentation/controller/`

**Endpoints especiales:**
```java
@Controller
@RequestMapping("/admin/menu-items")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class ItemMenuController {
    
    // Lista de items
    @GetMapping
    public String listItems(Model model) { }
    
    // Formulario de creación (CON SELECTOR DE INGREDIENTES)
    @GetMapping("/new")
    public String newItemForm(Model model) {
        model.addAttribute("item", new ItemMenu());
        model.addAttribute("categories", categoryService.findAllActive());
        model.addAttribute("ingredients", ingredientService.findAllActive());
        return "admin/menu-items/form";
    }
    
    // Crear item con receta
    @PostMapping
    public String createItem(
        @ModelAttribute ItemMenu item,
        @RequestParam(required = false) List<Long> ingredientIds,
        @RequestParam(required = false) List<BigDecimal> quantities,
        @RequestParam(required = false) List<String> units,
        RedirectAttributes redirectAttributes) {
        
        // Construir la receta
        List<ItemIngredient> recipe = buildRecipe(ingredientIds, quantities, units);
        
        // Crear el item con su receta
        itemMenuService.create(item, recipe);
        
        redirectAttributes.addFlashAttribute("successMessage", "Item creado exitosamente");
        return "redirect:/admin/menu-items";
    }
    
    // AJAX: Obtener presentaciones por categoría
    @GetMapping("/presentations/{categoryId}")
    @ResponseBody
    public List<Presentation> getPresentationsByCategory(@PathVariable Long categoryId) {
        return presentationService.findActiveByCategoryId(categoryId);
    }
    
    // AJAX: Verificar stock disponible
    @GetMapping("/{id}/check-stock")
    @ResponseBody
    public Map<String, Object> checkStock(
        @PathVariable Long id, 
        @RequestParam int quantity) {
        
        Map<String, Object> response = new HashMap<>();
        boolean hasStock = itemMenuService.hasEnoughStock(id, quantity);
        response.put("hasStock", hasStock);
        return response;
    }
}
```

---

### 3. Vistas Thymeleaf (4 archivos)

#### `admin/menu-items/list.html`
**Características:**
- Usar fragmento `sidebar.html` con `activeMenu='menu'`
- Stats cards: Total items, Disponibles, No disponibles, Por categoría
- Filtros: Por categoría, por disponibilidad, búsqueda por nombre
- Tabla con:
  - Imagen del platillo
  - Nombre
  - Categoría
  - Presentación
  - Precio
  - Estado (Activo/Inactivo)
  - Disponibilidad (badge verde/rojo)
  - Botones: Ver receta, Editar, Activar/Desactivar

**Badge de disponibilidad:**
```html
<span th:if="${item.available}" 
      class="px-3 py-1 rounded-full text-xs font-semibold bg-green-100 text-green-700">
    ✅ Disponible
</span>
<span th:unless="${item.available}" 
      class="px-3 py-1 rounded-full text-xs font-semibold bg-red-100 text-red-700">
    ❌ Agotado
</span>
```

---

#### `admin/menu-items/form.html` ⭐ **MÁS COMPLEJO**
**Características:**
- Formulario con secciones:
  1. Información básica (nombre, descripción, precio)
  2. Categoría (select)
  3. Presentación (select que se llena dinámicamente al elegir categoría)
  4. Imagen (URL)
  5. **Receta - Ingredientes** (sección dinámica)

**Sección de Receta (JavaScript dinámico):**
```html
<div id="recipeSection">
    <h3>Receta - Ingredientes Necesarios</h3>
    <button type="button" onclick="addIngredientRow()">
        + Agregar Ingrediente
    </button>
    
    <div id="ingredientsContainer">
        <!-- Filas dinámicas de ingredientes -->
    </div>
</div>

<script>
let ingredientIndex = 0;

function addIngredientRow() {
    const container = document.getElementById('ingredientsContainer');
    const row = document.createElement('div');
    row.className = 'ingredient-row';
    row.innerHTML = `
        <select name="ingredientIds[${ingredientIndex}]" required>
            <option value="">Seleccionar ingrediente</option>
            <!-- Opciones desde Thymeleaf -->
            <option th:each="ing : ${ingredients}" 
                    th:value="${ing.idIngredient}"
                    th:text="${ing.name + ' (Stock: ' + ing.currentStock + ' ' + ing.unitOfMeasure + ')'}">
            </option>
        </select>
        
        <input type="number" 
               name="quantities[${ingredientIndex}]" 
               step="0.001" 
               placeholder="Cantidad" 
               required>
        
        <input type="text" 
               name="units[${ingredientIndex}]" 
               placeholder="Unidad (kg, unidades, L)" 
               required>
        
        <button type="button" onclick="removeIngredientRow(this)">
            ❌ Eliminar
        </button>
    `;
    container.appendChild(row);
    ingredientIndex++;
}

function removeIngredientRow(button) {
    button.parentElement.remove();
}

// Cargar presentaciones al cambiar categoría
document.getElementById('categorySelect').addEventListener('change', function() {
    const categoryId = this.value;
    
    fetch(`/admin/menu-items/presentations/${categoryId}`)
        .then(response => response.json())
        .then(presentations => {
            const select = document.getElementById('presentationSelect');
            select.innerHTML = '<option value="">Seleccionar presentación</option>';
            
            presentations.forEach(p => {
                const option = document.createElement('option');
                option.value = p.idPresentation;
                option.textContent = p.displayName;
                select.appendChild(option);
            });
        });
});
</script>
```

---

#### `admin/presentations/list.html`
**Seguir patrón de:** `admin/tables/list.html`

**Características:**
- Stats: Total presentaciones, Activas, Por categoría
- Filtro por categoría
- Tabla con: ID, Nombre, Abreviación, Categoría, Estado, Acciones

---

#### `admin/presentations/form.html`
**Seguir patrón de:** `admin/tables/form.html`

**Campos:**
- Nombre
- Abreviación
- Descripción
- Categoría (select)
- Estado (activo/inactivo)

---

### 4. Actualizar Sidebar

**Archivo:** `templates/fragments/sidebar.html`

**Agregar opciones de menú:**
```html
<!-- Existing menu items... -->

<a th:href="@{/admin/menu-items}"
   th:classappend="${activeMenu == 'menu'} ? 'active ...' : '...'"
   class="nav-item ...">
    <span class="material-symbols-outlined">restaurant_menu</span>
    <span>Menú</span>
</a>

<!-- Si quieres separar presentaciones en su propio link -->
<a th:href="@{/admin/presentations}"
   th:classappend="${activeMenu == 'presentations'} ? 'active ...' : '...'"
   class="nav-item ...">
    <span class="material-symbols-outlined">category</span>
    <span>Presentaciones</span>
</a>
```

---

## 🔥 FLUJO COMPLETO DE USO

### 1. Configuración Inicial (Admin)
1. Ir a `/admin/categories` - Crear categorías (Carnes, Bebidas, Postres)
2. Ir a `/admin/presentations` - Asignar presentaciones a cada categoría
3. Ir a `/admin/ingredients` - Crear ingredientes del inventario
4. Ir a `/admin/ingredient-categories` - Organizar ingredientes

### 2. Crear Items del Menú
1. Ir a `/admin/menu-items/new`
2. Llenar información básica
3. Seleccionar categoría → Se cargan sus presentaciones
4. Seleccionar presentación
5. Agregar ingredientes a la receta:
   - Click "+ Agregar Ingrediente"
   - Seleccionar ingrediente
   - Ingresar cantidad
   - Ingresar unidad (debe coincidir con la del ingrediente)
6. Guardar

### 3. Vender un Item (Automatizar descuento)
```java
// En el servicio de ventas/órdenes:
itemMenuService.sellItem(itemMenuId, quantity);

// Esto automáticamente:
// 1. Verifica que hay stock
// 2. Descuenta de cada ingrediente
// 3. Actualiza disponibilidad del item
```

### 4. Monitoreo
- Items se marcan como "No disponibles" automáticamente cuando falta algún ingrediente
- Vista de items con stock bajo
- Reportes de costos vs. precios

---

## 🎯 PRIORIDADES DE IMPLEMENTACIÓN

### Prioridad 1 (Esencial)
1. ✅ `PresentationServiceImpl.java`
2. ⭐ `ItemMenuService.java` + `ItemMenuServiceImpl.java`
3. ⭐ `ItemMenuController.java`
4. ⭐ `admin/menu-items/form.html` (con JavaScript dinámico)

### Prioridad 2 (Importante)
5. `admin/menu-items/list.html`
6. `PresentationController.java`
7. `admin/presentations/list.html` y `form.html`

### Prioridad 3 (Opcional)
8. Actualizar sidebar
9. Tests unitarios
10. Mejoras de UI

---

## 🐛 VALIDACIONES IMPORTANTES

### Al crear/editar ItemMenu:
- ✅ Nombre único
- ✅ Presentación debe pertenecer a la categoría seleccionada
- ✅ Al menos 1 ingrediente en la receta
- ✅ Unidades de ingredientes deben coincidir

### Al vender ItemMenu:
- ✅ Item debe estar activo
- ✅ Item debe estar disponible
- ✅ Verificar stock ANTES de descontar
- ✅ Operación transaccional (todo o nada)

### Al eliminar Presentation:
- ✅ No se puede eliminar si está en uso por ItemMenu

### Al eliminar Ingredient:
- ✅ No se puede eliminar si está en uso en recetas

---

## 📊 QUERIES ÚTILES PARA TESTING

```sql
-- Ver todos los items con sus recetas
SELECT 
    im.name AS platillo,
    ing.name AS ingrediente,
    ii.quantity,
    ii.unit,
    ing.current_stock
FROM item_menu im
JOIN item_ingredients ii ON im.id_item_menu = ii.id_item_menu
JOIN ingredients ing ON ii.id_ingredient = ing.id_ingredient
ORDER BY im.name;

-- Items que no se pueden preparar (sin stock)
SELECT im.* 
FROM item_menu im
WHERE im.active = TRUE AND im.available = FALSE;

-- Simular venta y ver impacto en inventario
SELECT 
    ing.name,
    ing.current_stock AS antes,
    (ing.current_stock - (ii.quantity * 2)) AS despues_de_vender_2
FROM item_ingredients ii
JOIN ingredients ing ON ii.id_ingredient = ing.id_ingredient
WHERE ii.id_item_menu = 1;
```

---

## ✅ CHECKLIST FINAL

- [ ] PresentationServiceImpl creado
- [ ] ItemMenuService + Impl creados
- [ ] PresentationController creado
- [ ] ItemMenuController creado
- [ ] Vistas de menu-items creadas
- [ ] Vistas de presentations creadas
- [ ] Sidebar actualizado
- [ ] Script SQL ejecutado
- [ ] Datos de prueba insertados
- [ ] Probado flujo completo:
  - [ ] Crear presentaciones
  - [ ] Crear item del menú con receta
  - [ ] Vender item y verificar descuento de inventario
  - [ ] Item se marca como no disponible cuando falta stock

---

## 🎓 RECURSOS ADICIONALES

- Ver `RestaurantTableController.java` como ejemplo de controlador
- Ver `RestaurantTableServiceImpl.java` como ejemplo de servicio
- Ver `admin/tables/list.html` como ejemplo de vista lista
- Ver `admin/tables/form.html` como ejemplo de formulario

---

¡Éxito con la implementación! 🚀
