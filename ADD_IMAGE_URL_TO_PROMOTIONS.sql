-- Script para agregar el campo image_url a la tabla promotions
-- Ejecutar este script en la base de datos

ALTER TABLE promotions 
ADD COLUMN image_url VARCHAR(500) NULL 
COMMENT 'URL de la imagen personalizada para mostrar en la landing page';

-- Verificar que la columna fue agregada correctamente
DESCRIBE promotions;
