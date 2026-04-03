-- Create join table for Product <-> Category many-to-many association.
CREATE TABLE IF NOT EXISTS product_categories (
    product_id VARCHAR(36) NOT NULL,
    category_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (product_id, category_id),
    CONSTRAINT fk_product_categories_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_categories_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

-- Backfill existing single-category links into the join table.
INSERT INTO product_categories (product_id, category_id)
SELECT p.id, p.category_id
FROM products p
WHERE p.category_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM product_categories pc
      WHERE pc.product_id = p.id AND pc.category_id = p.category_id
  );

