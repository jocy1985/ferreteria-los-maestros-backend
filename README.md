# Ferretería Los Maestros — Backend

API REST de la tienda online, hecha con Spring Boot y MySQL (DSY1104, Evaluación Parcial 3).
Las rutas están versionadas bajo `/api/v1`.

Desde la Guía 17 la solución tiene **dos backend**:

| | Auth (`ferreteria-los-maestros-auth`) | Este backend |
|---|---|---|
| Puerto | 8081 | 8080 |
| Base | `ferreteria_auth` | `ferreteria_los_maestros` |
| Responsabilidad | Usuarios, login, registro, emisión y renovación del JWT | Productos, categorías, boletas y detalles |
| JWT | Lo firma | Lo verifica (misma clave `JWT_SECRET`) |

Este backend no tiene tabla ni clases de usuario y nunca consulta la base de Auth: lee el correo,
el rol y el id directamente del token, después de verificar su firma, emisor y expiración.

## Requisitos

- Java 17 o superior.
- XAMPP con **MySQL** encendido en el puerto 3306 (usuario `root` sin contraseña).
- Maven no hace falta instalarlo: se usa `mvnw.cmd`, incluido en el proyecto.
- Para obtener tokens: el backend Auth corriendo en el 8081 (ver su README).

### Usuarios de prueba (solo desarrollo)

Viven en Auth, que los crea al arrancar. Se usan para pedir el token en su Swagger.

| Rol | Correo | Clave |
|---|---|---|
| CLIENTE | `cliente@losmaestros.cl` | `cliente123` |
| VENDEDOR | `vendedor@losmaestros.cl` | `vendedor123` |
| ADMINISTRADOR | `admin@losmaestros.cl` | `admin123` |

## Puesta en marcha

El orden importa: las tablas las crea Spring, y el script solo inserta datos en ellas.

1. Encender MySQL desde el panel de XAMPP.
2. Arrancar el backend una primera vez. Esto crea la base `ferreteria_los_maestros` y sus tablas:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

3. Cargar los datos iniciales en phpMyAdmin:
   1. Abrir <http://localhost/phpmyadmin>.
   2. Seleccionar la base `ferreteria_los_maestros` en el panel izquierdo.
   3. Ir a la pestaña **Importar**.
   4. Elegir el archivo `sql/datos_iniciales.sql`, con conjunto de caracteres `utf-8`.
   5. Presionar **Importar** (o **Continuar**, según la versión).
4. Comprobar que quedaron 8 categorías y 10 productos abriendo
   <http://localhost:8080/api/v1/productos> (es público, no pide token).

No hace falta reiniciar el backend después de importar.

### Si la base viene de antes de la Guía 17

Antes, `boleta.usuario_id` era una llave foránea hacia la tabla `usuario` de esta misma base.
`ddl-auto=update` no borra esa llave, y con ella un cliente nuevo (id 4 o más, creado en Auth) no
podría comprar. Hay dos formas de resolverlo:

- **Conservar las boletas:** en phpMyAdmin, pestaña SQL de `ferreteria_los_maestros`, buscar el
  nombre de la llave y borrarla:

  ```sql
  SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
  WHERE TABLE_SCHEMA = 'ferreteria_los_maestros' AND TABLE_NAME = 'boleta'
    AND REFERENCED_TABLE_NAME = 'usuario';

  ALTER TABLE boleta DROP FOREIGN KEY nombre_que_entrego_la_consulta;
  ```

  Después borrar la tabla `usuario` antigua: ya no se usa y guarda las claves en texto plano
  (`DROP TABLE usuario;`).
- **Partir de cero:** borrar la base `ferreteria_los_maestros`, arrancar el backend y volver a
  importar `sql/datos_iniciales.sql`.

### Si la base viene de antes de la Guía 18 (UNIQUE)

Desde la Guía 18 el código de producto y el nombre de categoría son `UNIQUE` también en la base.
En una base nueva Spring crea la restricción, pero `ddl-auto=update` **no** la agrega a una tabla
que ya existe (comprobado). En phpMyAdmin, pestaña SQL de `ferreteria_los_maestros`:

```sql
ALTER TABLE producto ADD CONSTRAINT uk_producto_codigo UNIQUE (codigo);
ALTER TABLE categoria ADD CONSTRAINT uk_categoria_nombre UNIQUE (nombre);
```

Si responde "Duplicate entry", primero hay que corregir el código o nombre repetido.

### Sobre el script de datos iniciales

- Trae las categorías y productos del frontend (`src/data/productos.js`). Los usuarios ya no están
  aquí: los crea Auth al arrancar.
- Usa `INSERT IGNORE` con ids fijos, así que se puede ejecutar más de una vez: los registros que
  ya existen no se modifican y el stock actual se conserva.

## Clave del JWT (`JWT_SECRET`)

`application.properties` trae un valor por defecto **solo para desarrollo** (el mismo que Auth),
para que el proyecto arranque y `mvnw test` funcione sin configurar nada. Como el repositorio es
público, ese valor no debe usarse fuera del computador de desarrollo.

En producción la clave se entrega por variable de entorno, con el mismo valor en los dos backend:

```powershell
$env:JWT_SECRET = $secretoJwt   # el mismo valor que se le dio a Auth
.\mvnw.cmd spring-boot:run
```

El README de Auth explica cómo generar `$secretoJwt`. Si los valores no coinciden, este backend
responde 401 a todos los tokens.

## Seguridad por rol

| Ruta | Sin token | CLIENTE | VENDEDOR | ADMINISTRADOR |
|---|---|---|---|---|
| `GET /api/v1/productos/**`, `GET /api/v1/categorias/**` | 200 | 200 | 200 | 200 |
| `POST /api/v1/boletas` | 201 (invitado) | 201 | 403 (el vendedor no compra) | 201 |
| `GET /api/v1/boletas/mias` (compras propias, por el id del token) | 401 | 200 | 403 | 403 |
| `GET /api/v1/boletas/**`, `GET /api/v1/detalles-boleta/**` | 401 | 403 | 200 | 200 |
| POST, PUT y DELETE de productos y categorías; PUT y DELETE de boletas | 401 | 403 | 403 | 200/201/204 |

- **401 sin token:** `{ "error": "Debes iniciar sesión" }`.
- **401 con token alterado o expirado:** `{ "error": "Token inválido o expirado" }`. Pasa también en
  las rutas públicas: si la solicitud trae un Bearer, este debe ser válido. Así una compra con la
  sesión vencida no queda registrada como invitado.
- **403 rol insuficiente:** `{ "error": "No tienes permisos para esta operación" }`.
- **Quién compra:** el `POST` de boleta ya no recibe `usuarioId`. Si trae token, la boleta queda con
  el `id` del token; si no, `usuarioId` queda `null` (compra como invitado).
- **Respuesta del POST de boleta:** trae la boleta con sus líneas en `detalles` (código, nombre,
  cantidad, precio y subtotal de cada producto). Así el cliente o invitado ve su confirmación sin
  llamar a `GET /boletas/{id}/detalles`, que es solo para vendedor y administrador. En los `GET`,
  `detalles` va `null`.

## Probar en Swagger

1. En <http://localhost:8081/swagger-ui/index.html> (Auth) ejecutar `POST /api/v1/auth/login` y
   copiar el `token` sin comillas.
2. Abrir <http://localhost:8080/swagger-ui/index.html>, presionar **Authorize**, pegar solo el token
   y confirmar.
3. Probar la tabla de arriba con tokens de cliente, vendedor y administrador.

## Pruebas automáticas

```powershell
.\mvnw.cmd test
```

Necesitan **MySQL de XAMPP encendido**, porque levantan la aplicación completa. También
`.\mvnw.cmd clean package` las ejecuta: el JAR para producción solo se genera con MySQL encendido.
20 pruebas:

- `SeguridadTests` (7): restricciones por rol (401/403, `/mias`) con tokens firmados en la propia
  prueba; ninguna solicitud llega a modificar la base.
- `ProductoServiceTests` (6) y `BoletaServiceTests` (6), Guía 18: reglas de negocio contra una base
  aparte, `ferreteria_test` (URL con `createDatabaseIfNotExist=true` y `ddl-auto=create-drop`), así
  que nunca tocan `ferreteria_los_maestros`. Cubren código repetido (409, también en minúsculas),
  UNIQUE de la base, producto en boletas no se elimina, descuento de stock, total con precio de
  oferta, stock insuficiente sin guardar nada (`@Transactional`), despacho sin dirección (400),
  anulación que devuelve stock y boleta anulada que no se vuelve a anular ni modificar.
- `FerreteriaApplicationTests`: la aplicación arranca.

Los avisos `Error executing DDL ... doesn't exist` del registro son normales: `create-drop` intenta
borrar tablas que en la primera ejecución todavía no existen.

## Endpoints

| Recurso | Operaciones |
|---|---|
| `/api/v1/categorias` | GET, POST · `/{id}`: GET, PUT, DELETE |
| `/api/v1/productos` | GET, POST · `/{id}`: GET, PUT, DELETE |
| `/api/v1/boletas` | GET, POST · `/mias`: GET (solo CLIENTE) · `/{id}`: GET, PUT, DELETE · `/{id}/detalles`: GET |
| `/api/v1/detalles-boleta` | GET · `/{id}`: GET (solo lectura) |

Los usuarios (`/api/v1/usuarios`) están en Auth.

Para asociar un producto a su categoría, basta enviar el id:
`"categoria": { "id": 1 }`.

Ejemplo de `POST /api/v1/boletas` (con o sin token):

```json
{
  "nombreCliente": "Cliente Demo",
  "correoCliente": "cliente@losmaestros.cl",
  "telefonoCliente": "+56 9 5555 1111",
  "entrega": "retiro",
  "formaPago": "transferencia",
  "items": [
    { "productoId": 1, "cantidad": 2 },
    { "productoId": 2, "cantidad": 3 }
  ]
}
```

## Reglas de negocio

- El código de producto y el nombre de categoría no se pueden repetir: lo revisa el servicio (409)
  y, desde la Guía 18, también la base con `UNIQUE` (si dos solicitudes llegan a la vez, la base
  rechaza la segunda y también responde 409).
- El POST de boleta valida el stock, lo descuenta, crea los detalles y calcula el total usando
  `precioOferta` cuando existe. Si una línea falla, no se guarda nada.
- El PUT de boleta solo cambia datos de contacto, entrega y estado (`RECIBIDA`, `CONFIRMADA` o
  `ENTREGADA`).
- El DELETE de boleta no borra el registro: la deja en estado `ANULADA` y devuelve el stock. Una
  boleta anulada sigue apareciendo en los GET, pero no se puede modificar ni volver a anular.
- No se puede eliminar una categoría con productos ni un producto presente en boletas.

## Respuestas

| Código | Cuándo |
|---|---|
| 200 / 201 / 204 | GET y PUT / POST / DELETE correctos |
| 400 | Datos inválidos |
| 401 | Falta el token, o es inválido o expiró |
| 403 | El rol no autoriza la operación |
| 404 | El recurso no existe |
| 409 | Conflicto: dato repetido, stock insuficiente, boleta anulada o registro en uso |

Los errores siempre responden `{ "error": "mensaje" }`.

## Producción (Guía 18)

El despliegue en AWS EC2 (Nginx + dos servicios systemd + MariaDB) está en
[`deploy/DESPLIEGUE.md`](deploy/DESPLIEGUE.md), con los comandos exactos para los tres proyectos.

El perfil `prod` (`application-prod.properties`) se activa con `SPRING_PROFILES_ACTIVE=prod`.
Escucha solo en `127.0.0.1:8080` y todo sale de variables de entorno, **sin valores por defecto**
para los secretos (si falta una, la tienda no arranca):

| Variable | Qué es |
|---|---|
| `TIENDA_DB_URL`, `TIENDA_DB_USER`, `TIENDA_DB_PASSWORD` | Base `ferreteria_los_maestros` y su usuario propio `tienda_app` |
| `JWT_SECRET` | El mismo valor que Auth |
| `APP_CORS_ALLOWED_ORIGIN` | Origen del frontend, por ejemplo `http://IP_PUBLICA` (en desarrollo `http://localhost:5173`) |

| Ruta pública (Nginx) | Destino |
|---|---|
| `/api/auth-service/...` | Auth `127.0.0.1:8081/api/...` |
| `/api/tienda-service/...` | Tienda `127.0.0.1:8080/api/...` |
| `/swagger/auth/`, `/swagger/tienda/` | Swagger de cada backend (público para la presentación) |
| Lo demás | React (`/var/www/ferreteria`) |

Swagger se publica por Nginx (la Guía 18 usa túneles SSH) porque la presentación se hace desde un
computador de la sala sin la llave `.pem`. `server.forward-headers-strategy=framework` hace que
Swagger use el prefijo `/swagger/tienda` en "Try it out"; los endpoints siguen exigiendo JWT y rol.
El túnel queda documentado como alternativa.
