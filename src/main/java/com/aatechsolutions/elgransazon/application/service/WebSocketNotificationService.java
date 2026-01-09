package com.aatechsolutions.elgransazon.application.service;

import com.aatechsolutions.elgransazon.domain.entity.Order;
import com.aatechsolutions.elgransazon.presentation.dto.KitchenStatsDTO;
import com.aatechsolutions.elgransazon.presentation.dto.OrderNotificationDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Service for sending real-time WebSocket notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notifies about a new order
     * Only notifies the roles (chef/barista) that have items to prepare
     */
    public void notifyNewOrder(Order order) {
        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            log.warn("notifyNewOrder called with order without items: {}", order.getOrderNumber());
            return;
        }
        
        // Detect what type of items the order has
        boolean hasChefItems = order.getOrderDetails().stream()
            .anyMatch(detail -> detail.getItemMenu() != null && 
                Boolean.TRUE.equals(detail.getItemMenu().getRequiresPreparation()));
        
        boolean hasBaristaItems = order.getOrderDetails().stream()
            .anyMatch(detail -> detail.getItemMenu() != null && 
                Boolean.TRUE.equals(detail.getItemMenu().getRequiresBaristaPreparation()));
        
        OrderNotificationDTO notification = buildOrderNotification(order, "NEW_ORDER", 
            "Nuevo pedido #" + order.getOrderNumber());
        
        // Only notify CHEF if order has chef items
        if (hasChefItems) {
            messagingTemplate.convertAndSend("/topic/chef/orders", notification);
            log.info("👨‍🍳 WebSocket: Notifying CHEF - New order {} with chef items", order.getOrderNumber());
        }
        
        // Only notify BARISTA if order has barista items
        if (hasBaristaItems) {
            messagingTemplate.convertAndSend("/topic/barista/orders", notification);
            log.info("☕ WebSocket: Notifying BARISTA - New order {} with barista items", order.getOrderNumber());
        }
        
        // Always send to admin kitchen view
        messagingTemplate.convertAndSend("/topic/admin/kitchen", notification);
        
        log.info("🔔 WebSocket: New order notification sent - {} - Chef: {}, Barista: {}", 
            order.getOrderNumber(), hasChefItems, hasBaristaItems);
    }

    /**
     * Notifies about order status change
     * Sends to both chef and barista channels so all roles can update their views
     */
    public void notifyOrderStatusChange(Order order, String message) {
        notifyOrderStatusChange(order, message, null);
    }

    /**
     * Notifies about order status change
     * Only notifies the role that made the change (chef or barista)
     * 
     * @param order The order that changed status
     * @param message The status change message
     * @param roleWhoChanged The role that triggered the change ("chef", "barista", or null for all)
     */
    public void notifyOrderStatusChange(Order order, String message, String roleWhoChanged) {
        OrderNotificationDTO notification = buildOrderNotification(order, "STATUS_CHANGE", message);
        
        // If roleWhoChanged is specified, only notify that role
        // Otherwise, notify all roles that have items in the order
        if (roleWhoChanged != null) {
            // Only notify the role that made the change
            if ("chef".equalsIgnoreCase(roleWhoChanged)) {
                messagingTemplate.convertAndSend("/topic/chef/orders", notification);
                log.debug("👨‍🍳 WebSocket: Notifying CHEF ONLY - Order {} status changed by chef", order.getOrderNumber());
                
                // Send personal notification to assigned chef
                if (order.getPreparedBy() != null) {
                    messagingTemplate.convertAndSendToUser(
                        order.getPreparedBy().getUsername(),
                        "/queue/orders",
                        notification
                    );
                }
            } else if ("barista".equalsIgnoreCase(roleWhoChanged)) {
                messagingTemplate.convertAndSend("/topic/barista/orders", notification);
                log.debug("☕ WebSocket: Notifying BARISTA ONLY - Order {} status changed by barista", order.getOrderNumber());
                
                // Send personal notification to assigned barista
                if (order.getPreparedByBarista() != null) {
                    messagingTemplate.convertAndSendToUser(
                        order.getPreparedByBarista().getUsername(),
                        "/queue/orders",
                        notification
                    );
                }
            }
        } else {
            // Original logic: notify all roles that have items
            boolean hasChefItems = order.getOrderDetails() != null && order.getOrderDetails().stream()
                .anyMatch(detail -> detail.getItemMenu() != null && 
                    Boolean.TRUE.equals(detail.getItemMenu().getRequiresPreparation()));
            
            boolean hasBaristaItems = order.getOrderDetails() != null && order.getOrderDetails().stream()
                .anyMatch(detail -> detail.getItemMenu() != null && 
                    Boolean.TRUE.equals(detail.getItemMenu().getRequiresBaristaPreparation()));
            
            // Notify chef if order has chef items
            if (hasChefItems) {
                messagingTemplate.convertAndSend("/topic/chef/orders", notification);
                log.debug("👨‍🍳 WebSocket: Notifying CHEF - Order {} status changed", order.getOrderNumber());
                
                if (order.getPreparedBy() != null) {
                    messagingTemplate.convertAndSendToUser(
                        order.getPreparedBy().getUsername(),
                        "/queue/orders",
                        notification
                    );
                }
            }
            
            // Notify barista if order has barista items
            if (hasBaristaItems) {
                messagingTemplate.convertAndSend("/topic/barista/orders", notification);
                log.debug("☕ WebSocket: Notifying BARISTA - Order {} status changed", order.getOrderNumber());
                
                if (order.getPreparedByBarista() != null) {
                    messagingTemplate.convertAndSendToUser(
                        order.getPreparedByBarista().getUsername(),
                        "/queue/orders",
                        notification
                    );
                }
            }
        }
        
        // Always send to admin kitchen
        messagingTemplate.convertAndSend("/topic/admin/kitchen", notification);
        
        log.debug("WebSocket: Order status change - {} - {}", order.getOrderNumber(), message);
    }

    /**
     * Notifies when items are added to an existing order
     * Only notifies the roles (chef/barista) that have items added for them
     * 
     * @param order The order with new items
     * @param newItems The list of new OrderDetails that were added
     */
    public void notifyItemsAdded(Order order, java.util.List<com.aatechsolutions.elgransazon.domain.entity.OrderDetail> newItems) {
        if (newItems == null || newItems.isEmpty()) {
            log.warn("notifyItemsAdded called with empty items list");
            return;
        }
        
        // Detect what type of items were added
        boolean hasChefItems = newItems.stream()
            .anyMatch(detail -> detail.getItemMenu() != null && 
                Boolean.TRUE.equals(detail.getItemMenu().getRequiresPreparation()));
        
        boolean hasBaristaItems = newItems.stream()
            .anyMatch(detail -> detail.getItemMenu() != null && 
                Boolean.TRUE.equals(detail.getItemMenu().getRequiresBaristaPreparation()));
        
        String message = String.format("Se agregaron %d item(s) al pedido %s", newItems.size(), order.getOrderNumber());
        OrderNotificationDTO notification = buildOrderNotification(order, "ITEMS_ADDED", message);
        
        // Only notify CHEF if chef items were added
        if (hasChefItems) {
            messagingTemplate.convertAndSend("/topic/chef/orders", notification);
            log.info("👨‍🍳 WebSocket: Notifying CHEF - {} chef items added to order {}", 
                newItems.stream().filter(d -> Boolean.TRUE.equals(d.getItemMenu().getRequiresPreparation())).count(),
                order.getOrderNumber());
        }
        
        // Only notify BARISTA if barista items were added
        if (hasBaristaItems) {
            messagingTemplate.convertAndSend("/topic/barista/orders", notification);
            log.info("☕ WebSocket: Notifying BARISTA - {} barista items added to order {}", 
                newItems.stream().filter(d -> Boolean.TRUE.equals(d.getItemMenu().getRequiresBaristaPreparation())).count(),
                order.getOrderNumber());
        }
        
        // Always send to admin kitchen
        messagingTemplate.convertAndSend("/topic/admin/kitchen", notification);
        
        // Send to all roles to update their views (order list updates)
        messagingTemplate.convertAndSend("/topic/orders", notification);
        
        // If chef assigned and chef items added, send personal notification
        if (hasChefItems && order.getPreparedBy() != null) {
            messagingTemplate.convertAndSendToUser(
                order.getPreparedBy().getUsername(),
                "/queue/orders",
                notification
            );
        }
        
        // If barista assigned and barista items added, send personal notification
        if (hasBaristaItems && order.getPreparedByBarista() != null) {
            messagingTemplate.convertAndSendToUser(
                order.getPreparedByBarista().getUsername(),
                "/queue/orders",
                notification
            );
        }
        
        log.info("🔔 WebSocket: Items added notification sent - Order {} - Chef items: {}, Barista items: {}", 
            order.getOrderNumber(), hasChefItems, hasBaristaItems);
    }

    /**
     * Notifies when a chef is assigned to an order
     */
    public void notifyChefAssigned(Order order, String chefName) {
        OrderNotificationDTO notification = buildOrderNotification(order, "CHEF_ASSIGNED",
            "Pedido asignado a " + chefName);
        notification.setChefName(chefName);
        
        // Send to all to update kitchen view
        messagingTemplate.convertAndSend("/topic/chef/orders", notification);
        messagingTemplate.convertAndSend("/topic/admin/kitchen", notification);
        
        log.info("WebSocket: Chef assignment - {} assigned to {}", 
            order.getOrderNumber(), chefName);
    }

    /**
     * Notifies when an order is accepted by a chef or barista
     * This will hide the order from other chefs/baristas who didn't accept it
     * 
     * @param order The order that was accepted
     * @param acceptedBy The username of who accepted it
     * @param role The role ("chef" or "barista")
     */
    public void notifyOrderAccepted(Order order, String acceptedBy, String role) {
        OrderNotificationDTO notification = buildOrderNotification(order, "ORDER_ACCEPTED",
            "Pedido #" + order.getOrderNumber() + " fue aceptado por " + acceptedBy);
        notification.setChefName(acceptedBy);
        
        // Send to the appropriate channel so OTHER chefs/baristas hide it
        if ("chef".equalsIgnoreCase(role)) {
            messagingTemplate.convertAndSend("/topic/chef/orders", notification);
            log.info("👨‍🍳 WebSocket: Notifying ALL CHEFS - Order {} accepted by chef {}", 
                order.getOrderNumber(), acceptedBy);
        } else if ("barista".equalsIgnoreCase(role)) {
            messagingTemplate.convertAndSend("/topic/barista/orders", notification);
            log.info("☕ WebSocket: Notifying ALL BARISTAS - Order {} accepted by barista {}", 
                order.getOrderNumber(), acceptedBy);
        }
        
        // Send to admin kitchen
        messagingTemplate.convertAndSend("/topic/admin/kitchen", notification);
        
        log.info("🔔 WebSocket: Order accepted notification sent - {} by {} ({})", 
            order.getOrderNumber(), acceptedBy, role);
    }

    /**
     * Updates kitchen statistics in real-time
     */
    public void updateKitchenStats(KitchenStatsDTO stats) {
        messagingTemplate.convertAndSend("/topic/kitchen/stats", stats);
        log.debug("WebSocket: Kitchen stats updated - pending={}, inPrep={}", 
            stats.getPendingCount(), stats.getInPreparationCount());
    }

    /**
     * Sends notification to administrators
     */
    public void notifyAdmins(String message, Object data) {
        AdminNotification notification = new AdminNotification(message, data);
        messagingTemplate.convertAndSend("/topic/admin/notifications", notification);
        log.info("WebSocket: Admin notification sent - {}", message);
    }

    /**
     * Notifies about order deletion
     */
    public void notifyOrderDeleted(Long orderId, String orderNumber) {
        OrderDeletionNotification notification = new OrderDeletionNotification(orderId, orderNumber);
        messagingTemplate.convertAndSend("/topic/chef/orders", notification);
        messagingTemplate.convertAndSend("/topic/admin/kitchen", notification);
        log.info("WebSocket: Order deletion notification - {}", orderNumber);
    }

    /**
     * Notifies about order cancellation
     */
    public void notifyOrderCancelled(Order order) {
        OrderNotificationDTO notification = buildOrderNotification(order, "ORDER_CANCELLED",
            "Pedido #" + order.getOrderNumber() + " ha sido cancelado");
        
        // Send to all chefs to remove from their view
        messagingTemplate.convertAndSend("/topic/chef/orders", notification);
        
        // Send to admin kitchen
        messagingTemplate.convertAndSend("/topic/admin/kitchen", notification);
        
        // If chef was assigned, send personal notification
        if (order.getPreparedBy() != null) {
            messagingTemplate.convertAndSendToUser(
                order.getPreparedBy().getUsername(),
                "/queue/orders",
                notification
            );
        }
        
        log.info("WebSocket: Order cancellation notification - {}", order.getOrderNumber());
    }

    // Helper method to build order notification DTO
    private OrderNotificationDTO buildOrderNotification(Order order, String type, String message) {
        return OrderNotificationDTO.builder()
            .orderId(order.getIdOrder())
            .orderNumber(order.getOrderNumber())
            .status(order.getStatus())
            .orderType(order.getOrderType())
            .tableNumber(order.getTable() != null ? order.getTable().getTableNumber() : null)
            .total(order.getTotal())
            .createdAt(order.getCreatedAt())
            .itemCount(order.getOrderDetails() != null ? order.getOrderDetails().size() : 0)
            .items(order.getOrderDetails() != null ? order.getOrderDetails().stream()
                .map(detail -> OrderNotificationDTO.OrderItemDTO.builder()
                    .name(detail.getItemMenu().getName())
                    .quantity(detail.getQuantity())
                    .requiresPreparation(detail.getItemMenu().getRequiresPreparation())
                    .build())
                .collect(Collectors.toList()) : null)
            .notificationType(type)
            .message(message)
            .chefName(order.getPreparedBy() != null ? 
                order.getPreparedBy().getNombre() + " " + order.getPreparedBy().getApellido() : null)
            .build();
    }

    // Inner classes for specific notification types
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class AdminNotification {
        private String message;
        private Object data;
    }

    @lombok.Data
    private static class OrderDeletionNotification {
        private Long orderId;
        private String orderNumber;
        private String notificationType = "ORDER_DELETED";

        public OrderDeletionNotification(Long orderId, String orderNumber) {
            this.orderId = orderId;
            this.orderNumber = orderNumber;
        }
    }
}
