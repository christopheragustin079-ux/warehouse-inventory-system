@echo off
title Warehouse Inventory System
echo Starting Warehouse Inventory System...
echo.
javac -cp ".;mysql-connector-j-26.7.0.jar" GroceryServer.java
if errorlevel 1 (
  echo.
  echo ERROR: Java compilation failed.
  pause
  exit /b
)
java -cp ".;mysql-connector-j-26.7.0.jar" GroceryServer
pause
