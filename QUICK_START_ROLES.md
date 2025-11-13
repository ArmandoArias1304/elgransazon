# 🚀 Guía Rápida - Sistema de Roles

## Pasos para Iniciar

### 1️⃣ Actualizar Base de Datos
```bash
mysql -u root -p < database/init_employee_with_roles.sql
```

### 2️⃣ Verificar Datos
```sql
USE bd_restaurant;

-- Ver roles
SELECT * FROM roles;

-- Ver empleados y sus roles
SELECT 
    e.nombre,
    e.apellido,
    r.nombre_rol
FROM employee e
JOIN employee_roles er ON e.id_empleado = er.id_empleado
JOIN roles r ON er.id_rol = r.id_rol
ORDER BY e.nombre;
```

### 3️⃣ Iniciar Aplicación
```powershell
.\mvnw.cmd spring-boot:run
```

### 4️⃣ Probar Usuarios

| Usuario | Password | Rol | Redirige a |
|---------|----------|-----|------------|
| admin | password123 | Admin | `/admin/dashboard` |
| juan | password123 | Waiter | `/waiter/dashboard` |
| maria | password123 | Waiter | `/waiter/dashboard` |
| carlos | password123 | Chef | `/chef/dashboard` |
| ana | password123 | Chef | `/chef/dashboard` |

## ✅ Lo que se implementó:

### 🎭 Entidades y Relaciones
- ✅ Entidad `Role` con constantes de roles
- ✅ Relación Many-to-Many entre `Employee` y `Role`
- ✅ Tabla intermedia `employee_roles`

### 🔐 Seguridad
- ✅ Autenticación basada en roles desde BD
- ✅ Rutas protegidas por rol (`/admin/**`, `/waiter/**`, `/chef/**`)
- ✅ Redirección automática según rol del usuario

### 🎨 Vistas
- ✅ Dashboard personalizado para Administrador (morado)
- ✅ Dashboard personalizado para Mesero (rosa)
- ✅ Dashboard personalizado para Cocinero (naranja-amarillo)

### 🎯 Controladores
- ✅ `AdminController` - Panel de administrador
- ✅ `WaiterController` - Panel de mesero
- ✅ `ChefController` - Panel de cocinero

### 🗄️ Base de Datos
- ✅ Script SQL con tablas y datos de prueba
- ✅ 3 roles predefinidos
- ✅ 5 usuarios de ejemplo con roles asignados

## 🎯 Funcionalidades Principales

### Administrador puede:
- 👥 Gestionar empleados
- 📊 Ver reportes de ventas
- 📦 Controlar inventario
- ⚙️ Configurar sistema
- 🏪 Gestionar mesas
- 📱 Ver órdenes en vivo

### Mesero puede:
- ➕ Crear nuevas órdenes
- 🪑 Gestionar sus mesas
- 📋 Ver órdenes activas
- 📖 Consultar menú
- 💳 Procesar pagos
- 📊 Ver sus ventas

### Cocinero puede:
- 🔔 Ver órdenes pendientes
- 🍳 Gestionar órdenes en progreso
- ✅ Marcar órdenes listas
- 📦 Revisar inventario de cocina
- 📖 Consultar recetas
- 📊 Ver estadísticas

## 🔧 Características Técnicas

### Código Limpio
- ✅ Nombres descriptivos en inglés
- ✅ Documentación JavaDoc completa
- ✅ Logging en todas las capas
- ✅ Manejo de excepciones robusto

### Arquitectura
- ✅ 3 capas (Presentación, Aplicación, Datos)
- ✅ Patrón MVC implementado
- ✅ Principios SOLID aplicados
- ✅ Relaciones JPA correctamente configuradas

### Seguridad
- ✅ BCrypt para contraseñas
- ✅ Sesiones stateful
- ✅ Control de acceso por rol
- ✅ Redirección basada en autoridad

## 📝 Notas Importantes

1. **Todos los passwords de prueba**: `password123`
2. **Los roles deben empezar con**: `ROLE_` (ej: `ROLE_ADMIN`)
3. **Relación Many-to-Many**: Un empleado puede tener múltiples roles
4. **FetchType.EAGER**: Los roles se cargan automáticamente con el empleado
5. **Cascada ON DELETE**: Si se borra un empleado, se borran sus relaciones

## 🎉 ¡Listo para usar!

Ahora tu POS tiene un sistema completo de roles con:
- ✅ Autenticación y autorización
- ✅ Dashboards personalizados por rol
- ✅ Control de acceso granular
- ✅ Interfaz moderna y responsiva
- ✅ Código limpio y mantenible

---

**¿Siguiente paso?** Implementar las funcionalidades específicas de cada rol (CRUD de órdenes, gestión de mesas, etc.)
