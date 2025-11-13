# Spring Security Configuration - Restaurant POS

## 📋 Configuración Completada

Se ha configurado Spring Security con las siguientes características:

### ✅ Características Implementadas

1. **Sesiones Stateful**
   - Gestión de sesiones mediante cookies
   - Timeout de sesión: 30 minutos
   - Máximo 1 sesión por usuario

2. **Autenticación con Base de Datos**
   - UserDetailsService personalizado
   - Contraseñas encriptadas con BCrypt
   - Tabla `employee` para usuarios del POS

3. **Thymeleaf Integration**
   - Vistas HTML dinámicas
   - Integración con Spring Security
   - Páginas de login y dashboard

4. **Arquitectura en 3 Capas**
   - **Presentación**: Controllers (AuthController, HomeController)
   - **Aplicación/Negocio**: Services (CustomUserDetailsService)
   - **Acceso a Datos**: Repositories (EmployeeRepository), Entities (Employee)

## 🗄️ Estructura de Base de Datos

### Tabla: `employee`

```sql
CREATE TABLE employee (
    id_empleado BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    contrasenia VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uk_employee_nombre (nombre)
);
```

## 🚀 Instrucciones de Configuración

### 1. Configurar Base de Datos

Ejecuta el script SQL ubicado en `database/init_employee.sql`:

```bash
mysql -u root -p < database/init_employee.sql
```

O desde MySQL Workbench/phpMyAdmin ejecuta el contenido del archivo.

### 2. Configurar Credenciales de Base de Datos

Edita `src/main/resources/application.properties`:

```properties
spring.datasource.username=root
spring.datasource.password=tu_password_aqui
```

### 3. Compilar el Proyecto

```bash
./mvnw clean install
```

### 4. Ejecutar la Aplicación

```bash
./mvnw spring-boot:run
```

### 5. Acceder al Sistema

Abre tu navegador en: `http://localhost:8080`

## 👤 Usuarios de Prueba

El script SQL incluye usuarios de prueba con la contraseña: **`password123`**

| Usuario | Nombre Completo | Contraseña |
|---------|----------------|------------|
| admin   | Administrator  | password123 |
| juan    | Juan Perez     | password123 |
| maria   | Maria Garcia   | password123 |
| carlos  | Carlos Rodriguez | password123 |

## 📁 Estructura del Proyecto

```
src/main/java/com/aatechsolutions/elgransazon/
├── domain/
│   ├── entity/
│   │   └── Employee.java                    # Entidad JPA
│   └── repository/
│       └── EmployeeRepository.java          # Repositorio Spring Data
├── application/
│   └── service/
│       └── CustomUserDetailsService.java    # Lógica de autenticación
├── infrastructure/
│   └── security/
│       ├── SecurityConfig.java              # Configuración de seguridad
│       └── CustomAuthenticationSuccessHandler.java
└── presentation/
    └── controller/
        ├── AuthController.java              # Controlador de login
        └── HomeController.java              # Controlador principal

src/main/resources/
├── templates/
│   ├── auth/
│   │   └── login.html                       # Vista de login
│   └── home.html                            # Vista de dashboard
└── application.properties                    # Configuración
```

## 🔐 Endpoints de Seguridad

| Endpoint | Método | Descripción | Acceso |
|----------|--------|-------------|--------|
| `/login` | GET | Página de login | Público |
| `/perform_login` | POST | Procesar login | Público |
| `/logout` | POST | Cerrar sesión | Autenticado |
| `/`, `/home` | GET | Dashboard principal | Autenticado |

## 🛠️ Características de Seguridad

### Autenticación
- ✅ Formulario de login personalizado
- ✅ Encriptación BCrypt
- ✅ Protección contra fuerza bruta (sesión única)

### Sesiones
- ✅ Stateful (cookies)
- ✅ Timeout automático (30 min)
- ✅ Invalidación al logout
- ✅ Una sesión activa por usuario

### Autorización
- ✅ Todos los empleados tienen rol `ROLE_EMPLOYEE`
- ✅ Rutas protegidas excepto login y recursos estáticos
- ✅ Redirección automática a login si no autenticado

## 📝 Próximos Pasos Recomendados

1. **Agregar Roles Diferenciados**
   - Crear tabla de roles (ADMIN, WAITER, CASHIER, etc.)
   - Implementar autorización basada en roles

2. **Mejorar Seguridad**
   - Habilitar CSRF en producción
   - Implementar rate limiting
   - Agregar auditoría de logins

3. **Funcionalidades Adicionales**
   - Recordar sesión ("Remember Me")
   - Recuperación de contraseña
   - Cambio de contraseña en primer login

4. **Testing**
   - Tests de integración con Spring Security
   - Tests de controladores
   - Tests de servicios

## 🔧 Herramientas de Desarrollo

### Generar Hash BCrypt para Contraseñas

Puedes usar este código Java para generar hashes:

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "password123";
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println("Encoded: " + encodedPassword);
    }
}
```

## ⚠️ Notas Importantes

1. **CSRF está deshabilitado** en `SecurityConfig` para facilitar el desarrollo. Habilítalo en producción.

2. **Cambiar password de BD** en `application.properties` según tu configuración local.

3. **Las contraseñas** deben estar encriptadas con BCrypt antes de almacenarse en la BD.

4. **Session cookies** configuradas como `http-only` para prevenir XSS.

## 🐛 Troubleshooting

### Error: Access Denied / 403
- Verifica que CSRF esté deshabilitado o configúralo correctamente
- Confirma que el usuario existe en la BD

### Error: Cannot connect to database
- Verifica que MySQL esté corriendo en `localhost:3306`
- Confirma credenciales en `application.properties`
- Asegúrate de que la BD `bd_restaurant` exista

### Error: Invalid username or password
- Verifica que la contraseña esté hasheada con BCrypt
- Confirma que el usuario tenga `enabled = TRUE`
- Revisa los logs para más detalles

## 📚 Referencias

- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [Thymeleaf + Spring Security](https://www.thymeleaf.org/doc/articles/springsecurity.html)
- [BCrypt Password Encoder](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/crypto/bcrypt/BCryptPasswordEncoder.html)
