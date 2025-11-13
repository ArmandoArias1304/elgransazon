-- =====================================================
-- Create Promotion-Items Junction Table
-- =====================================================
-- This table manages the Many-to-Many relationship
-- between Promotions and Menu Items
-- =====================================================

CREATE TABLE IF NOT EXISTS promotion_items (
    id_promotion BIGINT NOT NULL,
    id_item_menu BIGINT NOT NULL,
    
    -- Composite Primary Key
    PRIMARY KEY (id_promotion, id_item_menu),
    
    -- Foreign Keys
    CONSTRAINT fk_promotion_items_promotion 
        FOREIGN KEY (id_promotion) 
        REFERENCES promotions(id_promotion) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_promotion_items_item 
        FOREIGN KEY (id_item_menu) 
        REFERENCES item_menu(id_item_menu) 
        ON DELETE CASCADE,
    
    -- Indexes for performance
    INDEX idx_promotion_items_promotion (id_promotion),
    INDEX idx_promotion_items_item (id_item_menu)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Comments
-- =====================================================

ALTER TABLE promotion_items COMMENT = 'Junction table linking promotions with menu items (Many-to-Many relationship)';
