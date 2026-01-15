# Sistema de Direcciones con Mapa Interactivo

## Descripción

Se implementó un sistema de direcciones de entrega con mapa interactivo que permite a los clientes:

1. **Guardar múltiples direcciones** - Casa, Trabajo, etc.
2. **Marcar ubicación en el mapa** - Click o arrastrar marcador
3. **Usar geolocalización** - "Usar mi ubicación actual"
4. **Seleccionar dirección predeterminada** - Para pedidos rápidos
5. **Geocodificación inversa** - Obtiene la dirección automáticamente del mapa

## Archivos Creados

### Backend

1. **`CustomerAddress.java`** - Entidad JPA

   - Almacena: label, address, reference, latitude, longitude
   - Soporta múltiples direcciones por cliente
   - Campo `isDefault` para dirección predeterminada
   - Soft delete con campo `active`

2. **`CustomerAddressRepository.java`** - Repositorio JPA

   - Consultas optimizadas por cliente
   - Métodos para establecer predeterminada
   - Soft delete

3. **`CustomerAddressService.java`** - Servicio de negocio
   - CRUD completo de direcciones
   - Manejo automático de dirección predeterminada

### Frontend

4. **`profile.html`** - Vista de perfil actualizada

   - Sección de direcciones con lista de tarjetas
   - Mapa interactivo (Leaflet + OpenStreetMap)
   - Modal para agregar/editar direcciones
   - Botón "Usar mi ubicación actual"

5. **`menu.html`** - Menú del cliente actualizado
   - Selector de dirección de entrega
   - Validación de dirección antes de ordenar

### Base de Datos

6. **`CREATE_CUSTOMER_ADDRESSES_TABLE.sql`**
   - Crea tabla `customer_addresses`
   - Migra direcciones existentes de la tabla `customers`

## Endpoints REST Agregados

| Método | Endpoint                             | Descripción                      |
| ------ | ------------------------------------ | -------------------------------- |
| GET    | `/client/addresses`                  | Lista direcciones del cliente    |
| POST   | `/client/addresses`                  | Crear nueva dirección            |
| PUT    | `/client/addresses/{id}`             | Actualizar dirección             |
| DELETE | `/client/addresses/{id}`             | Eliminar dirección (soft delete) |
| POST   | `/client/addresses/{id}/set-default` | Establecer como predeterminada   |

## Tecnologías Utilizadas

- **Leaflet.js** - Biblioteca de mapas interactivos (gratuita, sin API key)
- **OpenStreetMap** - Tiles de mapas gratuitos
- **Nominatim** - Geocodificación inversa gratuita

## Instalación

1. Ejecutar el script SQL:

```sql
-- En tu cliente MySQL/MariaDB
source CREATE_CUSTOMER_ADDRESSES_TABLE.sql
```

2. Reiniciar la aplicación Spring Boot

## Uso

### Para el Cliente

1. Ir a **Mi Perfil**
2. En la sección **"Mis Direcciones de Entrega"**:

   - Click en **"Nueva Dirección"**
   - Escribir nombre (Casa, Trabajo, etc.)
   - Marcar ubicación en el mapa (click o arrastrar)
   - Opcionalmente usar **"Usar mi ubicación actual"**
   - Agregar referencia (opcional)
   - Guardar

3. Al hacer un pedido **DELIVERY**:
   - Seleccionar la dirección del dropdown
   - La referencia se incluye automáticamente
   - Se puede agregar notas adicionales

### Características del Mapa

- **Click en el mapa** → Mueve el marcador
- **Arrastrar marcador** → Ajusta ubicación precisa
- **Geocodificación automática** → Sugiere dirección del punto
- **Zoom** → Rueda del mouse o botones +/-

## Compatibilidad

- Mantiene compatibilidad con el campo `address` de la tabla `customers`
- Si no hay direcciones nuevas, usa la dirección legacy
- La migración SQL crea entradas para direcciones existentes

## Notas Técnicas

- Las coordenadas se almacenan con 8 decimales (precisión ~1mm)
- El mapa por defecto muestra Ciudad de México (configurable)
- Geocodificación usa Nominatim (límite: 1 request/segundo)
- Los mapas funcionan sin conexión después de cargar los tiles
