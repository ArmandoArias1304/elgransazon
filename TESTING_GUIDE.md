# 🚀 Guía de Prueba - Spring Security Configuration

## ✅ Configuración Completada

Se ha implementado exitosamente:

1. ✅ **Spring Security** con sesiones stateful
2. ✅ **Tabla Employee** para autenticación
3. ✅ **Controladores** que devuelven vistas HTML
4. ✅ **Thymeleaf** para renderizado de páginas
5. ✅ **Arquitectura de 3 capas** (Presentación, Aplicación, Datos)
6. ✅ **BCrypt** para encriptación de contraseñas
7. ✅ **CustomUserDetailsService** para autenticación personalizada

## 📋 Pasos para Probar

### 1️⃣ Crear la Base de Datos

Abre MySQL y ejecuta:

```sql
CREATE DATABASE IF NOT EXISTS bd_restaurant;
USE bd_restaurant;
```

O ejecuta el script completo:

```bash
mysql -u root -p < database/init_employee.sql
```

### 2️⃣ Configurar application.properties

Edita el archivo y ajusta tu password de MySQL:

```properties
spring.datasource.password=TU_PASSWORD_AQUI
```

### 3️⃣ Compilar el Proyecto

Desde PowerShell en la raíz del proyecto:

```powershell
.\mvnw.cmd clean install
```

### 4️⃣ Ejecutar la Aplicación

```powershell
.\mvnw.cmd spring-boot:run
```

### 5️⃣ Probar el Login

1. Abre tu navegador en: `http://localhost:8080`
2. Serás redirigido automáticamente a `/login`
3. Usa estas credenciales:
   - **Usuario**: `admin`
   - **Contraseña**: `password123`

### 6️⃣ Verificar Dashboard

Después del login exitoso:
- Deberías ver el dashboard del POS
- Tu nombre de usuario aparecerá en la navbar
- Podrás hacer logout desde el botón en la navbar

## 🧪 Casos de Prueba

### ✅ Caso 1: Login Exitoso
1. Ir a `http://localhost:8080/login`
2. Ingresar: `admin` / `password123`
3. ✅ **Resultado**: Redirige a `/home` con dashboard

### ✅ Caso 2: Login Fallido
1. Ir a `http://localhost:8080/login`
2. Ingresar credenciales incorrectas
3. ✅ **Resultado**: Mensaje de error "Invalid username or password"

### ✅ Caso 3: Protección de Rutas
1. Cerrar sesión (logout)
2. Intentar acceder a `http://localhost:8080/home`
3. ✅ **Resultado**: Redirige a `/login`

### ✅ Caso 4: Logout
1. Estar logueado
2. Click en botón "Logout"
3. ✅ **Resultado**: Redirige a `/login?logout=true` con mensaje de éxito

### ✅ Caso 5: Sesión Única
1. Loguearse en una ventana del navegador
2. Abrir otra ventana e intentar loguearse con el mismo usuario
3. ✅ **Resultado**: La primera sesión se invalida

### ✅ Caso 6: Timeout de Sesión
1. Loguearse exitosamente
2. Esperar 30 minutos sin actividad
3. Intentar navegar
4. ✅ **Resultado**: Sesión expirada, redirige a login

## 🔐 Usuarios de Prueba Disponibles

| Usuario | Contraseña | Nombre Completo |
|---------|------------|----------------|
| `admin` | `password123` | Administrator |
| `juan` | `password123` | Juan Perez |
| `maria` | `password123` | Maria Garcia |
| `carlos` | `password123` | Carlos Rodriguez |

## 🛠️ Herramientas Incluidas

### Generador de Contraseñas BCrypt

Ejecuta esta clase para generar nuevos hashes:

```bash
java -cp target/classes com.aatechsolutions.elgransazon.util.PasswordHashGenerator
```

## 📊 Verificación de la Configuración

### Verificar que la BD esté lista

```sql
USE bd_restaurant;
SELECT * FROM employee;
```

### Verificar logs de la aplicación

Busca estos mensajes en la consola:

```
✅ "Started ElgransazonApplication"
✅ "Tomcat started on port 8080"
```

### Verificar Spring Security

En los logs deberías ver:

```
✅ "Will secure any request with [...]"
✅ "Creating filter chain"
```

## 🐛 Problemas Comunes y Soluciones

### ❌ Error: "Access to DialectResources.CLASSPATH_RESOURCE denied"

**Solución**: Ya está resuelto, Thymeleaf está configurado correctamente.

### ❌ Error: "Table 'bd_restaurant.employee' doesn't exist"

**Solución**: 
```sql
USE bd_restaurant;
SOURCE database/init_employee.sql;
```

### ❌ Error: "Cannot connect to database"

**Solución**: Verificar que MySQL esté corriendo:
```powershell
mysql -u root -p
```

### ❌ Login no funciona pero no hay errores

**Solución**: Verificar que la contraseña en BD esté hasheada:
```sql
SELECT nombre, LEFT(contrasenia, 20) AS hash_inicio FROM employee;
```
Debe comenzar con `$2a$10$`

## 📁 Archivos Creados

```
✅ domain/entity/Employee.java
✅ domain/repository/EmployeeRepository.java
✅ application/service/CustomUserDetailsService.java
✅ application/service/EmployeeService.java
✅ infrastructure/security/SecurityConfig.java
✅ infrastructure/security/CustomAuthenticationSuccessHandler.java
✅ presentation/controller/AuthController.java
✅ presentation/controller/HomeController.java
✅ templates/auth/login.html
✅ templates/home.html
✅ database/init_employee.sql
✅ util/PasswordHashGenerator.java
✅ SECURITY_CONFIG.md (documentación completa)
```

## 🎯 Próximos Pasos Sugeridos

1. **Agregar más empleados**:
   ```java
   // Usar EmployeeService para crear empleados
   employeeService.create(newEmployee);
   ```

2. **Implementar roles diferentes**:
   - ADMIN, WAITER, CHEF, CASHIER
   - Crear tabla de roles y relación many-to-many

3. **Mejorar UI**:
   - Agregar CSS framework (Bootstrap, Tailwind)
   - Crear componentes reutilizables
   - Agregar validaciones frontend

4. **Funcionalidades adicionales**:
   - Cambio de contraseña
   - Perfil de usuario
   - Auditoría de accesos
   - Remember me

## 📞 Verificación Final

Ejecuta este checklist:

- [ ] Base de datos `bd_restaurant` creada
- [ ] Tabla `employee` con datos de prueba
- [ ] `application.properties` configurado
- [ ] Proyecto compila sin errores (`mvnw clean install`)
- [ ] Aplicación inicia correctamente
- [ ] Login funciona con usuario de prueba
- [ ] Dashboard se muestra correctamente
- [ ] Logout funciona correctamente
- [ ] Rutas protegidas redirigen a login

## 🎉 ¡Listo!

Tu sistema POS ahora tiene:
- ✅ Autenticación segura
- ✅ Sesiones stateful
- ✅ Vistas con Thymeleaf
- ✅ Arquitectura limpia en 3 capas
- ✅ Código siguiendo buenas prácticas
- ✅ SOLID principles aplicados

---

**Nota**: Recuerda habilitar CSRF en producción editando `SecurityConfig.java`:

```java
.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
)
```
