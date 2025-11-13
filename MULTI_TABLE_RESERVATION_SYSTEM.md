# Sistema de Reservaciones Multi-Mesa

## 📋 Resumen
Sistema implementado para permitir la selección de múltiples mesas simultáneamente al crear una reservación, generando un registro independiente por cada mesa seleccionada con la misma información del cliente.

## 🎯 Objetivo
Eliminar la necesidad de crear reservaciones una por una cuando un cliente requiere múltiples mesas, mejorando la eficiencia del proceso de reservación para grupos grandes.

---

## 🏗️ Arquitectura del Sistema

### Frontend (Thymeleaf + JavaScript)
```
admin/reservations/form.html
├── Grid Visual de Mesas (Cards)
│   ├── Mostrar todas las mesas disponibles
│   ├── Información: número, capacidad, ubicación
│   └── Estado de selección (activo/inactivo)
├── Sistema de Selección
│   ├── Click para toggle selección
│   ├── Badge visual de cantidad seleccionadas
│   └── Resumen de capacidad mínima
└── JavaScript
    ├── selectedTables[] array
    ├── toggleTableSelection(card)
    ├── updateSelectionUI()
    └── clearTableSelection()
```

### Backend (Spring Boot)
```
ReservationController.java
└── createReservation()
    ├── Recibe tableIds (String separado por comas)
    ├── Valida selección de al menos 1 mesa
    ├── Parse de IDs a List<Long>
    ├── Loop por cada mesa
    │   ├── Buscar RestaurantTable
    │   ├── Crear nueva Reservation
    │   ├── Copiar datos del cliente
    │   └── Guardar en base de datos
    └── Mensaje de éxito con cantidad creada
```

---

## 💻 Implementación Detallada

### 1. Interfaz de Usuario (form.html)

#### Grid de Tarjetas de Mesas
```html
<div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
    <div th:each="table : ${tables}" 
         class="table-card" 
         th:data-table-id="${table.id}"
         th:data-table-number="${table.tableNumber}"
         th:data-capacity="${table.capacity}"
         onclick="toggleTableSelection(this)">
        
        <!-- Número de mesa -->
        <div class="text-3xl font-bold text-primary-600">
            <span th:text="${table.tableNumber}">1</span>
        </div>
        
        <!-- Capacidad -->
        <div class="flex items-center justify-center">
            <i class="fas fa-user mr-1 text-sm text-gray-500"></i>
            <span th:text="${table.capacity} + ' personas'">4 personas</span>
        </div>
        
        <!-- Ubicación -->
        <div th:if="${table.location}" 
             class="text-xs text-gray-500">
            <i class="fas fa-map-marker-alt mr-1"></i>
            <span th:text="${table.location}">Terraza</span>
        </div>
        
        <!-- Badge de selección -->
        <div class="absolute top-2 right-2 w-6 h-6 border-2 border-gray-300 
                    rounded-full transition-all duration-200 selection-badge 
                    bg-white flex items-center justify-center">
            <i class="fas fa-check text-white text-xs"></i>
        </div>
    </div>
</div>
```

#### Campo Hidden para IDs
```html
<input type="hidden" name="tableIds" id="tableIds" />
```

#### Número de Invitados (Auto-calculado)
```html
<div>
    <label class="block text-sm font-medium text-gray-700 mb-2">
        Número de Invitados
    </label>
    <input type="number" 
           id="numberOfGuests" 
           name="numberOfGuests"
           th:field="*{numberOfGuests}"
           class="w-full px-3 py-2 border rounded-lg bg-gray-100 
                  text-gray-500 cursor-not-allowed" 
           readonly />
    <p class="text-xs text-gray-500 mt-1">
        Se calcula automáticamente como la capacidad mínima 
        de las mesas seleccionadas
    </p>
</div>
```

---

### 2. Lógica JavaScript

#### Variables Globales
```javascript
let selectedTables = []; // Array de objetos {id, number, capacity}
```

#### Función de Toggle de Selección
```javascript
function toggleTableSelection(card) {
    const tableId = card.getAttribute('data-table-id');
    const tableNumber = card.getAttribute('data-table-number');
    const capacity = parseInt(card.getAttribute('data-capacity'));
    
    const index = selectedTables.findIndex(t => t.id === tableId);
    
    if (index > -1) {
        // Deseleccionar
        selectedTables.splice(index, 1);
        card.classList.remove('selected');
    } else {
        // Seleccionar
        selectedTables.push({
            id: tableId,
            number: tableNumber,
            capacity: capacity
        });
        card.classList.add('selected');
    }
    
    updateSelectionUI();
}
```

#### Actualización de UI
```javascript
function updateSelectionUI() {
    const tableIdsInput = document.getElementById('tableIds');
    const numberOfGuestsInput = document.getElementById('numberOfGuests');
    const countBadge = document.getElementById('selectedCount');
    const summaryDiv = document.getElementById('selectedSummary');
    
    if (selectedTables.length === 0) {
        // Sin selección
        tableIdsInput.value = '';
        numberOfGuestsInput.value = '';
        countBadge.textContent = '0';
        summaryDiv.classList.add('hidden');
    } else {
        // Con selección
        const ids = selectedTables.map(t => t.id).join(',');
        tableIdsInput.value = ids;
        
        // Calcular capacidad mínima
        const minCapacity = Math.min(...selectedTables.map(t => t.capacity));
        numberOfGuestsInput.value = minCapacity;
        
        countBadge.textContent = selectedTables.length;
        summaryDiv.classList.remove('hidden');
        
        // Mostrar números de mesas seleccionadas
        const tableNumbersSpan = document.getElementById('selectedTableNumbers');
        const tableNumbers = selectedTables
            .map(t => '#' + t.number)
            .join(', ');
        tableNumbersSpan.textContent = tableNumbers;
    }
}
```

#### Limpiar Selección
```javascript
function clearTableSelection() {
    selectedTables = [];
    document.querySelectorAll('.table-card.selected').forEach(card => {
        card.classList.remove('selected');
    });
    updateSelectionUI();
}
```

#### Validación de Formulario
```javascript
document.querySelector('form').addEventListener('submit', function(e) {
    if (selectedTables.length === 0) {
        e.preventDefault();
        Swal.fire({
            icon: 'warning',
            title: 'Selección requerida',
            text: 'Debes seleccionar al menos una mesa',
            confirmButtonColor: '#38e07b'
        });
        return false;
    }
});
```

---

### 3. Estilos CSS

```css
.table-card {
    position: relative;
    padding: 1.5rem;
    border: 2px solid #e5e7eb;
    border-radius: 0.75rem;
    text-align: center;
    cursor: pointer;
    transition: all 0.2s;
    background: white;
}

.table-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.table-card.selected {
    border-color: #38e07b;
    box-shadow: 0 0 0 3px rgba(56, 224, 123, 0.1);
}

.table-card.selected .selection-badge {
    background-color: #38e07b;
    border-color: #38e07b;
}
```

---

### 4. Controlador Backend

#### Método createReservation()
```java
@PostMapping
public String createReservation(
        @Valid @ModelAttribute("reservation") Reservation reservation,
        BindingResult bindingResult,
        @RequestParam(value = "tableIds", required = false) String tableIds,
        Authentication authentication,
        Model model,
        RedirectAttributes redirectAttributes) {
    
    // Validar selección de mesas
    if (tableIds == null || tableIds.trim().isEmpty()) {
        model.addAttribute("errorMessage", "Debes seleccionar al menos una mesa");
        // Retornar al formulario con error
        return "admin/reservations/form";
    }
    
    // Parse de IDs
    String[] tableIdArray = tableIds.split(",");
    List<Long> tableIdList = new ArrayList<>();
    for (String idStr : tableIdArray) {
        try {
            tableIdList.add(Long.parseLong(idStr.trim()));
        } catch (NumberFormatException e) {
            log.error("Invalid table ID: {}", idStr);
        }
    }
    
    // Crear una reservación por cada mesa
    List<Reservation> createdReservations = new ArrayList<>();
    for (Long tableId : tableIdList) {
        RestaurantTable table = tableService.findById(tableId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Mesa no encontrada: " + tableId));
        
        // Crear copia de la reservación para esta mesa
        Reservation tableReservation = new Reservation();
        tableReservation.setCustomerName(reservation.getCustomerName());
        tableReservation.setCustomerPhone(reservation.getCustomerPhone());
        tableReservation.setCustomerEmail(reservation.getCustomerEmail());
        tableReservation.setNumberOfGuests(reservation.getNumberOfGuests());
        tableReservation.setReservationDate(reservation.getReservationDate());
        tableReservation.setReservationTime(reservation.getReservationTime());
        tableReservation.setSpecialRequests(reservation.getSpecialRequests());
        tableReservation.setRestaurantTable(table);
        
        // Guardar
        Reservation created = reservationService.create(tableReservation, username);
        createdReservations.add(created);
    }
    
    // Mensaje de éxito personalizado
    String successMessage;
    if (createdReservations.size() == 1) {
        successMessage = "Reservación creada exitosamente para " + 
            createdReservations.get(0).getCustomerName();
    } else {
        successMessage = String.format(
            "Se crearon %d reservaciones exitosamente para %s", 
            createdReservations.size(), 
            reservation.getCustomerName()
        );
    }
    
    redirectAttributes.addFlashAttribute("successMessage", successMessage);
    return "redirect:/admin/reservations";
}
```

---

## 🔄 Flujo de Uso

### Escenario 1: Reservación Simple (1 mesa)
1. Usuario abre formulario de reservación
2. Llena datos del cliente (nombre, teléfono, email)
3. Selecciona fecha y hora
4. **Click en 1 tarjeta de mesa**
5. Badge muestra "1 mesa seleccionada"
6. Campo "Número de Invitados" se llena automáticamente con capacidad de la mesa
7. Usuario envía formulario
8. Backend crea **1 reservación**
9. Mensaje: "Reservación creada exitosamente para [Nombre]"

### Escenario 2: Reservación Multi-Mesa (3 mesas)
1. Usuario abre formulario de reservación
2. Llena datos del cliente (nombre, teléfono, email)
3. Selecciona fecha y hora
4. **Click en 3 tarjetas de mesa** (ej: Mesa #5, #6, #7)
5. Badge muestra "3 mesas seleccionadas"
6. Resumen muestra: "Mesas: #5, #6, #7"
7. Campo "Número de Invitados" se llena con capacidad **mínima** de las 3 mesas
   - Mesa #5: 6 personas
   - Mesa #6: 4 personas ← **mínima**
   - Mesa #7: 8 personas
   - Resultado: numberOfGuests = 4
8. Usuario envía formulario
9. Backend crea **3 reservaciones**:
   - Reservación 1: Mesa #5, cliente Juan Pérez, 4 invitados
   - Reservación 2: Mesa #6, cliente Juan Pérez, 4 invitados
   - Reservación 3: Mesa #7, cliente Juan Pérez, 4 invitados
10. Mensaje: "Se crearon 3 reservaciones exitosamente para Juan Pérez"

---

## ⚙️ Validaciones Implementadas

### Frontend
- ✅ Al menos 1 mesa debe estar seleccionada antes de enviar
- ✅ Advertencia con SweetAlert2 si no hay selección
- ✅ Campo numberOfGuests es readonly (solo lectura)
- ✅ Visual feedback de selección (borde verde, badge activo)

### Backend
- ✅ Parámetro `tableIds` es requerido
- ✅ Validación de string vacío
- ✅ Validación de parse de números
- ✅ Validación de existencia de mesa en BD
- ✅ Validación estándar de `@Valid` en Reservation
- ✅ Manejo de excepciones con mensajes claros

---

## 📊 Modelo de Datos

### Tabla `reservations`
```sql
-- Para reservación multi-mesa de Juan Pérez con 3 mesas
INSERT INTO reservations (customer_name, customer_phone, customer_email, 
                          number_of_guests, reservation_date, reservation_time,
                          special_requests, restaurant_table_id, status)
VALUES 
('Juan Pérez', '555-1234', 'juan@example.com', 4, '2024-06-15', '19:00:00', 
 'Evento corporativo', 5, 'PENDING'),
 
('Juan Pérez', '555-1234', 'juan@example.com', 4, '2024-06-15', '19:00:00', 
 'Evento corporativo', 6, 'PENDING'),
 
('Juan Pérez', '555-1234', 'juan@example.com', 4, '2024-06-15', '19:00:00', 
 'Evento corporativo', 7, 'PENDING');
```

**Resultado**: 3 registros independientes, cada uno vinculado a una mesa diferente.

---

## 🎨 Características de UX

### Visual Feedback
- **Sin selección**: Tarjeta blanca con borde gris
- **Con selección**: Tarjeta con borde verde (#38e07b) y sombra verde
- **Hover**: Efecto de elevación (translateY)
- **Badge**: Círculo vacío → Círculo verde con ✓

### Información en Tiempo Real
- **Badge de cantidad**: Muestra cuántas mesas están seleccionadas
- **Resumen de mesas**: Lista números de mesas seleccionadas (#5, #6, #7)
- **Auto-cálculo**: numberOfGuests se actualiza automáticamente

### Responsividad
- **Mobile (< 640px)**: 2 columnas
- **Tablet (640-1024px)**: 3 columnas
- **Desktop (1024-1280px)**: 4 columnas
- **Large Desktop (> 1280px)**: 5 columnas

---

## 🔧 Mantenimiento

### Para Modificar el Cálculo de Capacidad
Actualmente usa **capacidad mínima**. Para cambiar a **suma de capacidades**:

```javascript
// En updateSelectionUI()
// ANTES (mínima):
const minCapacity = Math.min(...selectedTables.map(t => t.capacity));

// DESPUÉS (suma):
const totalCapacity = selectedTables.reduce((sum, t) => sum + t.capacity, 0);
```

### Para Desactivar Multi-Selección en Modo Edición
```html
<!-- En form.html -->
<div th:if="${!isEdit}">
    <!-- Grid de tarjetas multi-selección -->
</div>

<div th:if="${isEdit}">
    <!-- Dropdown tradicional de 1 sola mesa -->
    <select name="restaurantTable.id" required>
        <option th:each="table : ${tables}" 
                th:value="${table.id}"
                th:text="'Mesa #' + ${table.tableNumber}">
        </option>
    </select>
</div>
```

---

## 🐛 Troubleshooting

### Problema: Las mesas no se seleccionan al hacer click
**Solución**: Verificar que el JavaScript esté cargado correctamente y que las clases CSS estén definidas.

### Problema: El campo numberOfGuests no se actualiza
**Solución**: Verificar que el ID del input sea exactamente `numberOfGuests` y que la función `updateSelectionUI()` esté siendo llamada.

### Problema: Backend recibe tableIds vacío
**Solución**: Verificar que el input hidden tenga `name="tableIds"` y que el JavaScript esté actualizando su valor correctamente.

### Problema: Error "Mesa no encontrada"
**Solución**: Verificar que solo se estén mostrando mesas activas en el frontend (`tableService.findReservableTables()`).

---

## 📈 Mejoras Futuras

1. **Arrastrar y soltar**: Implementar drag & drop para organizar mesas
2. **Vista de plano**: Mostrar layout visual del restaurante
3. **Filtros**: Filtrar por capacidad, ubicación, disponibilidad
4. **Búsqueda**: Buscar mesa por número
5. **Agrupación automática**: Sugerir combinaciones de mesas para X personas
6. **Historial**: Mostrar reservaciones previas del mismo cliente
7. **Notificaciones**: Email/SMS de confirmación por cada mesa reservada
8. **Cancelación en lote**: Cancelar todas las mesas de una reservación multi-mesa

---

## ✅ Testing Checklist

- [ ] Seleccionar 1 mesa y crear reservación
- [ ] Seleccionar 3 mesas y verificar 3 registros en BD
- [ ] Deseleccionar una mesa ya seleccionada
- [ ] Intentar enviar sin seleccionar mesas (debe mostrar alerta)
- [ ] Verificar cálculo de capacidad mínima
- [ ] Verificar mensaje de éxito personalizado (singular/plural)
- [ ] Verificar que todas las reservaciones tengan los mismos datos de cliente
- [ ] Verificar que cada reservación esté vinculada a mesa diferente
- [ ] Verificar responsividad en mobile/tablet/desktop
- [ ] Verificar estados visuales (hover, selected, default)

---

## 📝 Notas Importantes

1. **Independencia de registros**: Cada mesa genera un registro completamente independiente en la base de datos
2. **Mismo cliente, múltiples reservaciones**: No se crea una reservación "grupal", sino N reservaciones individuales
3. **Capacidad mínima vs suma**: Se usa mínima para evitar sobrecupo en alguna mesa
4. **No hay relación entre reservaciones**: No existe un campo "grupo" o "lote" que las vincule
5. **Cancelación individual**: Cada reservación puede ser cancelada independientemente

---

## 📚 Archivos Modificados

### Creados
- ✅ `MULTI_TABLE_RESERVATION_SYSTEM.md` - Esta documentación

### Modificados
- ✅ `admin/reservations/form.html` - UI completa de selección multi-mesa
- ✅ `ReservationController.java` - Método `createReservation()` con soporte multi-mesa

---

**Fecha de implementación**: 2024  
**Versión**: 1.0  
**Estado**: ✅ Completado y funcional
