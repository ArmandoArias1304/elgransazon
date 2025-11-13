# 📧 Sistema de Verificación de Email y Restablecimiento de Contraseña

## 📋 Resumen

Se ha implementado un sistema completo de verificación de email y restablecimiento de contraseña para clientes del restaurante "El Gran Sazón".

---

## 🎯 Características Implementadas

### ✅ 1. Verificación de Email

- Los clientes deben verificar su email antes de poder iniciar sesión
- Al registrarse, se envía automáticamente un email de verificación
- Token de verificación válido por 15 minutos
- No se permite login sin email verificado

### ✅ 2. Restablecimiento de Contraseña

- Formulario para solicitar restablecimiento por email
- Token de reset válido por 15 minutos
- Token se marca como usado después de cambiar la contraseña
- Validación de que las contraseñas coincidan

### ✅ 3. Lógica Inteligente de Reenvío

- **Si token NO está vencido**: No se envía nuevo email (esperar 15 minutos)
- **Si token SÍ está vencido**: Se envía nuevo email automáticamente
- Mensajes claros al usuario sobre el estado del envío

---

## 📁 Archivos Creados/Modificados

### Entidades

- ✅ `Customer.java` - Agregado campo `emailVerified`
- ✅ `EmailVerificationToken.java` - Nueva entidad para tokens de verificación
- ✅ `PasswordResetToken.java` - Nueva entidad para tokens de reset

### Repositorios

- ✅ `EmailVerificationTokenRepository.java` - Nuevo repositorio
- ✅ `PasswordResetTokenRepository.java` - Nuevo repositorio

### Servicios

- ✅ `EmailService.java` - Servicio de envío de emails con SendGrid
- ✅ `EmailVerificationService.java` - Lógica de verificación de email
- ✅ `PasswordResetService.java` - Lógica de restablecimiento de contraseña

### Controladores

- ✅ `EmailVerificationController.java` - Endpoint de verificación
- ✅ `PasswordResetController.java` - Endpoints de reset de contraseña
- ✅ `ClientAuthController.java` - Actualizado para enviar email al registrarse
- ✅ `CustomAuthenticationSuccessHandler.java` - Validación de email verificado

### Vistas HTML

- ✅ `forgot-password.html` - Formulario para solicitar reset
- ✅ `reset-password.html` - Formulario para nueva contraseña
- ✅ `verify-email-result.html` - Página de resultado de verificación
- ✅ `loginClient.html` - Actualizado con mensajes de error y link de reset

### Configuración

- ✅ `SecurityConfig.java` - Rutas públicas agregadas
- ✅ `pom.xml` - Dependencia de SendGrid agregada
- ✅ `application.properties` - Ya configurado

### Base de Datos

- ✅ `ADD_EMAIL_VERIFICATION_SYSTEM.sql` - Script de migración

---

## 🔧 Configuración Requerida

### 1. Variables de Entorno

Debes configurar las siguientes variables de entorno:

```bash
# API Key de SendGrid (obtener en https://sendgrid.com)
SENDGRID_API_KEY=SG.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Email desde donde se envían los correos
FROM_EMAIL=noreply@elgransazon.com

# Nombre que aparece como remitente
FROM_NAME=El Gran Sazón
```

### 2. Ejecutar Script SQL

Ejecuta el script `ADD_EMAIL_VERIFICATION_SYSTEM.sql` en tu base de datos MySQL:

```bash
mysql -u root -p bd_restaurant < ADD_EMAIL_VERIFICATION_SYSTEM.sql
```

O desde MySQL Workbench/phpMyAdmin.

### 3. Obtener API Key de SendGrid

1. Crea una cuenta en [SendGrid](https://sendgrid.com)
2. Ve a **Settings** → **API Keys**
3. Crea una nueva API Key con permisos de **Mail Send**
4. Copia la API Key y configúrala en las variables de entorno

---

## 🔄 Flujo de Usuario

### Registro de Cliente

1. Cliente se registra en `/client/register`
2. Sistema crea cuenta con `emailVerified = false`
3. Sistema genera token de verificación (válido 15 min)
4. Sistema envía email con link de verificación
5. Cliente recibe email con botón "Verificar Mi Email"

### Verificación de Email

1. Cliente hace clic en el link del email
2. Sistema valida el token
3. Si es válido: marca `emailVerified = true`
4. Muestra página de éxito
5. Cliente puede ahora iniciar sesión

### Intento de Login Sin Verificar

1. Cliente intenta iniciar sesión
2. Sistema detecta `emailVerified = false`
3. Sistema verifica si existe token vigente:
   - **Token vigente**: Mensaje "revisa tu email, ya enviamos un correo"
   - **Token vencido**: Envía nuevo email automáticamente
4. Bloquea el acceso hasta que verifique

### Restablecimiento de Contraseña

1. Cliente hace clic en "¿Olvidaste tu contraseña?"
2. Ingresa su email en `/client/forgot-password`
3. Sistema genera token de reset (válido 15 min)
4. Sistema envía email con link de restablecimiento
5. Cliente hace clic en el link
6. Ingresa nueva contraseña en `/client/reset-password`
7. Sistema valida token y actualiza contraseña
8. Cliente puede iniciar sesión con nueva contraseña

---

## 📧 Plantillas de Email

### Email de Verificación

- **Asunto**: Verifica tu Correo Electrónico - El Gran Sazón
- **Contenido**: Mensaje de bienvenida con botón verde
- **Acción**: Link a `/client/verify-email?token=XXX`

### Email de Restablecimiento

- **Asunto**: Restablecimiento de Contraseña - El Gran Sazón
- **Contenido**: Instrucciones de reset con botón verde
- **Acción**: Link a `/client/reset-password?token=XXX`

---

## 🛡️ Seguridad

### Tokens

- Generados con `SecureRandom` (48 bytes)
- Codificados en Base64 URL-safe
- Longitud: ~64 caracteres
- Expiración: 15 minutos

### Validaciones

- Email verificado requerido para login
- Contraseña mínimo 6 caracteres
- Contraseñas deben coincidir
- Token solo se puede usar una vez (password reset)

### Spring Security

- Rutas públicas protegidas
- Sesiones invalidadas en intentos fallidos
- Cookies HTTP-only

---

## 🔌 Endpoints

### Públicos (No requieren autenticación)

| Método | Endpoint                         | Descripción                 |
| ------ | -------------------------------- | --------------------------- |
| GET    | `/client/login`                  | Formulario de login         |
| GET    | `/client/register`               | Formulario de registro      |
| POST   | `/client/register`               | Procesar registro           |
| GET    | `/client/verify-email?token=X`   | Verificar email             |
| GET    | `/client/forgot-password`        | Formulario solicitud reset  |
| POST   | `/client/password-reset/request` | Solicitar reset             |
| GET    | `/client/reset-password?token=X` | Formulario nueva contraseña |
| POST   | `/client/password-reset/confirm` | Confirmar nueva contraseña  |

### Protegidos (Requieren autenticación)

| Método | Endpoint            | Descripción           |
| ------ | ------------------- | --------------------- |
| GET    | `/client/dashboard` | Dashboard del cliente |

---

## 🧪 Pruebas Recomendadas

### Caso 1: Registro Normal

1. Registrar nuevo cliente
2. Verificar que llegue el email
3. Hacer clic en el link de verificación
4. Intentar login → Debe funcionar

### Caso 2: Login Sin Verificar

1. Registrar nuevo cliente
2. NO hacer clic en el link
3. Intentar login → Debe bloquearse
4. Mensaje debe indicar revisar email

### Caso 3: Token Expirado

1. Registrar cliente
2. Esperar 15+ minutos
3. Intentar login → Debe enviar nuevo email
4. Verificar con nuevo link

### Caso 4: Reset de Contraseña

1. Ir a "Olvidé mi contraseña"
2. Ingresar email
3. Verificar recepción de email
4. Hacer clic en link
5. Ingresar nueva contraseña
6. Login con nueva contraseña → Debe funcionar

---

## 🐛 Troubleshooting

### No llegan los emails

**Problema**: Los emails no se están enviando

**Soluciones**:

1. Verificar que `SENDGRID_API_KEY` esté configurada
2. Verificar que la API Key sea válida en SendGrid
3. Verificar que `FROM_EMAIL` esté verificado en SendGrid
4. Revisar logs de la aplicación para errores
5. Verificar que no haya firewall bloqueando conexiones SMTP

### Emails van a spam

**Problema**: Los emails llegan a la carpeta de spam

**Soluciones**:

1. Configurar SPF, DKIM y DMARC en SendGrid
2. Usar dominio verificado (no @gmail.com)
3. Agregar dominio al whitelist del cliente

### Token inválido o expirado

**Problema**: El link dice que el token es inválido

**Soluciones**:

1. Verificar que no hayan pasado más de 15 minutos
2. Solicitar nuevo token (intentar login nuevamente)
3. Verificar que la tabla `email_verification_tokens` tenga datos

---

## 📊 Base de Datos

### Tabla: customers

```sql
email_verified BOOLEAN NOT NULL DEFAULT FALSE
```

### Tabla: email_verification_tokens

```sql
id_token BIGINT PRIMARY KEY AUTO_INCREMENT
customer_id BIGINT (FK a customers)
token VARCHAR(100) UNIQUE
expiration DATETIME
created_at DATETIME
```

### Tabla: password_reset_tokens

```sql
id_token BIGINT PRIMARY KEY AUTO_INCREMENT
customer_id BIGINT (FK a customers)
token VARCHAR(100) UNIQUE
expiration DATETIME
used BOOLEAN DEFAULT FALSE
created_at DATETIME
```

---

## 🎨 Diseño de Vistas

Todas las vistas siguen el diseño corporativo de "El Gran Sazón":

- **Color primario**: #38e07b (verde)
- **Fuente**: Work Sans
- **Estilo**: Moderno, limpio, con gradientes
- **Iconos**: Material Symbols Outlined
- **Responsive**: Adaptado a móviles y desktop

---

## 📝 Notas Importantes

1. **Producción**: Cambiar las URLs de `localhost:8080` a tu dominio real en `EmailService.java`
2. **CSRF**: Está deshabilitado, habilitar en producción
3. **HTTPS**: Usar HTTPS en producción para cookies seguras
4. **Rate Limiting**: Considerar limitar intentos de solicitud de reset
5. **Email Templates**: Personalizar con logo y colores corporativos

---

## 🚀 Próximas Mejoras Sugeridas

- [ ] Agregar 2FA (autenticación de dos factores)
- [ ] Histórico de cambios de contraseña
- [ ] Notificación de login desde nuevo dispositivo
- [ ] Rate limiting en endpoints públicos
- [ ] Captcha en registro y reset de contraseña
- [ ] Email templates más elaborados con logo

---

## 👨‍💻 Soporte

Para dudas o problemas, contactar al equipo de desarrollo.

**Fecha de implementación**: Noviembre 2024  
**Versión**: 1.0.0
