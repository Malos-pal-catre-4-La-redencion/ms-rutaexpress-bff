# ms-rutaexpress-bff

BFF de RutaExpress. Valida el JWT emitido por Azure AD (issuer + firma) y
autoriza cada endpoint según el claim `roles` del token, usando
`@PreAuthorize` de Spring Security.

## Requisitos

- Java 17
- Maven 3.9+ (o el wrapper `./mvnw` si lo agregas con `mvn -N wrapper:wrapper`)

## Correr en local

```bash
mvn spring-boot:run
```

Por defecto levanta en `http://localhost:8081` y valida contra el tenant de
RutaExpress. No necesitas exportar variables de entorno para desarrollo local
— los defaults en `application.yml` ya apuntan a los IDs reales del proyecto.

## Cómo conseguir un token para probar (mientras no existe el frontend)

Pega esta URL en el navegador (reemplaza nada, ya tiene los IDs reales) e
inicia sesión con cualquiera de los 4 usuarios de prueba:

```
https://login.microsoftonline.com/4a684343-6276-4c46-9913-5450f4672cbe/oauth2/v2.0/authorize?client_id=b0361a22-65c8-4c6c-ac3a-767662d4a975&response_type=token&redirect_uri=https%3A%2F%2Fjwt.ms&scope=openid%20profile%20api%3A%2F%2Fb0361a22-65c8-4c6c-ac3a-767662d4a975%2Faccess_as_user&response_mode=fragment&nonce=112233
```

jwt.ms va a mostrar el **access token** (no el id token de la vez pasada).
Para reconocerlo: trae los claims `scp` y `azp`, y NO trae `at_hash`. Su `aud`
es el client id en formato GUID (`b0361a22-...`), sin prefijo — así son los
tokens v2.0, a diferencia de v1.0 que sí usaba `api://`. Copia el token largo
de la caja de texto de arriba (no el JSON decodificado de abajo).

## Probar los endpoints

```bash
TOKEN="<pega aquí el access token>"

# Cualquier usuario autenticado
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/bff/me

# Solo Admin (200 con admin, 403 con cualquier otro rol)
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/bff/admin/ping

# Sin token (debe dar 401)
curl -i http://localhost:8081/api/bff/ping
```

Repite con un token de cada rol (Admin, Despachador, Cliente, Auditor) para
confirmar que cada uno solo entra a lo que le corresponde — eso es
exactamente lo que pide el punto de "BFF valida el token y autoriza por rol"
de la rúbrica EP1.

## Nuevo: proxy hacia shipments y catalog

El BFF ahora reenvía hacia los otros dos microservicios. Para probarlo
completo necesitas los 3 corriendo a la vez:

```bash
# terminal 1
cd ms-rutaexpress-shipments && mvn spring-boot:run -Dspring-boot.run.profiles=local
# terminal 2
cd ms-rutaexpress-catalog && mvn spring-boot:run -Dspring-boot.run.profiles=local
# terminal 3
cd ms-rutaexpress-bff && mvn spring-boot:run
```

Con un token de `admin` o `despachador`:

```powershell
# Crear un envío a través del BFF (no directo a shipments)
curl.exe -i -X POST http://localhost:8081/api/bff/envios `
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" `
  -d '{\"remitente\":\"Tienda RutaExpress\",\"destinatario\":\"Juan Pérez\",\"direccionDestino\":\"Av. Siempre Viva 123\",\"pesoKg\":12.5}'

# Ver el catálogo
curl.exe -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/bff/servicios
```

Con un token de `cliente`, probar `POST /api/bff/servicios` debería dar
**403** (solo Admin puede editar el catálogo) — esa es la prueba de que las
reglas de rol están donde corresponde: en el BFF, no en cada microservicio.

## Nuevo (EP2): proxy de administración de RabbitMQ

El BFF reenvía hacia `ms-rutaexpress-rabbit-admin` (URL en `RABBIT_ADMIN_URL`, por
defecto `http://localhost:8084`). **Toda** la ruta `/api/bff/admin/**` de este
controlador exige rol `Admin` (`@PreAuthorize` a nivel de clase); cualquier otro rol
recibe **403** sin que la llamada llegue al microservicio, y sin token, **401**.

| Método | Ruta del BFF | Reenvía a |
|---|---|---|
| POST | `/api/bff/admin/queues` | `POST /queues` |
| GET | `/api/bff/admin/queues/{name}` | `GET /queues/{name}` |
| DELETE | `/api/bff/admin/queues/{name}` | `DELETE /queues/{name}` |
| POST | `/api/bff/admin/exchanges` | `POST /exchanges` |
| DELETE | `/api/bff/admin/exchanges/{name}` | `DELETE /exchanges/{name}` |
| POST | `/api/bff/admin/bindings` | `POST /bindings` |
| DELETE | `/api/bff/admin/bindings?exchange=&cola=&routingKey=` | `DELETE /bindings` |

Los errores del administrador (400 de validación con su mensaje claro, 404, 409, 503) se
reenvían tal cual; el frontend los muestra sin transformarlos.

```bash
# Con un token de Admin: crea una cola
curl -i -X POST http://localhost:8081/api/bff/admin/queues \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre":"q.demo","ttlMs":60000,"maxLength":100}'

# Con un token de Despachador o Cliente: 403.
# Con nombre vacío (token Admin): 400 con {"campos":{"nombre":"El nombre de la cola es obligatorio"}}
curl -i -X POST http://localhost:8081/api/bff/admin/queues \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"nombre":""}'
```

## Pruebas automatizadas

```bash
mvn test
```

`PingControllerTest` simula JWTs con distintos roles (sin depender de Azure
AD real) para verificar que la autorización por rol funciona.
`RabbitAdminProxyControllerTest` comprueba que la ruta de administración rechaza
sin token (401) y a cualquier rol que no sea Admin (403), y que Admin sí pasa.
