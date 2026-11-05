# Despliegue separado en una EC2

React se compila en `front/dist`. El JAR contiene únicamente el backend.
Nginx sirve React y reenvía `/api/` y `/ws` a Java en `127.0.0.1:8081`.
El navegador utiliza una sola dirección; API y WebSocket mantienen sus rutas relativas.

## Crear el paquete en Windows

Requisitos: PowerShell 7.5+, Node compatible con Vite, npm, Maven y JDK 17+.

```powershell
./scripts/preparar-release.ps1
```

Instala dependencias del lockfile, compila React y ejecuta `mvn clean verify`.
El ZIP en `dist/release-FECHA-HORA.zip` contiene:

- `frontend/`: HTML, CSS, JavaScript y recursos para Nginx.
- `paqrap.jar`: backend sin React.
- `deploy/nginx.conf`: configuración de referencia HTTP para EC2.
- `data/`: archivos para diario, 5D y colapso.
- Guías y checksum SHA-256 del JAR.

Los archivos antiguos en `back/src/main/resources/static` se excluyen del
empaquetado; no es necesario borrarlos para compilar.

## Configurar EC2

Instalar Java 17+ y Nginx. Descomprimir el ZIP y copiar el contenido de
`frontend/` a `/var/www/paqrap/`, con permisos de lectura para Nginx.
Guardar el JAR en `/opt/paqrap/paqrap.jar`.

```sh
sudo mkdir -p /var/www/paqrap /opt/paqrap
sudo cp -r frontend/. /var/www/paqrap/
sudo cp paqrap.jar /opt/paqrap/paqrap.jar
sudo cp deploy/nginx.conf /etc/nginx/conf.d/paqrap.conf
```

Deshabilitar el sitio predeterminado de Nginx si ocupa `default_server` en
puerto 80. En Ubuntu suele ser el enlace `/etc/nginx/sites-enabled/default`.
Después validar y recargar:

```sh
sudo nginx -t
sudo systemctl reload nginx
```

Ejecutar Java (más adelante se debe registrar como servicio):

```sh
CORS_ALLOWED_ORIGINS=http://IP_PUBLICA java -Xms256m -Xmx1024m -jar /opt/paqrap/paqrap.jar --server.address=127.0.0.1 --server.port=8081
```

Sustituir `IP_PUBLICA` por la dirección real. Si cambia al encender EC2,
actualizar el origen permitido. Abrir `http://IP_PUBLICA` en el navegador.
El puerto 8081 no necesita exponerse en el grupo de seguridad.
La plantilla es HTTP; configurar TLS cuando se habilite HTTPS y actualizar
el origen permitido. Ajustar la memoria después de medir las simulaciones.

## Actualizar solo React

Compilar con `VITE_USE_MOCK=false`, `VITE_API_URL` y `VITE_WS_URL` vacías.
Copiar primero los nuevos assets y después el HTML:

```sh
sudo cp -r frontend/assets/. /var/www/paqrap/assets/
sudo cp frontend/index.html /var/www/paqrap/index.html
```

Copiar también cualquier otro recurso público modificado de `frontend/`.
Conservar temporalmente los assets anteriores para navegadores con pestañas
abiertas. No hace falta reiniciar Java ni recargar Nginx si su configuración
no cambió. Para actualizar el backend sí hay que reemplazar el JAR y reiniciar
su proceso; esto pierde las corridas y archivos cargados en memoria.

## Verificar

Desde la raíz del repositorio, con Node 24 y un servidor de pruebas activo:

```sh
node scripts/verificar-paquete.mjs http://IP_PUBLICA
```

Comprueba páginas a través de Nginx, API, carga de los cinco archivos,
registro de un pedido diario y snapshot STOMP/WebSocket. Modifica el estado
de la demo: no ejecutarlo contra una operación real que deba conservarse.
Para comprobar solo el backend local:

```sh
node scripts/verificar-paquete.mjs http://127.0.0.1:18081 --backend-only
```

El desarrollo sigue usando Vite y Spring Boot en dos terminales.
`npm run preview` no reemplaza Nginx para esta comprobación integrada.
Los scripts `test:map` y `test:daily` referenciados por package.json faltan en
el repositorio y no se consideran pruebas ejecutadas.

## Datos de muestra

- Diario: registrar (34,15), cliente c9103, cantidad 4 y plazo 12 horas.
- 5D corta: `ventas_small.txt` y `bloqueo_small.txt`.
- 5D completa: `ventas.txt` y `bloqueo.txt`.
- Colapso: `pedidos_colapso.txt`, opcionalmente `bloqueo.txt`.

El día 01 corresponde a la fecha inicial elegida. Consultar
`GUIA_DE_PRUEBA.md` para el guion completo.
