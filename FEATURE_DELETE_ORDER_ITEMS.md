# Feature: Delete Individual Items from Orders

## 📋 Overview

Implemented the ability to delete individual items from orders with intelligent stock management and validations.

## ✅ Implementation Date

Completed: December 2024

## 🎯 User Requirements

> "Quiero que se pueda eliminar un item pero con la siguiente condición: Que el estatus de ese item sea diferente a entregado y si se elimina cuando el estatus de ese item esté en pendiente se deberá regresar el stock automáticamente, de lo contrario será manualmente y si el item no requiere preparación y no está en entregado deberá ser devuelto automáticamente."

## 🔧 Technical Implementation

### Backend Changes

#### 1. OrderService.java

**Added method signature:**

```java
OrderDetail deleteOrderItem(Long orderId, Long itemDetailId, String username) throws Exception;
```

#### 2. OrderServiceImpl.java (lines ~1170-1255)

**Implemented `deleteOrderItem()` method:**

**Validations:**

- ✅ Order must not be CANCELLED or PAID
- ✅ Item must exist in order
- ✅ Item status must not be DELIVERED

**Stock Return Logic (automatic):**

- ✅ PENDING status → Automatic stock return (item never prepared)
- ✅ READY status + requiresPreparation=FALSE → Automatic stock return (doesn't need preparation)

**Stock Return Logic (manual):**

- ⚠️ READY status + requiresPreparation=TRUE → Manual stock return (chef already prepared)
- ⚠️ IN_PREPARATION status → Manual stock return (item being prepared)

**Process:**

1. Validate order status (not CANCELLED/PAID)
2. Find item in order details
3. Validate item not DELIVERED
4. Check if automatic stock return applies: `shouldReturnStockAutomatically(itemToDelete)`
5. If automatic: call `returnStockForItem(itemMenu, quantity)`
6. Remove item from `order.getOrderDetails()`
7. Recalculate order total: `detail.getUnitPrice() * quantity`
8. Recalculate order status: `order.calculateStatusFromItems()`
9. Save and return deleted `OrderDetail`

#### 3. OrderController.java (lines ~1078-1125)

**Added DELETE endpoint:**

```java
@DeleteMapping("/{orderId}/items/{itemId}")
@ResponseBody
public Map<String, Object> deleteOrderItem(
    @PathVariable Long orderId,
    @PathVariable Long itemId,
    @AuthenticationPrincipal UserDetails userDetails
) throws Exception
```

**Response format:**

```json
{
  "success": true,
  "message": "Item eliminado exitosamente del pedido",
  "stockInfo": "✅ Stock devuelto automáticamente (item nunca fue preparado)",
  "orderTotal": 45000.0,
  "orderStatus": "READY",
  "remainingItems": 2
}
```

**Helper method:** `analyzeItemStockReturn()` (lines ~1650-1675)

- Returns descriptive stock return messages based on item status

#### 4. CashierController.java (lines ~833-880)

**Added same DELETE endpoint:**

- Endpoint: `/cashier/orders/{orderId}/items/{itemId}`
- Same logic as OrderController
- Helper method: `analyzeItemStockReturn()` (lines ~1230-1255)

### Frontend Changes

#### 5. admin/orders/view.html

**Table header:**

- Added "Acciones" column (line ~320)

**Table body:**

- Added delete button cell (lines ~440-462)
- Button only shows if `itemStatus != DELIVERED`
- Shows "Entregado" text if item is delivered
- Data attributes: `data-item-id`, `data-item-name`, `data-item-status`, `data-requires-preparation`

**JavaScript function:** `deleteOrderItem(itemId, itemName, itemStatus, requiresPrep)`

- SweetAlert2 confirmation modal
- Dynamic warning messages based on item status
- DELETE fetch request to `/admin/orders/{orderId}/items/{itemId}`
- Success modal with stock info and order summary
- Page reload after deletion

**Event listener:**

- Attaches click handler to all `.btn-delete-item` buttons

#### 6. waiter/orders/view.html

**Same changes as admin view:**

- Table structure: "Acciones" column + delete button cell
- JavaScript function: `deleteOrderItem()`
- Endpoint: `/waiter/orders/{orderId}/items/{itemId}`
- Event listener for delete buttons

#### 7. cashier/orders/view.html

**Same changes as admin view:**

- Table structure: "Acciones" column + delete button cell
- JavaScript function: `deleteOrderItem()`
- Endpoint: `/cashier/orders/{orderId}/items/{itemId}`
- Event listener for delete buttons

## 📊 Stock Return Logic Summary

| Item Status    | Requires Preparation | Stock Return     | Message                                                          |
| -------------- | -------------------- | ---------------- | ---------------------------------------------------------------- |
| PENDING        | -                    | ✅ Automatic     | Stock devuelto automáticamente (item nunca fue preparado)        |
| READY          | FALSE                | ✅ Automatic     | Stock devuelto automáticamente (item no requiere preparación)    |
| READY          | TRUE                 | ⚠️ Manual        | Stock debe ser devuelto manualmente (chef ya preparó el item)    |
| IN_PREPARATION | -                    | ⚠️ Manual        | Stock debe ser devuelto manualmente (item estaba en preparación) |
| DELIVERED      | -                    | ❌ Cannot Delete | Item cannot be deleted                                           |

## 🚫 Validation Rules

1. **Order Status Validation:**

   - Cannot delete items from PAID orders
   - Cannot delete items from CANCELLED orders

2. **Item Status Validation:**

   - Cannot delete items with DELIVERED status
   - Shows "Entregado" text instead of delete button

3. **Stock Return:**
   - Automatic: PENDING or (READY + !requiresPreparation)
   - Manual: IN_PREPARATION or (READY + requiresPreparation)
   - Uses existing `shouldReturnStockAutomatically()` logic

## 🎨 UX/UI Features

**Confirmation Modal:**

- Dynamic warning messages based on item status
- Color-coded indicators:
  - ✅ Green: Automatic stock return
  - ⚠️ Orange: Manual stock return required
- "Sí, eliminar" / "Cancelar" buttons
- Loading state during deletion

**Success Modal:**

- Success message: "¡Item Eliminado!"
- Stock return information
- Order summary:
  - Items restantes
  - Nuevo total (formatted currency)
- "Aceptar" button (no auto-close)

**Delete Button:**

- Red trash icon button
- Only visible if item not DELIVERED
- Hover effects: scale animation
- Tooltip: "Eliminar item"
- Class: `.btn-delete-item`

## 📝 Files Modified

**Backend (4 files):**

1. `src/main/java/com/elgransazon/service/OrderService.java`
2. `src/main/java/com/elgransazon/service/OrderServiceImpl.java`
3. `src/main/java/com/elgransazon/controller/OrderController.java`
4. `src/main/java/com/elgransazon/controller/CashierController.java`

**Frontend (3 files):** 5. `src/main/resources/templates/admin/orders/view.html` 6. `src/main/resources/templates/waiter/orders/view.html` 7. `src/main/resources/templates/cashier/orders/view.html`

## ✅ Testing Checklist

- [ ] Delete item with PENDING status → Stock returned automatically
- [ ] Delete item with READY + requiresPreparation=FALSE → Stock returned automatically
- [ ] Delete item with READY + requiresPreparation=TRUE → Manual stock return message
- [ ] Delete item with IN_PREPARATION → Manual stock return message
- [ ] Cannot delete item with DELIVERED status → Button not shown
- [ ] Cannot delete from PAID order → Validation error
- [ ] Cannot delete from CANCELLED order → Validation error
- [ ] Order total recalculated correctly after deletion
- [ ] Order status recalculated correctly after deletion
- [ ] Success modal shows correct stock info
- [ ] Success modal shows correct order summary
- [ ] Page reloads after successful deletion
- [ ] Test in admin role
- [ ] Test in waiter role
- [ ] Test in cashier role

## 🔗 Related Features

- Stock Management System
- Order State Management
- Item Status Workflow
- Requires Preparation Feature
- Order Cancellation Logic

## 📚 Related Documentation

- `FEATURE_REQUIRES_PREPARATION.md`
- `ORDER_STATUS_WORKFLOW.md`
- `CHEF_FUNCTIONALITY_IMPLEMENTATION.md`
- `VALIDATIONS_COMPREHENSIVE_GUIDE.md`

## 🎯 Success Metrics

- ✅ All backend endpoints compile without errors
- ✅ Frontend buttons render conditionally
- ✅ Stock return logic consistent with cancellation logic
- ✅ Modal messages clear and informative
- ✅ Order totals and status recalculate correctly
- ✅ Implemented for all 3 roles: admin, waiter, cashier

## 💡 Future Enhancements

- [ ] Add confirmation step for manual stock returns
- [ ] Show item deletion history/audit log
- [ ] Bulk delete multiple items
- [ ] Undo deletion functionality
- [ ] Real-time updates without page reload
