# 🔧 Feature: Campo `requiresPreparation` en Items del Menú

## 📋 Descripción General

Se ha implementado un nuevo campo `requiresPreparation` en la entidad `ItemMenu` que permite distinguir entre items que requieren preparación del chef (pizzas, hamburguesas, platos calientes) e items que están listos para servir (refrescos, bebidas embotelladas, postres comprados).

Esta funcionalidad optimiza el flujo de trabajo del chef, quien **solo verá items que realmente debe preparar**.

---

## 🎯 Problema Resuelto

**Antes:**

- El chef veía TODOS los pedidos, incluso los que solo contenían refrescos o bebidas embotelladas
- Órdenes con solo Coca-Cola aparecían en la vista del chef innecesariamente
- El chef perdía tiempo revisando items que no requieren preparación

**Ahora:**

- El chef **SOLO** ve pedidos con al menos un item que requiere preparación
- Pedidos con únicamente refrescos/bebidas **NO aparecen** en la vista del chef
- Flujo automático: items sin preparación pasan directamente a estado READY

---

## ⚙️ Cambios Técnicos Implementados

### 1. **Base de Datos**

```sql
ALTER TABLE item_menu
ADD COLUMN requires_preparation BOOLEAN NOT NULL DEFAULT TRUE;
```

### 2. **Entidad ItemMenu.java**

```java
@Column(name = "requires_preparation", nullable = false)
@Builder.Default
private Boolean requiresPreparation = true;
```

### 3. **Formulario de Items** (`admin/menu-items/form.html`)

- Nuevo checkbox: "¿Requiere preparación del chef?"
- Con descripción clara de cuándo marcar/desmarcar
- Color naranja para diferenciarlo visualmente

### 4. **Lógica del Chef** (`ChefOrderServiceImpl.java`)

- Filtrado automático en `findAll()`: solo muestra orders con items que requieren preparación
- Método helper `hasItemsRequiringPreparation()`

### 5. **Lógica de Pedidos** (`OrderServiceImpl.java`)

- Nuevo método: `autoAdvanceOrderIfNoPreparationNeeded()`
- Al crear pedido: si TODOS los items tienen `requiresPreparation=false`, el pedido pasa automáticamente a READY
- Al agregar items: los que tienen `requiresPreparation=false` se marcan automáticamente como READY

---

## 📊 Flujos de Estado

### Flujo 1: Item CON preparación (requiresPreparation = TRUE)

```
Ejemplo: Pizza, Hamburguesa, Pasta

PENDING → (Chef acepta) → IN_PREPARATION → (Chef termina) → READY → DELIVERED → PAID
```

### Flujo 2: Item SIN preparación (requiresPreparation = FALSE)

```
Ejemplo: Coca-Cola, Cerveza, Agua embotellada

PENDING → (Automático) → READY → DELIVERED → PAID
```

### Flujo 3: Pedido MIXTO (algunos items con y otros sin preparación)

```
Ejemplo: Pizza + Coca-Cola

Pizza:       PENDING → IN_PREPARATION → READY
Coca-Cola:   PENDING → (Auto) READY

Pedido:      PENDING → IN_PREPARATION → READY → DELIVERED → PAID
```

---

## 🎨 Uso en el Sistema

### **Para Administradores:**

#### 1. Crear/Editar Item del Menú

1. Ir a **Menú → Items del Menú → Nuevo/Editar**
2. Llenar información del producto
3. **Checkbox "¿Requiere preparación del chef?"**:
   - ✅ **Marcado (DEFAULT)**: Pizzas, hamburguesas, platos calientes, ensaladas preparadas
   - ❌ **Desmarcado**: Coca-Cola, Pepsi, cervezas, jugos embotellados, postres comprados

#### 2. Actualizar Items Existentes

Ejecutar el script SQL incluido para marcar automáticamente refrescos comunes:

```bash
# Ver archivo: ADD_REQUIRES_PREPARATION_COLUMN.sql
```

### **Para el Chef:**

#### Vista del Chef (Pedidos Pendientes)

- **ANTES**: Veía 50 pedidos, 20 de ellos solo con refrescos
- **AHORA**: Solo ve 30 pedidos que realmente necesitan preparación
- Items sin preparación **NO aparecen** en su lista

#### Indicador Visual

Los items que el chef ve en su lista **garantizan** que al menos uno requiere preparación.

---

## 📝 Ejemplos de Clasificación

### ✅ Items que REQUIEREN preparación (`requiresPreparation = TRUE`)

```
🍕 Pizzas (Napolitana, Hawaiana, Pepperoni)
🍔 Hamburguesas (Clásica, BBQ, Vegetariana)
🍝 Pastas (Carbonara, Alfredo, Bolognesa)
🥗 Ensaladas preparadas (César, Griega, Mixta)
🍳 Platos calientes (Arroz con pollo, Bandeja paisa)
🥘 Sopas (Sancocho, Ajiaco, Consomé)
🌮 Tacos, Burritos, Quesadillas
🍰 Postres hechos en casa (Tiramisú casero, Flan)
```

### ❌ Items que NO requieren preparación (`requiresPreparation = FALSE`)

```
🥤 Refrescos (Coca-Cola, Pepsi, Sprite, Fanta)
🍺 Cervezas embotelladas (Corona, Heineken, Poker)
🧃 Jugos embotellados (Hit, Del Valle, Postobón)
💧 Agua embotellada (Brisa, Cristal, Oasis)
🍷 Vinos embotellados
🧊 Bebidas frías embotelladas
🍬 Dulces empacados
🍰 Postres comprados (Cheesecake factory, helados)
🥜 Snacks empacados (Papas, Doritos, Nachos)
```

---

## 🔍 Verificación del Sistema

### 1. **Verificar en Base de Datos**

```sql
-- Ver todos los items y su configuración
SELECT
    id_item_menu,
    name,
    requires_preparation,
    CASE
        WHEN requires_preparation = TRUE THEN '👨‍🍳 Chef debe preparar'
        ELSE '✅ Listo para servir'
    END as estado
FROM item_menu
ORDER BY requires_preparation DESC, name;
```

### 2. **Probar Flujo Completo**

#### Test 1: Pedido solo con refrescos

```
1. Crear pedido: Mesa #5, 2x Coca-Cola
2. Resultado esperado:
   - Estado del pedido: READY (automático)
   - Chef NO ve este pedido
   - Mesero puede servir inmediatamente
```

#### Test 2: Pedido solo con items que requieren preparación

```
1. Crear pedido: Mesa #3, 1x Pizza Napolitana
2. Resultado esperado:
   - Estado del pedido: PENDING
   - Chef VE este pedido en su lista
   - Chef debe aceptar y preparar
```

#### Test 3: Pedido mixto

```
1. Crear pedido: Mesa #7, 1x Hamburguesa + 1x Coca-Cola
2. Resultado esperado:
   - Estado del pedido: PENDING
   - Chef VE este pedido (por la hamburguesa)
   - Coca-Cola: inmediatamente READY
   - Hamburguesa: PENDING → IN_PREPARATION → READY
   - Cuando hamburguesa esté READY, todo el pedido pasa a READY
```

---

## 🎛️ Configuración Recomendada

### Items Comunes en Restaurantes

```sql
-- Marcar refrescos como NO requieren preparación
UPDATE item_menu
SET requires_preparation = FALSE
WHERE name IN (
    'Coca-Cola', 'Pepsi', 'Sprite', 'Fanta',
    'Cerveza Corona', 'Cerveza Heineken',
    'Agua Brisa', 'Jugo Hit Mango', 'Jugo Hit Mora'
);

-- Verificar cambios
SELECT name, requires_preparation FROM item_menu
WHERE requires_preparation = FALSE;
```

---

## 🚨 Notas Importantes

### 1. **Migración de Datos Existentes**

- Por defecto, TODOS los items tienen `requiresPreparation = TRUE`
- Debes ejecutar el script SQL para actualizar refrescos/bebidas
- O usar el formulario para editar items uno por uno

### 2. **Pedidos en Proceso**

- Los pedidos **ya creados** mantienen su flujo normal
- La lógica solo aplica a **nuevos pedidos** o **nuevos items agregados**

### 3. **Categorías Sugeridas**

No es necesario crear categorías especiales, pero puedes organizarlo así:

```
📁 Bebidas Frías (requiresPreparation = FALSE)
   - Refrescos
   - Cervezas
   - Aguas

📁 Bebidas Calientes (requiresPreparation = TRUE)
   - Café
   - Té
   - Chocolate caliente

📁 Platos Principales (requiresPreparation = TRUE)
   - Todo lo que cocina el chef
```

---

## 📈 Beneficios

✅ **Para el Chef:**

- Lista más limpia y enfocada
- Solo ve lo que debe preparar
- Mejor gestión del tiempo

✅ **Para el Mesero/Cajero:**

- Items sin preparación disponibles inmediatamente
- Servicio más rápido en bebidas
- Mejor experiencia del cliente

✅ **Para el Negocio:**

- Procesos optimizados
- Reducción de tiempos de espera
- Mejor flujo de trabajo

---

## 🔧 Mantenimiento

### Actualizar Item Existente

```sql
-- Marcar un item como SIN preparación
UPDATE item_menu
SET requires_preparation = FALSE
WHERE name = 'Nombre del Item';

-- Marcar un item como CON preparación
UPDATE item_menu
SET requires_preparation = TRUE
WHERE name = 'Nombre del Item';
```

### Actualizar en Masa por Categoría

```sql
-- Si tienes categoría "Bebidas Frías"
UPDATE item_menu
SET requires_preparation = FALSE
WHERE id_category = (SELECT id_category FROM category WHERE name = 'Bebidas Frías');
```

---

## 📞 Soporte

Si encuentras algún comportamiento inesperado:

1. Verifica que el campo `requires_preparation` esté correctamente configurado
2. Revisa los logs del sistema para mensajes como:
   - `"Order {numero} contains ONLY items that don't require preparation"`
   - `"Item '{nombre}' doesn't require preparation. Auto-setting to READY status"`
3. Ejecuta la consulta SQL de verificación

---

**Fecha de implementación:** 2025-11-09  
**Versión:** 1.0  
**Desarrollado por:** AAtech Solutions
