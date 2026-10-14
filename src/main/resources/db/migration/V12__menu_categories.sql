ALTER TABLE menu_items ALTER COLUMN category TYPE VARCHAR(100);
CREATE TABLE menu_categories (
    name VARCHAR(100) PRIMARY KEY,
    normalized_name VARCHAR(100) NOT NULL UNIQUE
);
INSERT INTO menu_categories(name, normalized_name)
SELECT MIN(category), LOWER(TRIM(category)) FROM menu_items GROUP BY LOWER(TRIM(category));
UPDATE menu_items SET category = (SELECT name FROM menu_categories WHERE normalized_name = LOWER(TRIM(menu_items.category)));
ALTER TABLE menu_items ADD CONSTRAINT fk_menu_category FOREIGN KEY (category) REFERENCES menu_categories(name);
