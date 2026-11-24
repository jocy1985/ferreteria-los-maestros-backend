# Despliegue en AWS EC2 — Ferretería Los Maestros

Guía paso a paso para publicar los tres proyectos en **una** instancia EC2 Ubuntu 24.04 (t3.micro).
Todos los comandos son de este proyecto: solo hay que
reemplazar los valores en MAYÚSCULAS (`IP_PUBLICA`, `RUTA\CLAVE.pem`, `CAMBIAR_...`).

> Los secretos (claves de base, `JWT_SECRET`, claves demo) **nunca** van a Git ni a capturas
> públicas. Las plantillas de `deploy/env/` no tienen secretos: los valores reales solo existen en
> `/etc/ferreteria/*.env` dentro de la EC2.

## 0. Cómo queda armado

| Componente | Dónde escucha | Acceso desde Internet |
|---|---|---|
| Nginx + React | `0.0.0.0:80` | Sí: es el único punto HTTP público |
| Auth (`ferreteria-auth.service`) | `127.0.0.1:8081` | Solo a través de Nginx |
| Tienda (`ferreteria-tienda.service`) | `127.0.0.1:8080` | Solo a través de Nginx |
| MariaDB | `127.0.0.1:3306` | No |
| SSH | `22` | Solo desde tu IP y desde EC2 Instance Connect |

| Ruta pública | Destino interno |
|---|---|
| `/api/auth-service/...` | Auth `127.0.0.1:8081/api/...` (ej. `/api/auth-service/v1/auth/login` → `/api/v1/auth/login`) |
| `/api/tienda-service/...` | Tienda `127.0.0.1:8080/api/...` (ej. `/api/tienda-service/v1/productos` → `/api/v1/productos`) |
| `/swagger/auth/` | Swagger de Auth (público para la presentación) |
| `/swagger/tienda/` | Swagger de la tienda (público para la presentación) |
| Todo lo demás | Archivos de React en `/var/www/ferreteria` |

| En la EC2 | Valor |
|---|---|
| Bases | `ferreteria_auth` (usuario `auth_app`) y `ferreteria_los_maestros` (usuario `tienda_app`) |
| JAR | `/opt/ferreteria-auth/app.jar` y `/opt/ferreteria-tienda/app.jar` |
| Variables | `/etc/ferreteria/auth.env` y `/etc/ferreteria/tienda.env` (root, `chmod 600`) |
| Servicios | `ferreteria-auth` y `ferreteria-tienda` |
| Nginx | `/etc/nginx/sites-available/ferreteria` |

Archivos de esta carpeta:

```text
deploy/
├── DESPLIEGUE.md                 esta guía
├── nginx/ferreteria.conf         sitio de Nginx
├── systemd/ferreteria-auth.service
├── systemd/ferreteria-tienda.service
├── env/auth.env.example          plantillas SIN secretos
├── env/tienda.env.example
└── sql/crear_bases.sql           bases y usuarios de MariaDB
```

---

## 1. AWS: instancia, grupo de seguridad e IP elástica

**1.1 Instancia.** Ubuntu Server 24.04 LTS, tipo `t3.micro`, con el par de llaves del equipo
(`.pem`). Disco de al menos 12 GB (el swap usa 2 GB).

**1.2 Grupo de seguridad (reglas de entrada).** Solo estas tres:

| Tipo | Puerto | Origen | Para qué |
|---|---|---|---|
| SSH | 22 | **Mi IP** | Conectarse con la llave `.pem` |
| SSH | 22 | Lista de prefijos `com.amazonaws.REGION.ec2-instance-connect` (ej. `com.amazonaws.us-east-1.ec2-instance-connect`) | Acceso de emergencia sin `.pem` (sección 17) |
| HTTP | 80 | `0.0.0.0/0` (y `::/0` si hay IPv6) | La aplicación |

**No abrir** 8080, 8081 ni 3306: Nginx es el único punto público.

**1.3 IP elástica (recomendada).** Sin ella, la IP pública cambia cada vez que la instancia se
detiene y se vuelve a encender, y la URL de la presentación dejaría de servir.
EC2 → **Direcciones IP elásticas** → **Asignar** → **Acciones → Asociar** a la instancia.
Desde ahora `IP_PUBLICA` es esa IP elástica.

> Costo: AWS cobra por hora toda IPv4 pública (también la elástica, y también mientras la
> instancia está detenida si la IP queda asignada). Revisen el saldo de la cuenta y liberen la IP
> después de la evaluación. Si no la usan, vean la sección 16 cada vez que cambie la IP.

---

## 2. Llave `.pem` y primera conexión (PowerShell local)

En la carpeta de la llave:

```powershell
icacls .\CLAVE.pem /inheritance:r
icacls .\CLAVE.pem /grant:r "$($env:USERNAME):(R)"
icacls .\CLAVE.pem /remove:g "Users" "Authenticated Users" "Everyone"
```

Para no repetir la ruta y la IP, en cada PowerShell nuevo:

```powershell
$PEM = "RUTA\CLAVE.pem"
$IP = "IP_PUBLICA"
ssh -i $PEM ubuntu@$IP
```

Resultado esperado: se abre la sesión `ubuntu@ip-...` sin advertencias de permisos.

---

## 3. Compilar y copiar los tres proyectos (PowerShell local)

**Antes de compilar:**

1. **Encender MySQL en XAMPP.** `clean package` ejecuta las pruebas, y estas levantan las
   aplicaciones contra MySQL local: `ferreteria_test` (pruebas de servicios, se crea sola con
   `createDatabaseIfNotExist=true`), `ferreteria_los_maestros` y `ferreteria_auth`. Con MySQL
   apagado la compilación falla.
2. **Detener los backend locales** (8080 y 8081, VS Code o `spring-boot:run`): `clean` borra
   `target/`, y un proceso que lo está usando puede bloquear archivos.

Desde la carpeta que contiene los tres proyectos:

```powershell
cd ferreteria-los-maestros-auth
.\mvnw.cmd clean package
cd ..\ferreteria-los-maestros-backend
.\mvnw.cmd clean package
cd ..\ferreteria-los-maestros-react-main
npm install
npm run build
cd ..
```

Resultado esperado: `BUILD SUCCESS` en ambos backend (Auth 12 pruebas, tienda 20) y `dist/` creado.
`npm run build` usa `.env.production`, que trae rutas relativas (`/api/auth-service/v1` y
`/api/tienda-service/v1`): por eso React no depende de la IP.

Copiar a `/tmp` de la EC2 (cada archivo con un nombre distinto para no pisar uno con otro):

```powershell
ssh -i $PEM ubuntu@$IP "rm -rf /tmp/ferreteria-web /tmp/ferreteria-deploy"
scp -i $PEM ferreteria-los-maestros-auth\target\ferreteria-auth-0.0.1-SNAPSHOT.jar "ubuntu@${IP}:/tmp/ferreteria-auth.jar"
scp -i $PEM ferreteria-los-maestros-backend\target\ferreteria-0.0.1-SNAPSHOT.jar "ubuntu@${IP}:/tmp/ferreteria-tienda.jar"
scp -i $PEM -r ferreteria-los-maestros-backend\deploy "ubuntu@${IP}:/tmp/ferreteria-deploy"
scp -i $PEM ferreteria-los-maestros-backend\sql\datos_iniciales.sql "ubuntu@${IP}:/tmp/ferreteria-deploy/"
scp -i $PEM -r ferreteria-los-maestros-react-main\dist "ubuntu@${IP}:/tmp/ferreteria-web"
```

> En PowerShell se escribe `${IP}:` (con llaves): `$IP:` lo interpreta como otra cosa.

---

## 4. Preparar la EC2 (terminal SSH)

Desde aquí, todo se ejecuta **dentro de la EC2**.

**4.1 Swap de 2 GB.** Una t3.micro tiene 1 GB de RAM y corre dos JVM, MariaDB y Nginx: el swap
evita que Linux mate un proceso en un pico de memoria (por ejemplo, al arrancar los dos backend).

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h
```

Resultado esperado: la fila `Swap:` muestra `2.0Gi`. La línea en `/etc/fstab` lo reactiva al
reiniciar la instancia.

**4.2 Paquetes y zona horaria.** La zona horaria de Chile hace que la fecha de las boletas
(`LocalDateTime.now()`) salga en hora chilena y no en UTC.

```bash
sudo apt update
sudo apt install -y mariadb-server openjdk-21-jre-headless nginx curl
sudo systemctl enable --now mariadb nginx
sudo timedatectl set-timezone America/Santiago
java -version
timedatectl | grep "Time zone"
```

Los JAR se compilan para Java 17 y corren sin cambios en Java 21.

**4.3 Saltos de línea de Windows.** Los archivos se editaron en Windows; esto les quita el `\r`
para que systemd y Nginx los lean bien:

```bash
find /tmp/ferreteria-deploy -type f \( -name "*.service" -o -name "*.conf" -o -name "*.example" -o -name "*.sql" \) -exec sed -i 's/\r$//' {} \;
ls -R /tmp/ferreteria-deploy
```

---

## 5. Secretos, bases y usuarios de MariaDB (terminal SSH)

**5.1 Generar los valores** (anotarlos en un lugar seguro del equipo, no en Git ni en el chat):

```bash
openssl rand -base64 32   # JWT_SECRET (el mismo para los dos backend)
openssl rand -base64 18   # clave de auth_app
openssl rand -base64 18   # clave de tienda_app
```

Las claves de los usuarios demo (`DEMO_CLAVE_*`) las elige el equipo: se escriben en el login
durante la presentación, así que conviene que sean fáciles de escribir, pero **distintas** de
`cliente123` / `vendedor123` / `admin123`, que están publicadas en el README.

**5.2 Crear las bases y los usuarios:**

```bash
nano /tmp/ferreteria-deploy/sql/crear_bases.sql   # reemplazar CAMBIAR_CLAVE_AUTH_APP y CAMBIAR_CLAVE_TIENDA_APP
sudo mariadb < /tmp/ferreteria-deploy/sql/crear_bases.sql
rm /tmp/ferreteria-deploy/sql/crear_bases.sql     # la copia tenía las claves reales
```

Resultado esperado: aparecen `ferreteria_auth`, `ferreteria_los_maestros`, `auth_app@localhost` y
`tienda_app@localhost`. Las tablas las crea Spring en el primer arranque (`ddl-auto=update`),
incluidos los UNIQUE de código de producto y nombre de categoría.

---

## 6. Archivos de entorno protegidos (terminal SSH)

```bash
sudo mkdir -p /etc/ferreteria
sudo cp /tmp/ferreteria-deploy/env/auth.env.example /etc/ferreteria/auth.env
sudo cp /tmp/ferreteria-deploy/env/tienda.env.example /etc/ferreteria/tienda.env
sudo nano /etc/ferreteria/auth.env     # reemplazar cada CAMBIAR_... e IP_PUBLICA
sudo nano /etc/ferreteria/tienda.env
sudo chown root:root /etc/ferreteria/*.env
sudo chmod 600 /etc/ferreteria/*.env
sudo ls -l /etc/ferreteria
```

Comprobaciones sin mostrar los secretos en pantalla:

```bash
sudo grep -c -e '=CAMBIAR' -e '=http://IP_PUBLICA' /etc/ferreteria/*.env   # debe dar 0 en ambos
sudo grep -h '^JWT_SECRET=' /etc/ferreteria/*.env | sort -u | wc -l   # debe dar 1 (mismo secreto)
sudo grep -h '^APP_CORS_ALLOWED_ORIGIN=' /etc/ferreteria/*.env   # http://IP_PUBLICA en ambos
```

La primera busca `=CAMBIAR` (con el signo igual) porque los comentarios de las plantillas también
dicen "CAMBIAR_...": con `grep -c CAMBIAR` nunca daría 0 aunque todos los valores estén listos.

`APP_CORS_ALLOWED_ORIGIN` va con `http://`, sin barra final y exactamente igual a la dirección
que se escribe en el navegador.

### Alternativa sin editor: secretos en variables y `sed` (la usada por el equipo)

Reemplaza 5.1, 5.2 y los `nano` de esta sección: los secretos nunca aparecen en pantalla, no quedan
en el historial de bash y `crear_bases.sql` no se modifica (el SQL con las claves va directo a
MariaDB). Se ejecuta todo en **la misma** terminal SSH, porque las variables solo viven en ella.

```bash
# 1) Valores en variables (openssl dentro de $(...) no imprime nada; read -s no muestra lo escrito)
JWT=$(openssl rand -base64 32)
CLAVE_AUTH=$(openssl rand -base64 18)
CLAVE_TIENDA=$(openssl rand -base64 18)
read -rsp 'Clave demo cliente: ' DEMO_C; echo
read -rsp 'Clave demo vendedor: ' DEMO_V; echo
read -rsp 'Clave demo admin: ' DEMO_A; echo
IP=IP_PUBLICA        # reemplazar por la IP elástica, sin http://

# 2) Bases y usuarios: solo se reemplazan los valores entre comillas, no los comentarios
sed -e "s|'CAMBIAR_CLAVE_AUTH_APP'|'$CLAVE_AUTH'|" \
    -e "s|'CAMBIAR_CLAVE_TIENDA_APP'|'$CLAVE_TIENDA'|" \
    /tmp/ferreteria-deploy/sql/crear_bases.sql | sudo mariadb

# 3) Archivos de entorno a partir de las plantillas
sudo mkdir -p /etc/ferreteria
sed -e "s|=CAMBIAR_CLAVE_AUTH_APP|=$CLAVE_AUTH|" \
    -e "s|=CAMBIAR_SECRETO_BASE64_COMPARTIDO|=$JWT|" \
    -e "s|=http://IP_PUBLICA|=http://$IP|" \
    -e "s|=CAMBIAR_CLAVE_CLIENTE|=$DEMO_C|" \
    -e "s|=CAMBIAR_CLAVE_VENDEDOR|=$DEMO_V|" \
    -e "s|=CAMBIAR_CLAVE_ADMIN|=$DEMO_A|" \
    /tmp/ferreteria-deploy/env/auth.env.example | sudo tee /etc/ferreteria/auth.env > /dev/null
sed -e "s|=CAMBIAR_CLAVE_TIENDA_APP|=$CLAVE_TIENDA|" \
    -e "s|=CAMBIAR_SECRETO_BASE64_COMPARTIDO|=$JWT|" \
    -e "s|=http://IP_PUBLICA|=http://$IP|" \
    /tmp/ferreteria-deploy/env/tienda.env.example | sudo tee /etc/ferreteria/tienda.env > /dev/null
sudo chown root:root /etc/ferreteria/*.env
sudo chmod 600 /etc/ferreteria/*.env

# 4) Borrar las variables (o cerrar la terminal)
unset JWT CLAVE_AUTH CLAVE_TIENDA DEMO_C DEMO_V DEMO_A
```

Después se ejecutan las mismas comprobaciones de arriba (`=CAMBIAR` en 0, un solo `JWT_SECRET`,
la IP en `APP_CORS_ALLOWED_ORIGIN`) y la de 5.2 (bases y usuarios creados).

- Se usa `|` como separador de `sed` porque Base64 puede traer `/` y `+`, pero nunca `|`.
  Por lo mismo, las claves demo **no** deben tener `|`, `&` ni `\` (`sed` los interpreta).
- No hace falta anotar `JWT_SECRET` ni las claves de MariaDB: quedan en `/etc/ferreteria/*.env`
  (se leen con `sudo`). Las claves demo sí las debe recordar el equipo para el login.

---

## 7. Servicios systemd (terminal SSH)

```bash
sudo mkdir -p /opt/ferreteria-auth /opt/ferreteria-tienda
sudo mv /tmp/ferreteria-auth.jar /opt/ferreteria-auth/app.jar
sudo mv /tmp/ferreteria-tienda.jar /opt/ferreteria-tienda/app.jar
sudo chown -R ubuntu:ubuntu /opt/ferreteria-auth /opt/ferreteria-tienda
sudo cp /tmp/ferreteria-deploy/systemd/ferreteria-auth.service /etc/systemd/system/
sudo cp /tmp/ferreteria-deploy/systemd/ferreteria-tienda.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now ferreteria-auth ferreteria-tienda
```

En una t3.micro cada backend tarda entre 30 y 90 segundos en arrancar. Para mirar el arranque
(salir con `Ctrl+C`; el servicio sigue corriendo):

```bash
sudo journalctl -u ferreteria-tienda -f
```

Listo cuando aparece `Started FerreteriaApplication` (en Auth, `Started AuthApplication`).

```bash
sudo systemctl status ferreteria-auth --no-pager
sudo systemctl status ferreteria-tienda --no-pager
```

Resultado esperado: ambos `active (running)`. Cada unidad limita su JVM a 256 MB de heap
(`-Xms128m -Xmx256m`) y con `-XX:+ExitOnOutOfMemoryError` + `Restart=on-failure` se reinicia
sola si se queda sin memoria (sección 13).

---

## 8. Comprobar los backend antes de Nginx (terminal SSH)

```bash
curl -s http://127.0.0.1:8080/api/v1/productos; echo
curl -i http://127.0.0.1:8080/api/v1/boletas
curl -i -X POST http://127.0.0.1:8081/api/v1/auth/login -H "Content-Type: application/json" -d '{"correo":"admin@losmaestros.cl","clave":"incorrecta"}'
sudo ss -lntp | grep -E ':8080|:8081'
```

| Comprobación | Esperado |
|---|---|
| GET productos (público) | `[]` (todavía sin datos) |
| GET boletas sin token | 401 `{"error":"Debes iniciar sesión"}` |
| Login con clave incorrecta | 401 `{"error":"Credenciales inválidas"}` (Auth ya lee su base) |
| `ss` | Java en `127.0.0.1:8080` y `127.0.0.1:8081`, **no** en `0.0.0.0` |

> Un `GET` al login responde 401 (no 405): en `SecurityConfig` de Auth todo lo
> que no está permitido cae en `denyAll`, que sin token responde 401 antes de llegar a Spring MVC.

---

## 9. Datos iniciales de la tienda (terminal SSH)

Con la tienda ya arrancada (sus tablas existen), cargar categorías y productos:

```bash
sudo mariadb ferreteria_los_maestros < /tmp/ferreteria-deploy/datos_iniciales.sql
sudo mariadb -e "SELECT COUNT(*) AS categorias FROM ferreteria_los_maestros.categoria; SELECT COUNT(*) AS productos FROM ferreteria_los_maestros.producto;"
sudo mariadb -e "SHOW INDEX FROM ferreteria_los_maestros.producto WHERE Non_unique = 0;"
sudo mariadb -e "SELECT id, correo, rol, LEFT(clave, 7) AS hash FROM ferreteria_auth.usuario;"
```

Resultado esperado: 8 categorías, 10 productos, el índice UNIQUE sobre `codigo` y los usuarios
1–3 (cliente, vendedor, admin) con clave `$2a$10$` (BCrypt), creados por `DatosIniciales` con las
claves de `DEMO_CLAVE_*`. No hace falta reiniciar la tienda.

---

## 10. React y Nginx (terminal SSH)

```bash
sudo mkdir -p /var/www/ferreteria
sudo cp -a /tmp/ferreteria-web/. /var/www/ferreteria/
sudo chown -R www-data:www-data /var/www/ferreteria
sudo find /var/www/ferreteria -type d -exec chmod 755 {} \;
sudo find /var/www/ferreteria -type f -exec chmod 644 {} \;

sudo cp /tmp/ferreteria-deploy/nginx/ferreteria.conf /etc/nginx/sites-available/ferreteria
sudo ln -sfn /etc/nginx/sites-available/ferreteria /etc/nginx/sites-enabled/ferreteria
sudo unlink /etc/nginx/sites-enabled/default 2>/dev/null || true
sudo nginx -t
sudo systemctl reload nginx
```

Resultado esperado: `nginx -t` dice `syntax is ok` y `test is successful`.

---

## 11. Probar desde fuera (PowerShell local o navegador)

```powershell
curl.exe -s -o NUL -w "%{http_code}`n" "http://$IP/"
curl.exe -s "http://$IP/api/tienda-service/v1/productos"
curl.exe -s -o NUL -w "%{http_code}`n" "http://$IP/api/tienda-service/v1/boletas"
curl.exe -s -m 5 -o NUL -w "%{http_code}`n" "http://${IP}:8080/api/v1/productos"
```

| Caso | Esperado |
|---|---|
| Inicio de React | 200 |
| Productos por Nginx | JSON con los 10 productos |
| Boletas sin token | 401 |
| Puerto 8080 directo | `000` después de 5 s (cerrado en el grupo de seguridad) |

Login y token con PowerShell (reemplazar `CLAVE_ADMIN` por el valor de `DEMO_CLAVE_ADMIN`):

```powershell
$login = Invoke-RestMethod -Method Post -Uri "http://$IP/api/auth-service/v1/auth/login" -ContentType "application/json" -Body '{"correo":"admin@losmaestros.cl","clave":"CLAVE_ADMIN"}'
$token = $login.token
Invoke-RestMethod "http://$IP/api/tienda-service/v1/boletas" -Headers @{ Authorization = "Bearer $token" }
```

En el navegador, `http://IP_PUBLICA`:

| Prueba | Esperado |
|---|---|
| F5 en `/catalogo` y en `/producto/MC001` | La página carga (no 404): `try_files` |
| Login cliente → compra con retiro | Confirmación con la boleta; stock descontado |
| Mi cuenta del cliente | La compra recién hecha |
| Login vendedor | Solo Panel vendedor, Productos y Boletas |
| Login admin | Crear/editar producto, ver boletas y anular la de prueba (devuelve stock) |
| DevTools → Network | Las llamadas van a `/api/auth-service/v1/...` y `/api/tienda-service/v1/...` |

---

## 12. Swagger público (presentación) y túnel SSH (alternativa)

**Público por Nginx** (en la presentación se usa un computador de la sala sin la llave `.pem`, así
que no se puede abrir un túnel SSH):

- Auth: `http://IP_PUBLICA/swagger/auth/`
- Tienda: `http://IP_PUBLICA/swagger/tienda/`

Flujo: en el Swagger de Auth, `POST /api/v1/auth/login` → copiar el `token` → en el Swagger de la
tienda, **Authorize** → pegar el token → probar `GET /api/v1/boletas` (200 con admin o vendedor,
401 sin token, 403 con cliente).

Cómo funciona "Try it out" detrás del proxy: Nginx agrega `X-Forwarded-Prefix: /swagger/tienda`, y
el perfil `prod` tiene `server.forward-headers-strategy=framework`. Con eso Swagger informa como
servidor `http://IP_PUBLICA/swagger/tienda` (se ve en la lista **Servers**), y cada solicitud de
prueba vuelve a pasar por Nginx hasta `127.0.0.1:8080`. Swagger solo muestra la documentación:
los endpoints siguen exigiendo JWT y rol.

**Alternativa: túnel SSH** (con la llave, sin depender de Nginx). PowerShell local,
dejar la ventana abierta:

```powershell
ssh -i $PEM -L 18081:127.0.0.1:8081 -L 18080:127.0.0.1:8080 ubuntu@$IP
```

Abrir `http://localhost:18081/swagger-ui/index.html` (Auth) y
`http://localhost:18080/swagger-ui/index.html` (tienda).

---

## 13. Memoria: revisar y detectar un OOM (terminal SSH)

Consumo aproximado en una t3.micro: cada JVM 300–400 MB de memoria real (256 MB de heap más el
resto de la JVM), MariaDB unos 100 MB, Nginx y el sistema unos 200 MB. Es más que 1 GB, por eso el
swap de 2 GB es obligatorio.

```bash
free -h                                        # RAM y swap usados
swapon --show                                  # el swap está activo
ps -o pid,rss,args -C java | cut -c1-120       # RSS en KB de cada backend
systemctl status ferreteria-tienda --no-pager | grep -i memory
```

Hay dos tipos de "sin memoria":

| Qué pasó | Cómo se detecta |
|---|---|
| La JVM llenó sus 256 MB de heap | `sudo journalctl -u ferreteria-tienda --no-pager \| grep -i OutOfMemoryError` — la JVM termina y systemd la reinicia |
| Linux se quedó sin RAM ni swap y mató el proceso (OOM killer) | `sudo journalctl -k --no-pager \| grep -i -E "out of memory\|oom-kill\|killed process"` y en el servicio `status=9/KILL` |
| Cuántas veces se reinició | `systemctl show ferreteria-tienda -p NRestarts` |

Cambiar `ferreteria-tienda` por `ferreteria-auth` para revisar Auth. Si se repite: confirmar que
el swap está activo, revisar `free -h` y, si el heap es lo que se llena, subir solo ese backend a
`-Xmx320m` en su unidad (`sudo nano /etc/systemd/system/ferreteria-tienda.service`,
`sudo systemctl daemon-reload`, `sudo systemctl restart ferreteria-tienda`).

---

## 14. Diagnóstico (terminal SSH)

```bash
sudo nginx -t
sudo systemctl status nginx --no-pager
sudo systemctl status ferreteria-auth --no-pager
sudo systemctl status ferreteria-tienda --no-pager
sudo journalctl -u ferreteria-auth -n 80 --no-pager
sudo journalctl -u ferreteria-tienda -n 80 --no-pager
sudo tail -n 80 /var/log/nginx/error.log
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8081/v3/api-docs
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/v3/api-docs
```

| Síntoma | Primera revisión |
|---|---|
| 502 Bad Gateway | Backend detenido o todavía arrancando (esperar 1 min), puerto o `proxy_pass` equivocado |
| El servicio no arranca: `Could not resolve placeholder` | Falta esa variable en `/etc/ferreteria/*.env` |
| `Access denied for user` | Usuario, clave o base del `.env` distintos a los de `crear_bases.sql` |
| 401 en la tienda con un token recién emitido | `JWT_SECRET` distinto entre los dos `.env` |
| Error de CORS en el navegador | `APP_CORS_ALLOWED_ORIGIN` no coincide exactamente con la dirección del navegador |
| 404 al recargar una ruta de React | Bloque `try_files` de Nginx |
| Swagger carga pero "Try it out" falla | Falta `X-Forwarded-Prefix` en Nginx o el perfil `prod` no está activo |
| La tienda está vacía | Falta la sección 9 (datos iniciales) |
| El servicio se reinicia solo | Sección 13 (memoria) |

---

## 15. Actualizar una versión desplegada

Backend (ejemplo con la tienda; para Auth usar `ferreteria-auth`). En local, compilar
(sección 3, con XAMPP encendido) y copiar solo ese JAR:

```powershell
scp -i $PEM ferreteria-los-maestros-backend\target\ferreteria-0.0.1-SNAPSHOT.jar "ubuntu@${IP}:/tmp/ferreteria-tienda.jar"
```

En la EC2:

```bash
sudo systemctl stop ferreteria-tienda
sudo mv /tmp/ferreteria-tienda.jar /opt/ferreteria-tienda/app.jar
sudo chown ubuntu:ubuntu /opt/ferreteria-tienda/app.jar
sudo systemctl start ferreteria-tienda
sudo systemctl status ferreteria-tienda --no-pager
```

React: `npm run build` y copiar `dist` de nuevo (sección 3), y en la EC2 repetir las cinco
primeras líneas de la sección 10. No hace falta reiniciar los backend ni Nginx.

---

## 16. Si cambia la IP pública (sin IP elástica)

Al detener y encender la instancia sin IP elástica, AWS entrega otra IP. Qué cambiar:

1. En la EC2, `APP_CORS_ALLOWED_ORIGIN=http://IP_NUEVA` en **los dos** archivos:

   ```bash
   sudo nano /etc/ferreteria/auth.env
   sudo nano /etc/ferreteria/tienda.env
   sudo systemctl restart ferreteria-auth ferreteria-tienda
   ```

2. En local, `$IP = "IP_NUEVA"` para los comandos `ssh` y `scp`.

**No** hace falta recompilar React (usa rutas relativas) ni tocar Nginx (`server_name _` acepta
cualquier IP).

---

## 17. Acceso de emergencia sin `.pem`: EC2 Instance Connect

Sirve desde cualquier navegador (por ejemplo, el computador de la sala), sin la llave.

**Requisitos (dejarlos listos antes del día):**

- La regla de entrada SSH 22 con la lista de prefijos
  `com.amazonaws.REGION.ec2-instance-connect` (sección 1.2). Sin ella, Instance Connect no conecta.
- La instancia tiene IPv4 pública (la elástica sirve).
- Ubuntu 24.04 de AWS trae el paquete `ec2-instance-connect`. Se comprueba con
  `dpkg -l ec2-instance-connect`.

**Conectarse:** consola de AWS → EC2 → Instancias → seleccionar la instancia → **Conectar** →
pestaña **EC2 Instance Connect** → usuario `ubuntu` → **Conectar**. Se abre una terminal en el
navegador.

**Revisar y reiniciar:**

```bash
systemctl is-active mariadb nginx ferreteria-auth ferreteria-tienda
free -h
sudo systemctl restart ferreteria-auth ferreteria-tienda
sudo systemctl restart nginx
sudo journalctl -u ferreteria-tienda -n 50 --no-pager
```

Después de un `restart`, esperar 1–2 minutos antes de probar (mientras arrancan, Nginx responde 502).

---

## 18. Checklist del día de la presentación

**El día anterior**

- [ ] La IP elástica sigue asociada y la URL `http://IP_PUBLICA` abre desde un celular con datos
      móviles (otra red, no la del equipo).
- [ ] Instance Connect funciona (sección 17).
- [ ] El equipo tiene a mano: la URL, los correos demo y sus claves (`DEMO_CLAVE_*`) en un lugar
      que no se proyecte.

**30 minutos antes**

- [ ] Encender la instancia (EC2 → Instancias → **Iniciar instancia**) y esperar 2–3 minutos.
- [ ] Por Instance Connect o SSH: `systemctl is-active mariadb nginx ferreteria-auth ferreteria-tienda`
      → cuatro veces `active`.
- [ ] `free -h`: el swap aparece activo.
- [ ] Desde el celular con datos: `http://IP_PUBLICA` muestra la tienda con productos.
- [ ] F5 en `/catalogo` no da 404.
- [ ] Login **cliente** → compra con retiro → confirmación → aparece en Mi cuenta.
- [ ] Login **vendedor** → solo Panel vendedor, Productos y Boletas; ve la compra recién hecha.
- [ ] Login **administrador** → panel de administración; anular la compra de prueba (devuelve
      stock).
- [ ] `http://IP_PUBLICA/swagger/auth/` → login 200; `http://IP_PUBLICA/swagger/tienda/` →
      Authorize con ese token → `GET /api/v1/boletas` 200.

**Si algo falla en vivo:** Instance Connect → sección 17 (reiniciar) → sección 14 (diagnóstico).
El navegador marca el sitio como "No seguro" porque es `http`, sin certificado: es esperable.

---

## 19. Evidencias sugeridas (capturas)

| Evidencia | Qué debe mostrar |
|---|---|
| Grupo de seguridad | Solo 22 (Mi IP + Instance Connect) y 80 |
| IP elástica | Asociada a la instancia |
| MariaDB | Las dos bases y los dos usuarios (sección 5) |
| `.env` | `ls -l /etc/ferreteria` con `-rw------- root root` (sin abrir el contenido) |
| systemd | `ferreteria-auth` y `ferreteria-tienda` `active (running)` |
| `ss -lntp` | Java solo en `127.0.0.1:8080` y `127.0.0.1:8081` |
| `free -h` | RAM y swap de 2 GB |
| Nginx | `nginx -t` correcto y rutas a puertos distintos (Network del navegador) |
| JWT | Login por `/api/auth-service` y lectura protegida por `/api/tienda-service` |
| Swagger | Los dos Swagger públicos, con **Servers** `http://IP_PUBLICA/swagger/...` |
| React | Login con los tres roles, F5 en una ruta interna y una compra, desde la IP pública |
| Puerto cerrado | `http://IP_PUBLICA:8080` sin respuesta |

Ninguna captura debe mostrar el contenido de los `.env`, `JWT_SECRET`, las claves ni la llave `.pem`.
