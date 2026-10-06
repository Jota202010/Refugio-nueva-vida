# Autenticación JWT para la API

El sitio web conserva su inicio de sesión por formulario y sesión de Spring Security. JWT se usa por separado para endpoints de API:

- `POST /api/auth/token`: valida usuario y contraseña y devuelve un access token Bearer.
- `GET /api/auth/me`: requiere el token y devuelve el nombre de usuario y sus roles.
- `GET /api/support-chat/status` y `POST /api/support-chat`: siguen siendo endpoints públicos del chat y no requieren JWT.

Los tokens se firman con HS256, expiran a los 15 minutos y contienen únicamente el nombre de usuario, los roles y las marcas de tiempo. No se emiten refresh tokens. El backend no guarda los tokens ni ofrece revocación individual; deja de aceptarlos al expirar o al cambiar la clave de firma.

## Probar la API

Solicita un token usando las credenciales de una cuenta existente:

```bash
curl -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"adoptante1\",\"password\":\"Adopta2026!\"}"
```

La respuesta contiene `accessToken`, `tokenType` (`Bearer`) y `expiresIn` en segundos. Usa ese token en el endpoint protegido:

```bash
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer TU_ACCESS_TOKEN"
```

Las credenciales de demostración están en [docs/demo-data.md](demo-data.md). No uses las contraseñas de demostración fuera de Docker local.

## Clave de firma

Para desarrollo y demostración, si `JWT_SECRET` no está configurada, la aplicación genera una clave aleatoria en memoria y registra un aviso. Los tokens emitidos dejan de funcionar al reiniciar la aplicación. Esto permite iniciar la demo sin copiar secretos al repositorio.

Para conservar tokens durante reinicios o ejecutar varias instancias, configura `JWT_SECRET` con una clave Base64 aleatoria de al menos 256 bits, por ejemplo:

```bash
openssl rand -base64 32
```

Guárdala únicamente en el entorno o en un `.env` local ignorado por Git. No pongas la clave en `compose.yaml`, el código ni el historial de Git. Si la variable está configurada con Base64 inválido o con menos de 256 bits, la aplicación falla al iniciar en vez de usar una clave débil.
