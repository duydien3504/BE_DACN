-- Fix payment table to allow null order_id for shop registration payments
ALTER TABLE payments MODIFY COLUMN order_id BIGINT NULL;
