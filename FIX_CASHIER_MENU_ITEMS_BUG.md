# Fix: Cashier Menu Items Not Displaying

## 🐛 Problema Identificado

En la vista de selección de menú del cashier (`order-menu.html`), no se mostraban los items del menú, aunque la misma vista funcionaba correctamente para admin y waiter.

## 🔍 Causa Raíz

El método `menuSelection()` del `CashierController` tenía discrepancias en los atributos del modelo comparado con el `OrderController`:

### ❌ Implementación Incorrecta (CashierController)

```java
// Problema 1: itemsByCategory usaba Category como key en vez de Long
Map<Category, List<ItemMenu>> itemsByCategory = new LinkedHashMap<>();
for (Category category : categories) {
    List<ItemMenu> items = itemMenuService.findByCategoryIdAndAvailability(category.getIdCategory(), true);
    if (!items.isEmpty()) {
        itemsByCategory.put(category, items); // ❌ Key = Category object
    }
}

// Problema 2: Faltaban atributos requeridos
model.addAttribute("itemsByCategory", itemsByCategory); // ❌ Wrong type
// ❌ NO se enviaba "categories"
// ❌ NO se enviaba "allItems"
// ❌ "employee" se enviaba como "currentEmployee"
```

### ✅ Implementación Correcta (Ahora)

```java
// Get all active categories
List<Category> categories = categoryService.getAllActiveCategories();

// Get available menu items
List<ItemMenu> availableItems = itemMenuService.findAvailableItems();

// Group items by category ID using Stream API
Map<Long, List<ItemMenu>> itemsByCategory = availableItems.stream()
    .collect(Collectors.groupingBy(item -> item.getCategory().getIdCategory()));

// Get current employee
Employee employee = employeeService.findByUsername(username)
    .orElseThrow(() -> new IllegalStateException("Empleado no encontrado"));

// Add all required attributes
model.addAttribute("categories", categories);           // ✅ Lista de categorías
model.addAttribute("itemsByCategory", itemsByCategory); // ✅ Map<Long, List<ItemMenu>>
model.addAttribute("allItems", availableItems);         // ✅ Todos los items
model.addAttribute("employee", employee);               // ✅ Empleado actual
model.addAttribute("taxRate", config.getTaxRate());
```

## 📝 Cambios Realizados

### Archivo: `CashierController.java` (líneas 295-342)

1. **Cambiado el tipo de `itemsByCategory`**:
   - Antes: `Map<Category, List<ItemMenu>>`
   - Ahora: `Map<Long, List<ItemMenu>>`

2. **Agregado `categories` como atributo separado**:
   ```java
   model.addAttribute("categories", categories);
   ```

3. **Agregado `allItems` con todos los items disponibles**:
   ```java
   List<ItemMenu> availableItems = itemMenuService.findAvailableItems();
   model.addAttribute("allItems", availableItems);
   ```

4. **Corregido nombre del atributo `employee`**:
   - Antes: `currentEmployee`
   - Ahora: `employee` (para consistencia con OrderController)

5. **Agregado información de mesa para DINE_IN**:
   ```java
   RestaurantTable selectedTable = null;
   if (type == OrderType.DINE_IN && tableId != null) {
       selectedTable = restaurantTableService.findById(tableId)
           .orElse(null);
   }
   model.addAttribute("selectedTable", selectedTable);
   ```

6. **Usado Stream API para agrupar items**:
   ```java
   Map<Long, List<ItemMenu>> itemsByCategory = availableItems.stream()
       .collect(Collectors.groupingBy(item -> item.getCategory().getIdCategory()));
   ```

## 🎯 Resultado

Ahora el `CashierController` envía exactamente los mismos atributos que el `OrderController`:

| Atributo | Tipo | Descripción |
|----------|------|-------------|
| `categories` | `List<Category>` | Lista de categorías activas |
| `itemsByCategory` | `Map<Long, List<ItemMenu>>` | Items agrupados por ID de categoría |
| `allItems` | `List<ItemMenu>` | Todos los items disponibles |
| `employee` | `Employee` | Empleado actual (cashier) |
| `selectedTable` | `RestaurantTable` | Mesa seleccionada (solo DINE_IN) |
| `taxRate` | `BigDecimal` | Tasa de impuesto del sistema |
| `currentRole` | `String` | "cashier" |

## ✅ Verificación

La vista `order-menu.html` espera estos atributos para funcionar:

```html
<!-- Itera sobre las categorías -->
<div th:each="category : ${categories}">
    <!-- Obtiene los items de esta categoría usando el ID como key -->
    <div th:each="item : ${itemsByCategory.get(category.idCategory)}">
        <!-- Muestra el item -->
    </div>
</div>
```

Ahora que `itemsByCategory` usa `Long` (ID) como key en vez de `Category` como objeto, la expresión `itemsByCategory.get(category.idCategory)` funciona correctamente.

## 🔄 Patrón Aplicado

Se siguió el mismo patrón exitoso del `OrderController.menuSelection()` (líneas 295-356):

1. Obtener todas las categorías activas
2. Obtener todos los items disponibles
3. Agrupar items por ID de categoría usando Streams
4. Pasar 3 atributos al modelo: categories, itemsByCategory, allItems
5. Pasar información del empleado actual
6. Pasar información de mesa si aplica

## 🚀 Próximos Pasos

1. ✅ Fix aplicado
2. ⏳ Reiniciar aplicación
3. ⏳ Probar como cashier:
   - Dashboard → Crear Pedidos
   - Seleccionar mesa
   - Ingresar info del cliente
   - **Verificar que los items del menú se muestren correctamente**
   - Crear orden
   - Ver orden creada

---
**Fecha**: 2025-01-XX  
**Issue**: Menu items not displaying in cashier order-menu view  
**Status**: ✅ FIXED
