-- =====================================================
-- Script: Agregar columna requires_preparation a item_menu
-- Fecha: 2025-11-09
-- Descripción: Permite marcar items que NO requieren preparación del chef
--              (ej: refrescos, bebidas embotelladas, postres comprados)
-- =====================================================

-- Agregar columna requires_preparation (por defecto TRUE)
ALTER TABLE item_menu 
ADD COLUMN requires_preparation BOOLEAN NOT NULL DEFAULT TRUE
COMMENT 'Indica si el item requiere preparación del chef. FALSE para items listos como refrescos.';

-- Actualizar items que NO requieren preparación (ejemplos comunes)
-- Puedes ajustar esta lista según tus productos

-- Refrescos y bebidas embotelladas
UPDATE item_menu 
SET requires_preparation = FALSE 
WHERE LOWER(name) LIKE '%coca%' 
   OR LOWER(name) LIKE '%pepsi%'
   OR LOWER(name) LIKE '%sprite%'
   OR LOWER(name) LIKE '%fanta%'
   OR LOWER(name) LIKE '%refresco%'
   OR LOWER(name) LIKE '%gaseosa%'
   OR LOWER(name) LIKE '%agua%'
   OR LOWER(name) LIKE '%cerveza%'
   OR LOWER(name) LIKE '%vino%'
   OR LOWER(name) LIKE '%jugo embotellado%'
   OR LOWER(name) LIKE '%té frío%';

-- Postres y productos empacados (ajustar según tu menú)
UPDATE item_menu 
SET requires_preparation = FALSE 
WHERE LOWER(name) LIKE '%helado comprado%'
   OR LOWER(name) LIKE '%postre empacado%'
   OR LOWER(name) LIKE '%dulce%'
   OR LOWER(name) LIKE '%snack%';

-- Verificar cambios
SELECT 
    id_item_menu,
    name,
    requires_preparation,
    CASE 
        WHEN requires_preparation = TRUE THEN 'Requiere preparación (Chef)'
        ELSE 'Listo para servir (Sin Chef)'
    END as estado
FROM item_menu
ORDER BY requires_preparation DESC, name;

-- =====================================================
-- Notas de uso:
-- =====================================================
-- 1. Items con requires_preparation = TRUE: 
--    - Flujo normal: PENDING → IN_PREPARATION → READY → DELIVERED
--    - El chef debe aceptarlos y prepararlos
--
-- 2. Items con requires_preparation = FALSE:
--    - Flujo directo: PENDING → READY (automático) → DELIVERED
--    - El chef NO los verá en su lista
--    - Pasan automáticamente a estado READY
--
-- 3. Para actualizar un item manualmente:
--    UPDATE item_menu SET requires_preparation = FALSE WHERE name = 'Coca-Cola';
-- =====================================================
