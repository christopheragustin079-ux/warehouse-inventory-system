CREATE DATABASE IF NOT EXISTS warehouse_inventory_db;
USE warehouse_inventory_db;

CREATE TABLE IF NOT EXISTS users(
 id INT AUTO_INCREMENT PRIMARY KEY,
 username VARCHAR(50) UNIQUE NOT NULL,
 password VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS products(
 id INT AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS stock(
 id INT AUTO_INCREMENT PRIMARY KEY,
 product_id INT NOT NULL,
 box_code VARCHAR(100) NOT NULL,
 quantity INT NOT NULL,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(product_id) REFERENCES products(id)
);

-- No default user, products, or stock are inserted.
-- The first user must register through the website.
-- Products and stock are added by the user after logging in.
