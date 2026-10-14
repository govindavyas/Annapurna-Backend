ALTER TABLE store_settings ADD COLUMN banner_title VARCHAR(120);
ALTER TABLE store_settings ADD COLUMN banner_subtitle VARCHAR(180);
ALTER TABLE store_settings ADD COLUMN banner_image_url VARCHAR(500);
ALTER TABLE store_settings ADD COLUMN offer_enabled BOOLEAN;
ALTER TABLE store_settings ADD COLUMN offer_title VARCHAR(100);
ALTER TABLE store_settings ADD COLUMN offer_description VARCHAR(220);
ALTER TABLE store_settings ADD COLUMN offer_code VARCHAR(40);

UPDATE store_settings
SET
    banner_title = COALESCE(NULLIF(shop_name, ''), 'Local Food Kitchen'),
    banner_subtitle = 'Freshly cooked favorites delivered hot to your doorstep',
    offer_enabled = FALSE
WHERE id = 1;

ALTER TABLE store_settings ALTER COLUMN banner_title SET NOT NULL;
ALTER TABLE store_settings ALTER COLUMN banner_subtitle SET NOT NULL;
ALTER TABLE store_settings ALTER COLUMN offer_enabled SET NOT NULL;

UPDATE menu_items
SET image_url = CASE
    WHEN lower(name) LIKE '%idli%' THEN 'https://images.unsplash.com/photo-1668236543090-82eba5ee5976?auto=format&fit=crop&w=900&q=80'
    WHEN lower(name) LIKE '%dosa%' THEN 'https://images.unsplash.com/photo-1589301760014-d929f3979dbc?auto=format&fit=crop&w=900&q=80'
    WHEN lower(name) LIKE '%vada%' THEN 'https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=900&q=80'
    WHEN lower(name) LIKE '%meals%' THEN 'https://images.unsplash.com/photo-1512058564366-18510be2db19?auto=format&fit=crop&w=900&q=80'
    ELSE image_url
END
WHERE image_url IS NULL;
