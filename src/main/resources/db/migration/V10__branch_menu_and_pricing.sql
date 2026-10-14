ALTER TABLE branches ADD COLUMN delivery_fee_paise INTEGER NOT NULL DEFAULT 3000;
ALTER TABLE branches ADD COLUMN minimum_order_paise INTEGER NOT NULL DEFAULT 0;
ALTER TABLE branches ADD CONSTRAINT ck_branch_delivery_fee CHECK (delivery_fee_paise >= 0);
ALTER TABLE branches ADD CONSTRAINT ck_branch_minimum_order CHECK (minimum_order_paise >= 0);
ALTER TABLE menu_items ADD COLUMN all_branches BOOLEAN NOT NULL DEFAULT TRUE;
CREATE TABLE menu_item_branches (
    menu_item_id UUID NOT NULL REFERENCES menu_items(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches(id),
    PRIMARY KEY (menu_item_id, branch_id)
);
CREATE INDEX idx_menu_item_branches_branch ON menu_item_branches(branch_id);
