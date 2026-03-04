-- Pre-loaded demo data for Swagger UI demonstration

-- Products (catalog)
INSERT INTO product (id, name, description, sku, category, active, amount, currency)
VALUES (1, 'MacBook Pro 16"', 'Apple MacBook Pro with M3 Pro chip, 18GB RAM, 512GB SSD', 'ELEC-MBP-001', 'ELECTRONICS', true, 2499.00, 'EUR');
INSERT INTO product (id, name, description, sku, category, active, amount, currency)
VALUES (2, 'Wireless Headphones', 'Sony WH-1000XM5 noise-cancelling headphones', 'ELEC-WH-002', 'ELECTRONICS', true, 379.00, 'EUR');
INSERT INTO product (id, name, description, sku, category, active, amount, currency)
VALUES (3, 'Clean Code', 'Robert C. Martin - A Handbook of Agile Software Craftsmanship', 'BOOK-CC-001', 'BOOKS', true, 35.00, 'EUR');
INSERT INTO product (id, name, description, sku, category, active, amount, currency)
VALUES (4, 'Running Shoes', 'Nike Air Zoom Pegasus 41', 'SPRT-RS-001', 'SPORTS', true, 129.00, 'EUR');
INSERT INTO product (id, name, description, sku, category, active, amount, currency)
VALUES (5, 'Organic Coffee Beans', '1kg premium arabica beans from Colombia', 'FOOD-CB-001', 'FOOD', true, 24.50, 'EUR');

-- Inventory
INSERT INTO inventory (id, product_id, quantity_on_hand, reserved_quantity, reorder_threshold)
VALUES (1, 1, 50, 0, 10);
INSERT INTO inventory (id, product_id, quantity_on_hand, reserved_quantity, reorder_threshold)
VALUES (2, 2, 120, 0, 20);
INSERT INTO inventory (id, product_id, quantity_on_hand, reserved_quantity, reorder_threshold)
VALUES (3, 3, 200, 0, 30);
INSERT INTO inventory (id, product_id, quantity_on_hand, reserved_quantity, reorder_threshold)
VALUES (4, 4, 75, 0, 15);
INSERT INTO inventory (id, product_id, quantity_on_hand, reserved_quantity, reorder_threshold)
VALUES (5, 5, 300, 0, 50);

-- Customers
INSERT INTO customer (id, first_name, last_name, email, phone, street, city, zip_code, country)
VALUES (1, 'Alice', 'Martin', 'alice.martin@example.com', '+33612345678', '12 Rue de la Paix', 'Paris', '75002', 'FR');
INSERT INTO customer (id, first_name, last_name, email, phone, street, city, zip_code, country)
VALUES (2, 'Bob', 'Dupont', 'bob.dupont@example.com', '+33698765432', '45 Avenue des Champs', 'Lyon', '69001', 'FR');
