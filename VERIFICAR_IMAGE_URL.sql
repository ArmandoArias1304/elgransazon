-- Script para verificar que el campo image_url existe y tiene datos

-- 1. Verificar estructura de la tabla
DESCRIBE promotions;

-- 2. Ver todas las promociones con sus image_url
SELECT 
    id_promotion,
    name,
    promotion_type,
    image_url,
    active,
    start_date,
    end_date
FROM promotions
ORDER BY priority DESC, id_promotion DESC;

-- 3. Verificar promociones activas HOY con image_url
SELECT 
    id_promotion,
    name,
    promotion_type,
    image_url,
    CASE 
        WHEN image_url IS NULL THEN '❌ NULL'
        WHEN image_url = '' THEN '❌ VACÍO'
        ELSE '✅ TIENE URL'
    END as estado_imagen
FROM promotions
WHERE active = 1
  AND CURDATE() BETWEEN start_date AND end_date
ORDER BY priority DESC;

-- 4. Contar promociones por estado de image_url
SELECT 
    CASE 
        WHEN image_url IS NULL THEN 'NULL'
        WHEN image_url = '' THEN 'VACÍO'
        ELSE 'CON URL'
    END as estado,
    COUNT(*) as cantidad
FROM promotions
GROUP BY 
    CASE 
        WHEN image_url IS NULL THEN 'NULL'
        WHEN image_url = '' THEN 'VACÍO'
        ELSE 'CON URL'
    END;
