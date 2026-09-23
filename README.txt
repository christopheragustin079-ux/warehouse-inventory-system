WAREHOUSE INVENTORY SYSTEM

UPDATED FLOW:
1. First-time user opens the website.
2. Click CREATE ACCOUNT.
3. Register a username and password.
4. After registration, go back to LOGIN.
5. Login using the registered account.
6. The Products page starts EMPTY.
7. User adds their own products.
8. Click a product to add stock using Box/Batch ID and Quantity.
9. PUSH adds stock to the top.
10. POP removes stock from the top (LIFO).
11. LOGOUT returns to the login page.

IMPORTANT:
- There is NO default admin account.
- There are NO sample products.
- There is NO sample stock.
- Registered users are saved in MySQL, so they can log in again later.
- The password is not saved in the browser.
- The username may be remembered on the login screen for convenience.
- The website is responsive for PC, laptop, tablet, and mobile phone.

SETUP:
1. Install Java JDK.
2. Install XAMPP and start MySQL.
3. Open phpMyAdmin.
4. Import warehouse_inventory.sql.
   If you already imported the OLD SQL, remove the old sample products/users/stock
   or recreate the database before importing this updated SQL.
5. Put your MySQL Connector/J .jar in this folder and name it:
   mysql-connector-j-26.7.0.jar
6. Open this folder in VS Code.
7. Double-click run.bat.
8. Open http://localhost:8080

DATABASE:
Database: warehouse_inventory_db
MySQL user: root
MySQL password: blank
