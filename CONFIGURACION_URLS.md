# Configuración de URLs Dinámicas

## Variables de Entorno

El sistema ahora usa variables de entorno para construir las URLs dinámicamente, permitiendo cambiar fácilmente entre desarrollo y producción.

### Variables Requeridas

```bash
APP_PROTOCOL=http          # Protocolo: http o https
APP_DOMAIN=localhost        # Dominio o IP del servidor
APP_PORT=8080              # Puerto (opcional, ver reglas abajo)
```

## Reglas de Construcción de URL

El sistema construye la URL base de la siguiente manera:

1. **Con puerto explícito**: `{protocolo}://{dominio}:{puerto}`

   - Ejemplo: `http://localhost:8080`

2. **Sin puerto (vacío)**: `{protocolo}://{dominio}`

   - Ejemplo: `https://elgransazon.com`

3. **Puerto por defecto omitido**:
   - Si protocolo=`http` y puerto=`80` → No incluye puerto
   - Si protocolo=`https` y puerto=`443` → No incluye puerto
   - Resultado: `https://elgransazon.com`

## Ejemplos de Configuración

### Desarrollo Local

```bash
APP_PROTOCOL=http
APP_DOMAIN=localhost
APP_PORT=8080
```

**URL generada**: `http://localhost:8080/client/reset-password?token=...`

---

### Producción con HTTPS (sin puerto)

```bash
APP_PROTOCOL=https
APP_DOMAIN=elgransazon.com
APP_PORT=
```

**URL generada**: `https://elgransazon.com/client/reset-password?token=...`

---

### Producción con HTTPS en puerto 443 (omitido)

```bash
APP_PROTOCOL=https
APP_DOMAIN=elgransazon.com
APP_PORT=443
```

**URL generada**: `https://elgransazon.com/client/reset-password?token=...`
(El puerto 443 se omite porque es el predeterminado para HTTPS)

---

### Producción con puerto personalizado

```bash
APP_PROTOCOL=https
APP_DOMAIN=api.elgransazon.com
APP_PORT=8443
```

**URL generada**: `https://api.elgransazon.com:8443/client/reset-password?token=...`

---

### Servidor con IP y puerto

```bash
APP_PROTOCOL=http
APP_DOMAIN=192.168.1.100
APP_PORT=3000
```

**URL generada**: `http://192.168.1.100:3000/client/reset-password?token=...`

## Configuración en Diferentes Entornos

### Heroku

Agregar las variables de entorno en Settings → Config Vars:

```
APP_PROTOCOL=https
APP_DOMAIN=tu-app.herokuapp.com
APP_PORT=
```

### Railway / Render

Agregar en Environment Variables:

```
APP_PROTOCOL=https
APP_DOMAIN=tu-app.railway.app
APP_PORT=
```

### Servidor VPS (Linux)

Crear archivo `.env` en la raíz del proyecto:

```bash
# /opt/elgransazon/.env
APP_PROTOCOL=https
APP_DOMAIN=elgransazon.com
APP_PORT=
```

O exportar como variables de sistema:

```bash
export APP_PROTOCOL=https
export APP_DOMAIN=elgransazon.com
export APP_PORT=
```

### Docker

En `docker-compose.yml`:

```yaml
environment:
  - APP_PROTOCOL=https
  - APP_DOMAIN=elgransazon.com
  - APP_PORT=
```

## URLs Afectadas

Las siguientes funcionalidades usan estas variables:

1. **Password Reset** (Recuperación de contraseña):

   - Ruta: `/client/reset-password?token={token}`
   - Email: `sendPasswordResetEmail()`

2. **Email Verification** (Verificación de email):
   - Ruta: `/client/verify-email?token={token}`
   - Email: `sendEmailVerification()`

## Valores por Defecto

Si no se configuran las variables de entorno, se usan estos valores por defecto (desarrollo):

```properties
app.protocol=http
app.domain=localhost
app.port=8080
```

## Validación

Para verificar que la configuración es correcta, revisa los logs al iniciar la aplicación:

```log
Sending password reset email to: usuario@ejemplo.com
```

Y verifica que la URL en el email sea la correcta según tu entorno.

## Solución de Problemas

### URLs incorrectas en emails

1. Verifica que las variables estén configuradas: `echo $APP_DOMAIN`
2. Revisa el log al enviar email - debe mostrar la URL correcta
3. Confirma que no haya espacios en las variables de entorno

### Puerto aparece cuando no debería

- Si usas `https` + puerto `443`, asegúrate que `APP_PORT=443` o `APP_PORT=` (vacío)
- Si usas `http` + puerto `80`, asegúrate que `APP_PORT=80` o `APP_PORT=` (vacío)

### URLs siguen usando localhost en producción

- Asegúrate de configurar las variables de entorno en el servidor
- Reinicia la aplicación después de configurar las variables
- Verifica que no estés usando un archivo `.env` local que sobrescriba las variables
