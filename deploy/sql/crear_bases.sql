-- Bases y usuarios de MariaDB en la EC2 (Guía 18). Se ejecuta UNA vez con: sudo mariadb < crear_bases.sql
-- Antes de ejecutarlo, reemplazar CAMBIAR_CLAVE_AUTH_APP y CAMBIAR_CLAVE_TIENDA_APP por las
-- mismas claves que van en auth.env y tienda.env. Después borrar la copia con las claves reales.
-- Cada backend tiene su propia base y su propio usuario: Auth no puede tocar las tablas de la
-- tienda ni al revés. @'localhost' impide conectarse a MariaDB desde fuera de la EC2.

CREATE DATABASE IF NOT EXISTS ferreteria_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ferreteria_los_maestros CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'auth_app'@'localhost' IDENTIFIED BY 'CAMBIAR_CLAVE_AUTH_APP';
CREATE USER IF NOT EXISTS 'tienda_app'@'localhost' IDENTIFIED BY 'CAMBIAR_CLAVE_TIENDA_APP';

GRANT ALL PRIVILEGES ON ferreteria_auth.* TO 'auth_app'@'localhost';
GRANT ALL PRIVILEGES ON ferreteria_los_maestros.* TO 'tienda_app'@'localhost';
FLUSH PRIVILEGES;

-- Comprobación: deben aparecer las dos bases y los dos usuarios
SHOW DATABASES LIKE 'ferreteria%';
SELECT User, Host FROM mysql.user WHERE User IN ('auth_app', 'tienda_app');
