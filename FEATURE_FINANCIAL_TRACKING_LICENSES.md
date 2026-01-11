# Sistema de Renovación y Seguimiento Financiero de Licencias

## Funcionalidades Implementadas

### 1. Renovación/Ajuste de Licencias con Monto

El sistema ahora permite:

- **Agregar tiempo (renovación)**: +1, +3, +6, +12 meses
- **Restar tiempo (cancelación/corrección)**: -1, -3, -6, -12 meses
- **Registrar monto**: Campo opcional para registrar el precio de cada renovación

#### Modal de Renovación
- Selector con opciones positivas (renovación) y negativas (cancelación)
- Campo de monto en MXN con validación
- Vista previa de la nueva fecha de vencimiento
- Cálculo automático de días totales de vigencia

### 2. Seguimiento Financiero

#### Tabla de Historial de Renovaciones
Muestra todas las renovaciones/ajustes con:
- Fecha y hora del evento
- Descripción detallada
- Meses agregados/restados (con badges de color)
- Monto en MXN

#### Resumen de Ingresos
- **Total Generado**: Suma de todos los montos de renovaciones
- Mostrado prominentemente en el dashboard
- Solo cuenta renovaciones con monto registrado (excluye correcciones sin costo)

### 3. Cambios en la Base de Datos

#### Nueva Estructura de `license_events`
```sql
ALTER TABLE license_events ADD COLUMN:
- amount DECIMAL(10,2) - Monto de la renovación
- months INT - Meses agregados (positivo) o restados (negativo)
```

### 4. Casos de Uso

#### Caso 1: Renovación Pagada
- Cliente paga $1,500 por 12 meses
- Seleccionar: "+12 meses"
- Ingresar: 1500.00
- Resultado: Se extiende la licencia y se registra el ingreso

#### Caso 2: Corrección sin Costo
- Te equivocaste y agregaste 6 meses de más
- Seleccionar: "-6 meses"
- Dejar monto en 0 o vacío
- Resultado: Se resta el tiempo sin afectar el total de ingresos

#### Caso 3: Cancelación con Reembolso
- Cliente cancela y pide reembolso
- Seleccionar: "-12 meses"
- Ingresar monto negativo o dejarlo en 0
- Resultado: Se resta el tiempo y se registra en el historial

### 5. Lógica del Backend

#### LicenseService.renewLicense()
```java
- Acepta meses positivos o negativos
- Acepta monto opcional (Double, puede ser null)
- Usa plusMonths() que funciona con valores negativos
- Registra el evento con toda la información
```

#### Cálculo de Ingresos
```java
getTotalRevenue():
- Suma solo eventos de tipo RENEWED
- Solo cuenta eventos con amount != null y amount > 0
- Ignora correcciones sin monto
```

### 6. Seguridad y Validación

- Solo el programador puede acceder al dashboard
- Validación de meses requerido
- Monto opcional (no requerido)
- Los meses negativos no pueden dejar la fecha antes de hoy (validar si es necesario)

## Migración

### Paso 1: Ejecutar Script SQL
```bash
mysql -u root -p elgransazon < ADD_FINANCIAL_TRACKING_TO_LICENSE_EVENTS.sql
```

### Paso 2: Reiniciar Aplicación
Los cambios en las entidades requieren reinicio.

### Paso 3: Verificar
1. Ir al dashboard del programador
2. Ver que aparezca la sección "Resumen Financiero"
3. Hacer una renovación de prueba con monto
4. Verificar que aparezca en el historial y sume al total

## Beneficios

✅ **Control Total**: Puedes agregar o restar tiempo según necesites
✅ **Seguimiento Financiero**: Sabes exactamente cuánto has ganado con cada cliente
✅ **Historial Completo**: Registro detallado de todas las renovaciones y ajustes
✅ **Flexibilidad**: Correcciones sin afectar los ingresos registrados
✅ **Transparencia**: Cliente puede ver historial de su licencia

## Próximas Mejoras Sugeridas

- [ ] Reportes mensuales/anuales de ingresos
- [ ] Gráficas de ingresos por período
- [ ] Exportar historial a PDF/Excel
- [ ] Recordatorios automáticos de renovación
- [ ] Sistema de descuentos por renovaciones anticipadas
- [ ] Facturación automática integrada
