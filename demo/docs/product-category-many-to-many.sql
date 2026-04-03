-- Sample seed data aligned with current JPA entities (MySQL)
USE projectcnpm;

CREATE TABLE IF NOT EXISTS product_categories (
    product_id VARCHAR(36) NOT NULL,
    category_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (product_id, category_id),
    CONSTRAINT fk_product_categories_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_categories_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

SET FOREIGN_KEY_CHECKS = 0;

-- Cleanup in FK-safe order (for rerun)
DELETE FROM mails;
DELETE FROM notifications;
DELETE FROM review_reports;
DELETE FROM discussions;
DELETE FROM reviews;
DELETE FROM import_details;
DELETE FROM import_receipts;
DELETE FROM inventories;
DELETE FROM warranties;
DELETE FROM shippings;
DELETE FROM payments;
DELETE FROM order_status_history;
DELETE FROM order_items;
DELETE FROM orders;
DELETE FROM cart_items;
DELETE FROM carts;
DELETE FROM product_images;
DELETE FROM product_categories;
DELETE FROM products;
DELETE FROM vouchers;
DELETE FROM suppliers;
DELETE FROM categories;
DELETE FROM owners;
DELETE FROM staffs;
DELETE FROM customers;
DELETE FROM users;

SET FOREIGN_KEY_CHECKS = 1;

START TRANSACTION;

-- 1) Users + subtype tables
INSERT INTO users (id, username, password, full_name, email, phone, address, gender, role, is_active, created_at) VALUES
('11111111-1111-1111-1111-111111111101', 'cus_an',   '$2a$12$JRXCc5f2OdjTBCwLotOgle1MLIzpuN7z/mlZoT5EO1LTdrmYdtDuu', 'Nguyen Van An',    'an.customer@example.com',   '0901000001', 'Ha Noi',  'MALE',   'CUSTOMER', 1, '2026-03-01 08:00:00'),
('11111111-1111-1111-1111-111111111102', 'cus_binh', '$2a$12$JRXCc5f2OdjTBCwLotOgle1MLIzpuN7z/mlZoT5EO1LTdrmYdtDuu', 'Tran Thi Binh',    'binh.customer@example.com', '0901000002', 'Da Nang', 'FEMALE', 'CUSTOMER', 1, '2026-03-01 08:10:00'),
('11111111-1111-1111-1111-111111111201', 'stf_dung', '$2a$12$JRXCc5f2OdjTBCwLotOgle1MLIzpuN7z/mlZoT5EO1LTdrmYdtDuu', 'Pham Quoc Dung',   'dung.staff@example.com',    '0902000001', 'Ha Noi',  'MALE',   'STAFF',    1, '2026-03-01 09:00:00'),
('11111111-1111-1111-1111-111111111202', 'stf_ha',   '$2a$12$JRXCc5f2OdjTBCwLotOgle1MLIzpuN7z/mlZoT5EO1LTdrmYdtDuu', 'Vo Thu Ha',        'ha.staff@example.com',      '0902000002', 'Can Tho', 'FEMALE', 'STAFF',    1, '2026-03-01 09:10:00'),
('11111111-1111-1111-1111-111111111301', 'own_long', '$2a$12$JRXCc5f2OdjTBCwLotOgle1MLIzpuN7z/mlZoT5EO1LTdrmYdtDuu', 'Bui Hoang Long',   'long.owner@example.com',    '0903000001', 'Ha Noi',  'MALE',   'OWNER',    1, '2026-03-01 10:00:00');

INSERT INTO customers (id) VALUES
('11111111-1111-1111-1111-111111111101'),
('11111111-1111-1111-1111-111111111102');

INSERT INTO staffs (id) VALUES
('11111111-1111-1111-1111-111111111201'),
('11111111-1111-1111-1111-111111111202');

INSERT INTO owners (id) VALUES
('11111111-1111-1111-1111-111111111301');

-- 2) Master data
INSERT INTO categories (id, name, description) VALUES
('22222222-2222-2222-2222-222222222001', 'Dong ho Co',     'Dong ho co truyen thong'),
('22222222-2222-2222-2222-222222222002', 'Dong ho Quartz', 'Dong ho pin do chinh xac cao'),
('22222222-2222-2222-2222-222222222003', 'Dong ho Smart',  'Dong ho thong minh');

INSERT INTO suppliers (id, name, contract_info, address) VALUES
('33333333-3333-3333-3333-333333333001', 'Nha cung cap A', 'Hop dong 2026-01', 'Ha Noi'),
('33333333-3333-3333-3333-333333333002', 'Nha cung cap B', 'Hop dong 2026-02', 'TP HCM');

INSERT INTO vouchers (id, code, discount_percent, usage_count, valid_from, valid_to, created_at, quantity, status) VALUES
('66666666-6666-6666-6666-666666666001', 'VC10OFF', 10, 0, '2026-03-01 00:00:00', '2026-12-31 23:59:59', '2026-03-01 00:00:00', 100, 'ACTIVE'),
('66666666-6666-6666-6666-666666666002', 'VC20OFF', 20, 3, '2026-03-01 00:00:00', '2026-06-30 23:59:59', '2026-03-01 00:00:00',  50, 'ACTIVE'),
('66666666-6666-6666-6666-666666666003', 'VC5OFF',   5, 1, '2026-01-01 00:00:00', '2026-03-31 23:59:59', '2026-01-01 00:00:00',   1, 'USED_UP');

-- 3) Products + images
INSERT INTO products (
    id, brand, name, description, price, stock_quantity,
    movement_type, glass_material, water_resistance, face_size,
    wire_material, wire_color, case_color, face_color,
    status, updated_at, category_id
) VALUES
('44444444-4444-4444-4444-444444444001', 'Seiko', 'Seiko 5 Sports',
 'Dong ho co tu dong, thiet ke the thao', 5200000, 15,
 'Automatic', 'Hardlex', '100m', '42mm',
 'Thep khong gi', 'Bac', 'Bac', 'Den',
 'ACTIVE', '2026-03-05 09:00:00', '22222222-2222-2222-2222-222222222001'),

('44444444-4444-4444-4444-444444444002', 'Casio', 'Casio MTP-VT01',
 'Dong ho quartz co dien, mat trang', 1500000, 40,
 'Quartz', 'Mineral', '30m', '40mm',
 'Da', 'Nau', 'Bac', 'Trang',
 'ACTIVE', '2026-03-05 09:10:00', '22222222-2222-2222-2222-222222222002'),

('44444444-4444-4444-4444-444444444003', 'Apple', 'Apple Watch SE',
 'Dong ho thong minh theo doi suc khoe', 6900000, 20,
 'Digital', 'Ion-X', '50m', '44mm',
 'Silicone', 'Den', 'Den', 'Den',
 'ACTIVE', '2026-03-05 09:20:00', '22222222-2222-2222-2222-222222222003');

INSERT INTO product_images (id, image_url, public_id, alt_text, is_thumbnail, product_id) VALUES
('45454545-4545-4545-4545-454545454001', 'https://example.com/p1-main.jpg', 'products/p1-main', 'Seiko 5 Sports', 1, '44444444-4444-4444-4444-444444444001'),
('45454545-4545-4545-4545-454545454002', 'https://example.com/p2-main.jpg', 'products/p2-main', 'Casio MTP-VT01', 1, '44444444-4444-4444-4444-444444444002'),
('45454545-4545-4545-4545-454545454003', 'https://example.com/p3-main.jpg', 'products/p3-main', 'Apple Watch SE', 1, '44444444-4444-4444-4444-444444444003');

-- 4) Cart flow
INSERT INTO carts (id, total_amount, customer_id) VALUES
('77777777-7777-7777-7777-777777777001', 5200000, '11111111-1111-1111-1111-111111111101'),
('77777777-7777-7777-7777-777777777002', 3000000, '11111111-1111-1111-1111-111111111102');

INSERT INTO cart_items (id, quantity, sub_total, cart_id, product_id) VALUES
('88888888-8888-8888-8888-888888888001', 1, 5200000, '77777777-7777-7777-7777-777777777001', '44444444-4444-4444-4444-444444444001'),
('88888888-8888-8888-8888-888888888002', 2, 3000000, '77777777-7777-7777-7777-777777777002', '44444444-4444-4444-4444-444444444002');

-- 5) Order flow
INSERT INTO orders (id, order_date, total_amount, note, shipping_address, status, customer_id, voucher_id) VALUES
('99999999-9999-9999-9999-999999999001', '2026-03-08 10:00:00', 4680000, 'Giao gio hanh chinh', 'Ha Noi',  'CONFIRMED', '11111111-1111-1111-1111-111111111101', '66666666-6666-6666-6666-666666666001'),
('99999999-9999-9999-9999-999999999002', '2026-03-09 11:00:00', 3000000, 'Goi hang ky',         'Da Nang', 'PENDING',   '11111111-1111-1111-1111-111111111102', NULL);

INSERT INTO order_items (id, quantity, sub_total, order_id, product_id) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa001', 1, 5200000, '99999999-9999-9999-9999-999999999001', '44444444-4444-4444-4444-444444444001'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa002', 2, 3000000, '99999999-9999-9999-9999-999999999002', '44444444-4444-4444-4444-444444444002');

INSERT INTO order_status_history (id, order_id, status, note, changed_at, changed_by) VALUES
('abababab-abab-abab-abab-ababababab01', '99999999-9999-9999-9999-999999999001', 'CONFIRMED', 'Don da xac nhan', '2026-03-08 10:01:00', 'stf_dung'),
('abababab-abab-abab-abab-ababababab02', '99999999-9999-9999-9999-999999999002', 'PENDING',   'Don moi tao',     '2026-03-09 11:00:00', 'cus_binh');

INSERT INTO payments (id, payment_date, amount, method, status, is_paid, order_id) VALUES
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001', '2026-03-08 10:05:00', 4680000, 'BANK_TRANSFER', 'COMPLETED', 1, '99999999-9999-9999-9999-999999999001'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002', '2026-03-09 11:10:00', 3000000, 'COD',           'RECEIVED',  0, '99999999-9999-9999-9999-999999999002');

INSERT INTO shippings (id, tracking_number, tracking_date, carrier_name, carrier_phone, estimated_delivery, order_id) VALUES
('cccccccc-cccc-cccc-cccc-ccccccccc001', 'TRACK-0001', '2026-03-08 14:00:00', 'GHN',  '0912345678', '2026-03-10 18:00:00', '99999999-9999-9999-9999-999999999001'),
('cccccccc-cccc-cccc-cccc-ccccccccc002', 'TRACK-0002', '2026-03-09 15:00:00', 'GHTK', '0987654321', '2026-03-11 18:00:00', '99999999-9999-9999-9999-999999999002');

-- 6) Warranty + inventory
INSERT INTO warranties (
    id, customer_phone, customer_name, issue_description,
    received_date, expected_return_date, status,
    technician_note, reject_reason, quantity, product_id
) VALUES
('dddddddd-dddd-dddd-dddd-ddddddddd001', '0901000001', 'Nguyen Van An', 'Dong ho cham 5 phut/ngay',
 '2026-03-12 09:00:00', '2026-03-20 17:00:00', 'RECEIVED',
 'Kiem tra bo may', NULL, 1, '44444444-4444-4444-4444-444444444001'),

('dddddddd-dddd-dddd-dddd-ddddddddd002', '0901000002', 'Tran Thi Binh', 'Mat kinh bi tray nhe',
 '2026-03-13 09:30:00', '2026-03-22 17:00:00', 'PROCESSING',
 'Danh bong mat kinh', NULL, 1, '44444444-4444-4444-4444-444444444002');

INSERT INTO inventories (
    id, issue_description, received_date, expected_return_date,
    status, tech_notes, quantity, product_id, customer_id, owner_id
) VALUES
('eeeeeeee-eeee-eeee-eeee-eeeeeeeee001', 'Bao tri dinh ky', '2026-03-15 08:00:00', '2026-03-18 17:00:00',
 'PENDING', 'Cho tiep nhan', 1, '44444444-4444-4444-4444-444444444001', '11111111-1111-1111-1111-111111111101', '11111111-1111-1111-1111-111111111301'),

('eeeeeeee-eeee-eeee-eeee-eeeeeeeee002', 'Loi kim giay', '2026-03-15 09:00:00', '2026-03-19 17:00:00',
 'IN_PROGRESS', 'Dang thay linh kien', 1, '44444444-4444-4444-4444-444444444002', '11111111-1111-1111-1111-111111111102', '11111111-1111-1111-1111-111111111301');

-- 7) Import flow
INSERT INTO import_receipts (id, import_date, note, supplier_id, owner_id) VALUES
('ffffffff-ffff-ffff-ffff-fffffffff001', '2026-03-06 08:00:00', 'Nhap lo thang 3 - dot 1', '33333333-3333-3333-3333-333333333001', '11111111-1111-1111-1111-111111111301'),
('ffffffff-ffff-ffff-ffff-fffffffff002', '2026-03-07 08:30:00', 'Nhap lo thang 3 - dot 2', '33333333-3333-3333-3333-333333333002', '11111111-1111-1111-1111-111111111301');

INSERT INTO import_details (id, quantity, import_price, import_receipt_id, product_id) VALUES
('12121212-1212-1212-1212-121212121001', 10, 4200000, 'ffffffff-ffff-ffff-ffff-fffffffff001', '44444444-4444-4444-4444-444444444001'),
('12121212-1212-1212-1212-121212121002', 20, 1100000, 'ffffffff-ffff-ffff-ffff-fffffffff002', '44444444-4444-4444-4444-444444444002');

-- 8) Feedback / communication
INSERT INTO reviews (id, rating, comment, created_at, customer_id, product_id) VALUES
('13131313-1313-1313-1313-131313131001', 5, 'San pham dep, dung nhu mo ta', '2026-03-11 10:00:00', '11111111-1111-1111-1111-111111111101', '44444444-4444-4444-4444-444444444001'),
('13131313-1313-1313-1313-131313131002', 4, 'Gia tot, giao nhanh',          '2026-03-11 10:10:00', '11111111-1111-1111-1111-111111111102', '44444444-4444-4444-4444-444444444002');

INSERT INTO discussions (id, start_date, end_date, score_sent, content_log, is_ai_handled, customer_id) VALUES
('14141414-1414-1414-1414-141414141001', '2026-03-12 08:00:00', '2026-03-12 08:30:00', 9, 'Hoi dap bao hanh Seiko', 0, '11111111-1111-1111-1111-111111111101'),
('14141414-1414-1414-1414-141414141002', '2026-03-12 09:00:00', '2026-03-12 09:20:00', 8, 'Tu van thay day dong ho', 1, '11111111-1111-1111-1111-111111111102');

INSERT INTO review_reports (id, start_date, end_date, total_sales, score_sales, created_at, owner_id) VALUES
('15151515-1515-1515-1515-151515151001', '2026-03-01 00:00:00', '2026-03-10 23:59:59', 120000000, 8.5, '2026-03-11 09:00:00', '11111111-1111-1111-1111-111111111301');

INSERT INTO notifications (id, title, content, direct_url, is_read, time_created, expiry, sender_id, receiver_id) VALUES
('16161616-1616-1616-1616-161616161001', 'Don hang da xac nhan', 'Don #001 da duoc xac nhan', '/orders/99999999-9999-9999-9999-999999999001', 0, '2026-03-08 10:06:00', '2026-04-08 10:06:00', '11111111-1111-1111-1111-111111111201', '11111111-1111-1111-1111-111111111101'),
('16161616-1616-1616-1616-161616161002', 'Khuyen mai thang 3',   'Ma VC20OFF dang co hieu luc', '/vouchers', 0, '2026-03-09 09:00:00', '2026-03-31 23:59:59', '11111111-1111-1111-1111-111111111202', '11111111-1111-1111-1111-111111111102');

INSERT INTO mails (id, def_date, subject, body, recipient, is_sent, customer_id) VALUES
('17171717-1717-1717-1717-171717171001', '2026-03-08 10:07:00', 'Xac nhan don hang #001', 'Don hang cua ban da duoc tiep nhan.', 'an.customer@example.com', 1, '11111111-1111-1111-1111-111111111101'),
('17171717-1717-1717-1717-171717171002', '2026-03-09 09:05:00', 'Khuyen mai dac biet',      'Su dung ma VC20OFF truoc ngay 30/06.', 'binh.customer@example.com', 1, '11111111-1111-1111-1111-111111111102');

-- 9) Many-to-many Product <-> Category
CREATE TABLE IF NOT EXISTS product_categories (
    product_id VARCHAR(36) NOT NULL,
    category_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (product_id, category_id),
    CONSTRAINT fk_product_categories_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_categories_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

-- Keep existing primary category links + add extra links
INSERT INTO product_categories (product_id, category_id)
SELECT p.id, p.category_id
FROM products p
WHERE p.category_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM product_categories pc WHERE pc.product_id = p.id AND pc.category_id = p.category_id
  );

INSERT INTO product_categories (product_id, category_id) VALUES
('44444444-4444-4444-4444-444444444001', '22222222-2222-2222-2222-222222222002'),
('44444444-4444-4444-4444-444444444003', '22222222-2222-2222-2222-222222222001')
ON DUPLICATE KEY UPDATE product_id = VALUES(product_id);

COMMIT;
