-- =====================================================
-- CREATE REVIEWS TABLE
-- Sistema de reseñas de clientes con aprobación por admin
-- =====================================================

CREATE TABLE IF NOT EXISTS reviews (
    id_review BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by BIGINT NULL,
    approved_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_review_customer FOREIGN KEY (customer_id) 
        REFERENCES customers(id_customer) ON DELETE CASCADE,
    CONSTRAINT fk_review_approved_by FOREIGN KEY (approved_by) 
        REFERENCES employee(id_empleado) ON DELETE SET NULL,
    
    -- Unique Constraint: Un cliente solo puede tener una reseña
    CONSTRAINT uk_review_customer UNIQUE (customer_id),
    
    -- Check Constraints
    CONSTRAINT chk_review_rating CHECK (rating >= 1 AND rating <= 5),
    CONSTRAINT chk_review_comment_length CHECK (CHAR_LENGTH(comment) >= 10 AND CHAR_LENGTH(comment) <= 500),
    CONSTRAINT chk_review_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índices para optimizar consultas
CREATE INDEX idx_review_status ON reviews(status);
CREATE INDEX idx_review_customer ON reviews(customer_id);
CREATE INDEX idx_review_created_at ON reviews(created_at DESC);

-- =====================================================
-- COMENTARIOS
-- =====================================================
-- Esta tabla almacena las reseñas de los clientes
-- - Un cliente solo puede tener UNA reseña (enforced por uk_review_customer)
-- - Si el cliente quiere cambiar su reseña, debe actualizar la existente
-- - Al actualizar, el status vuelve a PENDING para re-aprobación
-- - Solo las reseñas APPROVED se muestran en la landing page
-- - Los admins/managers pueden aprobar o rechazar reseñas
-- - rating: 1-5 estrellas (validado con CHECK constraint)
-- - comment: 10-500 caracteres (validado con CHECK constraint)
-- - status: PENDING (inicial), APPROVED (visible en landing), REJECTED
-- - approved_by: empleado que aprobó/rechazó (puede ser NULL)
-- - approved_at: fecha de aprobación/rechazo (puede ser NULL)
-- =====================================================
