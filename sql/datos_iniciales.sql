-- Datos iniciales de Ferretería Los Maestros.
-- Se ejecuta a mano en phpMyAdmin DESPUÉS del primer arranque del backend,
-- cuando Spring ya creó las tablas (ver README.md).
-- INSERT IGNORE con ids fijos: si se ejecuta de nuevo, omite los ids que ya existen
-- y no modifica sus datos (por ejemplo, no pisa el stock actual).
-- Origen: src/data/productos.js del proyecto React.
-- Los usuarios ya no van aquí (Guía 17): viven en el backend Auth (base ferreteria_auth),
-- que crea los usuarios de prueba con clave BCrypt al arrancar.

SET NAMES utf8mb4;
USE ferreteria_los_maestros;

INSERT IGNORE INTO categoria (id, nombre) VALUES
(1, 'Construcción'),
(2, 'Electricidad'),
(3, 'Ferretería general'),
(4, 'Gasfitería'),
(5, 'Herramientas'),
(6, 'Herramientas eléctricas'),
(7, 'Pinturas'),
(8, 'Seguridad');

INSERT IGNORE INTO producto
(id, codigo, nombre, marca, unidad, precio, precio_oferta, stock, stock_minimo, imagen, descripcion, categoria_id) VALUES
(1, 'MC001', 'Cemento gris 25 kg', 'Polpaico', 'saco', 5990, NULL, 80, 20, '/assets/img/productos/cemento-25kg.jpg', 'Cemento gris para trabajos generales de construcción, reparación y obra menor.', 1),
(2, 'PT001', 'Pintura látex interior 1 galón', 'Sipa', 'galón', 9990, 8990, 40, 10, '/assets/img/productos/pintura-latex.jpg', 'Pintura látex de uso interior para muros y cielos, pensada para terminaciones de uso general.', 7),
(3, 'HM001', 'Martillo carpintero 500 g', 'Stanley', 'unidad', 7990, NULL, 20, 5, '/assets/img/productos/martillo-carpintero.jpg', 'Martillo carpintero para trabajos de montaje, reparación y uso frecuente en obra o taller.', 5),
(4, 'HM010', 'Cinta métrica 8 m', 'Stanley', 'unidad', 6490, 5490, 20, 5, '/assets/img/productos/cinta-metrica.jpg', 'Cinta métrica de 8 metros para mediciones en construcción, instalaciones y trabajos generales.', 5),
(5, 'HE001', 'Taladro percutor 650 W', 'Makita', 'unidad', 79990, NULL, 2, 3, '/assets/img/productos/taladro-percutor.jpg', 'Taladro percutor para perforaciones de uso general en trabajos domésticos y profesionales.', 6),
(6, 'GS001', 'Cañería PVC 1/2 pulgada x 6 m', 'Tigre', 'tira', 5490, NULL, 30, 10, '/assets/img/productos/tuberia-pvc.jpg', 'Cañería de PVC para instalaciones y reparaciones de redes de agua de uso residencial.', 4),
(7, 'GS007', 'Llave de paso esfera 1/2 pulgada', 'Emmeti', 'unidad', 3490, NULL, 30, 8, '/assets/img/productos/llave-paso.jpg', 'Llave de paso tipo esfera para controlar el flujo de agua en instalaciones de gasfitería.', 4),
(8, 'EL002', 'Cable unipolar 2,5 mm²', 'Condulac', 'metro', 790, NULL, 100, 25, '/assets/img/productos/cable-unipolar.jpg', 'Cable unipolar para instalaciones eléctricas de uso general, vendido por metro.', 2),
(9, 'TR001', 'Tornillo autoperforante 8x1 caja 100', 'Hilti', 'caja', 2990, NULL, 40, 15, '/assets/img/productos/tornillo-autoperforante.jpg', 'Caja de tornillos autoperforantes para fijaciones rápidas en distintos trabajos de montaje.', 3),
(10, 'SE001', 'Casco de seguridad blanco', '3M', 'unidad', 6990, 5990, 4, 5, '/assets/img/productos/casco-seguridad.jpg', 'Casco de seguridad para protección de cabeza en faenas, talleres y trabajos de construcción.', 8);
