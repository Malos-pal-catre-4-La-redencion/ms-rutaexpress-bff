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

## Pruebas automatizadas

```bash
mvn test
```

`PingControllerTest` simula JWTs con distintos roles (sin depender de Azure
AD real) para verificar que la autorización por rol funciona.
