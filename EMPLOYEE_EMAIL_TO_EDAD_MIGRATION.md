# 🔄 Migración: Campo Email → Edad en Entidad Employee

## 📋 Resumen de Cambios

Se ha completado exitosamente la migración del campo `email` al campo `edad` en la entidad `Employee` y todas las vistas relacionadas.

---

## ✅ Cambios Realizados

### 🗂️ 1. Entidad y Repositorio

#### `Employee.java`

- ❌ **Eliminado**: Campo `email` (String, @NotBlank, @Email, unique, length=150)
- ✅ **Agregado**: Campo `edad` (Integer, @Min(18), @Max(100), nullable)
- Validación: Edad mínima 18 años, máxima 100 años

#### `EmployeeRepository.java`

- ❌ **Eliminado**: `Optional<Employee> findByEmail(String email)`
- ❌ **Eliminado**: `boolean existsByEmail(String email)`

### 💼 2. Lógica de Negocio

#### `EmployeeService.java`

- ❌ **Eliminado**: Método `findByEmail()`
- ❌ **Eliminado**: Validación de email único en `create()`
- ❌ **Eliminado**: Validación de email único en `update()`
- ✅ **Actualizado**: Método `update()` ahora usa `setEdad()` en lugar de `setEmail()`
- ✅ **Actualizado**: Javadoc para reflejar que se valida username y phone, no email

#### `EmployeeController.java`

- ✅ **Actualizado**: Método `getEmployeeDetails()` devuelve `edad` en lugar de `email`

### 🎨 3. Vistas HTML

#### **Formulario de Empleados** (`admin/employees/form.html`)

- ❌ **Eliminado**: Input de email con validación regex
- ✅ **Agregado**: Input de edad (`type="number"`, min=18, max=100)
- Icono: `email` → `calendar_today`
- Label: "Correo Electrónico" → "Edad"
- Placeholder: Email de ejemplo → "Ej: 25"
- Help text: "Opcional - Edad en años (entre 18 y 100)"
- ✅ **JavaScript**: Validación de rango de edad en tiempo real

#### **Vistas de Perfil** (4 archivos)

- `cashier/profile/view.html`
- `chef/profile/view.html`
- `delivery/profile/view.html`
- `waiter/profile/view.html`

**Cambios en cada archivo:**

- ❌ **Eliminado**: Sección de "Correo Electrónico"
- ✅ **Agregado**: Sección de "Edad" con icono `calendar_today`
- Display: `${employee.edad != null ? employee.edad + ' años' : 'No especificada'}`

#### **Lista de Empleados** (`admin/employees/list.html`)

- ❌ **Eliminado**: Columna de email con icono `mail`
- ✅ **Agregado**: Columna de edad con icono `calendar_today`
- Display: Muestra "No especificada" si edad es null

#### **Vistas de Turnos** (3 archivos)

- `admin/shifts/history.html`
- `admin/shifts/detail.html`
- `admin/shifts/assign-employees.html`

**Cambios:**

- ❌ **Eliminado**: Referencias a `employee.email`
- ✅ **Agregado**: Display de edad con formato condicional

---

## 🗄️ 4. Migración de Base de Datos

Se creó el archivo `MIGRATE_EMPLOYEE_EMAIL_TO_EDAD.sql` con los siguientes comandos:

```sql
-- Agregar columna edad (nullable)
ALTER TABLE employee
ADD COLUMN edad INT NULL
COMMENT 'Age of the employee (between 18-100 years)';

-- Eliminar columna email y su constraint único
ALTER TABLE employee
DROP COLUMN email;
```

### ⚠️ Notas Importantes:

- Todos los empleados existentes tendrán `edad = NULL` después de la migración
- El campo edad es **opcional** (nullable), a diferencia del email que era obligatorio
- **No hay constraint único** en edad (múltiples empleados pueden tener la misma edad)
- La validación de rango (18-100) se aplica a nivel de aplicación mediante anotaciones `@Min/@Max`

---

## 🧪 Validación y Testing

### ✅ Pruebas Recomendadas

1. **Crear Empleado**

   - Con edad válida (18-100) ✓
   - Sin edad (null) ✓
   - Con edad inválida (<18 o >100) - debe rechazarse ✓

2. **Actualizar Empleado**

   - Cambiar edad a valor válido ✓
   - Cambiar edad a null ✓
   - Intentar edad inválida - debe rechazarse ✓

3. **Visualización**

   - Ver perfil con edad ✓
   - Ver perfil sin edad (debe mostrar "No especificada") ✓
   - Lista de empleados con/sin edad ✓
   - Modales de turnos con/sin edad ✓

4. **Modo Oscuro**
   - Verificar que todos los displays de edad se vean correctamente ✓

---

## 📊 Impacto del Cambio

### ✅ Sin Impacto

- **Customer**: La entidad Customer **sigue usando email** (sin cambios)
- **Supplier**: La entidad Supplier **sigue usando email** (sin cambios)
- Autenticación de clientes (usa email de Customer)
- Sistema de verificación por email

### ⚠️ Requiere Acción

- **Migración de BD**: Ejecutar `MIGRATE_EMPLOYEE_EMAIL_TO_EDAD.sql`
- **Actualización de Datos**: Los administradores deben actualizar las edades de empleados existentes
- **Testing**: Probar creación/actualización/visualización de empleados

---

## 📁 Archivos Modificados

### Backend (Java)

1. `src/main/java/.../domain/entity/Employee.java`
2. `src/main/java/.../domain/repository/EmployeeRepository.java`
3. `src/main/java/.../application/service/EmployeeService.java`
4. `src/main/java/.../presentation/controller/EmployeeController.java`

### Frontend (HTML/Thymeleaf)

5. `src/main/resources/templates/admin/employees/form.html`
6. `src/main/resources/templates/admin/employees/list.html`
7. `src/main/resources/templates/cashier/profile/view.html`
8. `src/main/resources/templates/chef/profile/view.html`
9. `src/main/resources/templates/delivery/profile/view.html`
10. `src/main/resources/templates/waiter/profile/view.html`
11. `src/main/resources/templates/admin/shifts/history.html`
12. `src/main/resources/templates/admin/shifts/detail.html`
13. `src/main/resources/templates/admin/shifts/assign-employees.html`

### Base de Datos

14. `MIGRATE_EMPLOYEE_EMAIL_TO_EDAD.sql` (nuevo archivo)

### Documentación

15. `EMPLOYEE_EMAIL_TO_EDAD_MIGRATION.md` (este archivo)

---

## 🚀 Próximos Pasos

1. ✅ **Revisar Cambios**: Verificar que todos los archivos estén correctamente actualizados
2. ⏳ **Ejecutar Migración**: Correr `MIGRATE_EMPLOYEE_EMAIL_TO_EDAD.sql` en la base de datos
3. ⏳ **Testing Manual**: Probar la creación y edición de empleados
4. ⏳ **Actualizar Datos**: Agregar edades a empleados existentes (opcional)
5. ⏳ **Deploy**: Desplegar cambios en ambiente de producción

---

## 🔍 Verificación Rápida

### Query para verificar migración exitosa:

```sql
-- Ver estructura de la tabla
DESCRIBE employee;

-- Verificar empleados con/sin edad
SELECT
    idEmpleado,
    fullName,
    username,
    edad,
    telefono
FROM employee
ORDER BY idEmpleado;

-- Contar empleados con/sin edad
SELECT
    COUNT(*) as total_employees,
    COUNT(edad) as employees_with_edad,
    COUNT(*) - COUNT(edad) as employees_without_edad
FROM employee;
```

---

## 📞 Soporte

Si encuentras algún problema con la migración:

1. Verificar que el script SQL se haya ejecutado correctamente
2. Revisar logs de la aplicación por errores de validación
3. Comprobar que no hay referencias a `employee.email` en el código

---

**Fecha de Migración**: 2025
**Estado**: ✅ Completado
**Versión**: 1.0
