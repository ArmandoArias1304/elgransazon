# Feature: URL de Imagen Personalizada para Promociones

## 📋 Descripción

Se ha implementado la capacidad de agregar una URL de imagen personalizada para cada promoción. Anteriormente, las tarjetas de promoción en la landing page mostraban la imagen del primer producto asociado a la promoción. Ahora cada promoción puede tener su propia imagen única mediante una URL.

## 🎯 Objetivo

Permitir que cada promoción tenga una imagen personalizada y atractiva en la landing page, independiente de los productos asociados a la promoción.

## 🔧 Cambios Realizados

### 1. **Backend - Entidad Promotion**

**Archivo:** `src/main/java/com/aatechsolutions/elgransazon/domain/entity/Promotion.java`

Se agregó el campo `imageUrl`:

```java
@Size(max = 500, message = "La URL de la imagen no puede exceder 500 caracteres")
@Column(name = "image_url", length = 500)
private String imageUrl;
```

### 2. **Base de Datos**

**Archivo:** `ADD_IMAGE_URL_TO_PROMOTIONS.sql`

Script SQL para agregar la columna:

```sql
ALTER TABLE promotions
ADD COLUMN image_url VARCHAR(500) NULL
COMMENT 'URL de la imagen personalizada para mostrar en la landing page';
```

### 3. **Frontend - Formulario de Promociones**

**Archivo:** `src/main/resources/templates/admin/promotions/form.html`

Se agregó un nuevo campo en el formulario:

```html
<div class="md:col-span-2">
  <label
    class="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2"
  >
    URL de la Imagen de la Promoción
  </label>
  <input
    type="url"
    th:field="*{imageUrl}"
    placeholder="https://ejemplo.com/imagen-promocion.jpg"
    class="w-full rounded-lg border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
  />
  <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
    Ingrese la URL de la imagen que se mostrará en la landing page para esta
    promoción
  </p>
</div>
```

### 4. **Frontend - Landing Page**

**Archivo:** `src/main/resources/templates/home/landing.html`

Se actualizaron las 3 tarjetas de promoción para usar `imageUrl` de la promoción:

**Promoción BUY_X_PAY_Y (2x1, 3x2):**

```html
<img
  th:src="${promoCombo.imageUrl != null and !promoCombo.imageUrl.isEmpty()} ? ${promoCombo.imageUrl} : 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=600&q=80'"
  th:alt="${promoCombo.name}"
  class="promo-image"
/>
```

**Promoción PERCENTAGE_DISCOUNT (30%, 50% OFF):**

```html
<img
  th:src="${promoPercent.imageUrl != null and !promoPercent.imageUrl.isEmpty()} ? ${promoPercent.imageUrl} : 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600&q=80'"
  th:alt="${promoPercent.name}"
  class="promo-image"
/>
```

**Promoción FIXED_AMOUNT_DISCOUNT ($20 OFF):**

```html
<img
  th:src="${promoFixed.imageUrl != null and !promoFixed.imageUrl.isEmpty()} ? ${promoFixed.imageUrl} : 'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=600&q=80'"
  th:alt="${promoFixed.name}"
  class="promo-image"
/>
```

## 📝 Instrucciones de Uso

### Paso 1: Ejecutar el Script SQL

```sql
-- Conectarse a la base de datos y ejecutar:
source ADD_IMAGE_URL_TO_PROMOTIONS.sql;
```

O manualmente:

```sql
ALTER TABLE promotions
ADD COLUMN image_url VARCHAR(500) NULL;
```

### Paso 2: Crear o Editar una Promoción

1. Ir a **Admin → Promociones → Nueva Promoción** (o editar existente)
2. Completar los datos de la promoción como de costumbre
3. En el nuevo campo **"URL de la Imagen de la Promoción"**, ingresar la URL de la imagen
   - Ejemplo: `https://ejemplo.com/promo-verano-2024.jpg`
4. Guardar la promoción

### Paso 3: Verificar en la Landing Page

1. Ir a la landing page: `http://localhost:8080/`
2. Hacer scroll hasta la sección **"Promociones Especiales"**
3. Verificar que la imagen personalizada se muestre en la tarjeta de promoción

## ✨ Características

- ✅ Campo opcional: Si no se proporciona URL, se usa una imagen por defecto de Unsplash
- ✅ Validación: La URL no puede exceder 500 caracteres
- ✅ Soporte para los 3 tipos de promoción:
  - Buy X Pay Y (2x1, 3x2, etc.)
  - Porcentaje de descuento (20% OFF, 50% OFF)
  - Descuento fijo ($5 OFF, $10 OFF)
- ✅ Compatibilidad con modo oscuro en el formulario
- ✅ Placeholder con ejemplo de URL en el campo

## 🖼️ Imágenes Recomendadas

Para mejores resultados visuales:

- **Dimensiones:** 600x400px o superior (relación 3:2)
- **Formato:** JPG, PNG, WEBP
- **Tamaño:** Menos de 500KB para carga rápida
- **Hosting:** Usar servicios como:
  - Cloudinary
  - ImgBB
  - AWS S3
  - Google Cloud Storage
  - Imgur

## 🔄 Migración de Datos Existentes

Las promociones existentes seguirán funcionando normalmente:

- Si `imageUrl` es NULL o vacío → Usa imagen por defecto
- Si `imageUrl` tiene valor → Usa la imagen personalizada

No es necesario actualizar promociones existentes a menos que desees agregar imágenes personalizadas.

## 🐛 Troubleshooting

### La imagen no se muestra

1. Verificar que la URL sea accesible públicamente
2. Verificar que la URL use protocolo HTTPS
3. Revisar la consola del navegador para errores CORS
4. Asegurarse de que el servidor de imágenes permita hotlinking

### Error al guardar la promoción

1. Verificar que la URL no exceda 500 caracteres
2. Verificar que el formato de URL sea válido
3. Revisar logs del servidor para mensajes de error

## 📊 Impacto

- **Base de datos:** +1 columna en tabla `promotions`
- **Formulario:** +1 campo en el form de promociones
- **Landing page:** Mejora visual en las tarjetas de promoción
- **Performance:** Sin impacto (carga lazy de imágenes)

## 🎨 Ejemplos de URLs

```
https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=600&q=80
https://ejemplo.com/assets/promo-navidad-2024.jpg
https://cdn.cloudinary.com/restaurante/image/upload/v1234567890/promo-verano.jpg
```

## ✅ Checklist de Implementación

- [x] Agregar campo `imageUrl` a entidad Promotion
- [x] Crear script SQL para agregar columna
- [x] Actualizar formulario de promociones
- [x] Actualizar landing page (3 tipos de promoción)
- [x] Agregar validaciones en el campo
- [x] Agregar placeholder y texto de ayuda
- [x] Mantener compatibilidad con datos existentes
- [x] Documentar la feature

## 🚀 Próximas Mejoras (Opcional)

- [ ] Upload de imágenes directo al servidor (sin URLs externas)
- [ ] Preview de la imagen en el formulario
- [ ] Galería de imágenes predefinidas para elegir
- [ ] Validación de que la URL apunte a una imagen válida
- [ ] Redimensionamiento automático de imágenes
