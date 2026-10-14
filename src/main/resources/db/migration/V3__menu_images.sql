ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS image_url VARCHAR(512);

UPDATE menu_items
SET image_url = CASE name
    WHEN 'Idli Set' THEN 'https://images.unsplash.com/photo-1589301760014-d929f3979dbc?auto=format&fit=crop&w=900&q=80'
    WHEN 'Masala Dosa' THEN 'https://images.unsplash.com/photo-1668236543090-82eba5ee5976?auto=format&fit=crop&w=900&q=80'
    WHEN 'South Meals' THEN 'https://images.unsplash.com/photo-1617093727343-374698b1b08d?auto=format&fit=crop&w=900&q=80'
    WHEN 'Medu Vada' THEN 'https://images.unsplash.com/photo-1585937421612-70a008356fbe?auto=format&fit=crop&w=900&q=80'
    ELSE image_url
END
WHERE image_url IS NULL;
