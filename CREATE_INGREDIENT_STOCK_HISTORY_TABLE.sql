-- Tabla para el historial de adiciones de stock de ingredientes
-- Registra cada vez que se agrega stock con su costo correspondiente

CREATE TABLE IF NOT EXISTS ingredient_stock_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ingredient_id BIGINT NOT NULL,
    quantity_added DECIMAL(10,3) NOT NULL,
    cost_per_unit DECIMAL(10,2) NOT NULL,
    total_cost DECIMAL(12,2) NOT NULL,
    previous_stock DECIMAL(10,3),
    new_stock DECIMAL(10,3),
    added_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    added_by BIGINT,
    
    CONSTRAINT fk_stock_history_ingredient 
        FOREIGN KEY (ingredient_id) REFERENCES ingredient(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_stock_history_employee 
        FOREIGN KEY (added_by) REFERENCES employee(id) 
        ON DELETE SET NULL,
    
    INDEX idx_ingredient_added_at (ingredient_id, added_at DESC),
    INDEX idx_added_at (added_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Comentarios de la tabla
ALTER TABLE ingredient_stock_history COMMENT = 'Historial de adiciones de stock de ingredientes con sus costos';
