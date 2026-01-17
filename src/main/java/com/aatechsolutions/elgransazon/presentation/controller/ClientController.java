package com.aatechsolutions.elgransazon.presentation.controller;

import com.aatechsolutions.elgransazon.application.service.*;
import com.aatechsolutions.elgransazon.domain.entity.*;
import com.aatechsolutions.elgransazon.presentation.dto.ChangePasswordDTO;
import com.aatechsolutions.elgransazon.presentation.dto.UpdateProfileDTO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for Customer views
 * Handles menu display and order management for customers
 */
@Controller
@RequestMapping("/client")
@PreAuthorize("hasRole('ROLE_CLIENT')")
@Slf4j
public class ClientController {

    private final OrderService orderService;
    private final ItemMenuService itemMenuService;
    private final CategoryService categoryService;
    private final SystemConfigurationService systemConfigurationService;
    private final CustomerService customerService;
    private final PromotionService promotionService;
    private final ReviewService reviewService;
    private final PasswordEncoder passwordEncoder;
    private final TicketPdfService ticketPdfService;
    private final BusinessHoursService businessHoursService;
    private final CustomerAddressService customerAddressService;

    public ClientController(
            @Qualifier("customerOrderService") OrderService orderService,
            ItemMenuService itemMenuService,
            CategoryService categoryService,
            SystemConfigurationService systemConfigurationService,
            CustomerService customerService,
            PromotionService promotionService,
            ReviewService reviewService,
            PasswordEncoder passwordEncoder,
            TicketPdfService ticketPdfService,
            BusinessHoursService businessHoursService,
            CustomerAddressService customerAddressService) {
        this.orderService = orderService;
        this.itemMenuService = itemMenuService;
        this.categoryService = categoryService;
        this.systemConfigurationService = systemConfigurationService;
        this.customerService = customerService;
        this.promotionService = promotionService;
        this.reviewService = reviewService;
        this.passwordEncoder = passwordEncoder;
        this.ticketPdfService = ticketPdfService;
        this.businessHoursService = businessHoursService;
        this.customerAddressService = customerAddressService;
    }

    /**
     * Show customer dashboard (landing page after login)
     */
    @GetMapping("/dashboard")
    public String showDashboard(Authentication authentication, Model model) {
        log.debug("Customer {} accessing dashboard", authentication.getName());
        
        try {
            // Get customer info
            Customer customer = customerService.findByUsernameOrEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Cliente no encontrado"));
            
            // Get customer statistics
            List<Order> allOrders = orderService.findAll();
            long totalOrders = allOrders.size();
            long activeOrders = allOrders.stream()
                    .filter(o -> o.getStatus() != OrderStatus.CANCELLED && 
                               o.getStatus() != OrderStatus.PAID)
                    .count();
            
            model.addAttribute("customer", customer);
            model.addAttribute("totalOrders", totalOrders);
            model.addAttribute("activeOrders", activeOrders);
            
            // Check if restaurant is currently open
            boolean isRestaurantOpen = businessHoursService.isOpenNow();
            model.addAttribute("isRestaurantOpen", isRestaurantOpen);
            log.debug("Restaurant is currently: {}", isRestaurantOpen ? "open" : "closed");
            
            return "client/dashboard";
            
        } catch (Exception e) {
            log.error("Error loading dashboard for customer", e);
            model.addAttribute("errorMessage", "Error al cargar el dashboard: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Show menu to customer in VIEW-ONLY mode (when restaurant is closed)
     */
    @GetMapping("/view")
    public String showMenuViewOnly(Authentication authentication, Model model) {
        log.debug("Customer {} accessing menu in view-only mode", authentication.getName());
        
        try {
            // Update item availability
            itemMenuService.updateAllItemsAvailability();
            
            // Get active categories and available items
            List<Category> categories = categoryService.getAllActiveCategories();
            List<ItemMenu> availableItems = itemMenuService.findAvailableItems();
            
            // Group items by category
            Map<Long, List<ItemMenu>> itemsByCategory = availableItems.stream()
                    .collect(Collectors.groupingBy(item -> item.getCategory().getIdCategory()));
            
            // Get system configuration
            SystemConfiguration config = systemConfigurationService.getConfiguration();
            
            // Get customer info
            Customer customer = customerService.findByUsernameOrEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Cliente no encontrado"));
            
            model.addAttribute("config", config);
            model.addAttribute("categories", categories);
            model.addAttribute("itemsByCategory", itemsByCategory);
            model.addAttribute("currentRole", "client");
            model.addAttribute("customer", customer);
            
            return "client/view";
            
        } catch (Exception e) {
            log.error("Error showing view-only menu", e);
            model.addAttribute("errorMessage", "Error al cargar el menú: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Show menu to customer (landing page after login)
     */
    @GetMapping("/menu")
    public String showMenu(Authentication authentication, Model model) {
        log.debug("Customer {} accessing menu", authentication.getName());
        
        try {
            // Update item availability
            itemMenuService.updateAllItemsAvailability();
            
            // Get active categories and available items
            List<Category> categories = categoryService.getAllActiveCategories();
            List<ItemMenu> availableItems = itemMenuService.findAvailableItems();
            
            // Group items by category
            Map<Long, List<ItemMenu>> itemsByCategory = availableItems.stream()
                    .collect(Collectors.groupingBy(item -> item.getCategory().getIdCategory()));
            
            // Get system configuration
            SystemConfiguration config = systemConfigurationService.getConfiguration();
            
            // Get customer info
            Customer customer = customerService.findByUsernameOrEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Cliente no encontrado"));
            
            // Get enabled payment methods - for client we need both restaurant and delivery
            // Default is TAKEOUT (uses restaurant methods), DELIVERY uses delivery methods
            // We'll pass both sets to allow JavaScript to switch based on selected order type
            Map<PaymentMethodType, Boolean> restaurantPayments = config.getPaymentMethods();
            Map<PaymentMethodType, Boolean> deliveryPayments = config.getDeliveryPaymentMethods();
            
            // Default to restaurant methods (TAKEOUT is the default)
            List<PaymentMethodType> enabledPaymentMethods = restaurantPayments.entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            
            // Get delivery enabled methods for when user switches to DELIVERY
            List<PaymentMethodType> deliveryPaymentMethods = deliveryPayments.entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            
            // Get customer addresses for delivery selection
            var customerAddresses = customerAddressService.getAddressesByCustomerId(customer.getIdCustomer());
            var defaultAddress = customerAddresses.stream()
                    .filter(CustomerAddress::getIsDefault)
                    .findFirst()
                    .orElse(null);
            
            model.addAttribute("config", config);
            model.addAttribute("categories", categories);
            model.addAttribute("itemsByCategory", itemsByCategory);
            model.addAttribute("currentRole", "client");
            model.addAttribute("customer", customer);
            model.addAttribute("orderTypes", Arrays.asList(OrderType.TAKEOUT, OrderType.DELIVERY));
            model.addAttribute("orderType", OrderType.TAKEOUT); // Default order type
            model.addAttribute("enabledPaymentMethods", enabledPaymentMethods);
            model.addAttribute("deliveryPaymentMethods", deliveryPaymentMethods);
            model.addAttribute("customerAddresses", customerAddresses);
            model.addAttribute("defaultAddress", defaultAddress);
            model.addAttribute("hasAddresses", !customerAddresses.isEmpty());
            
            return "client/menu";
            
        } catch (Exception e) {
            log.error("Error loading menu for customer", e);
            model.addAttribute("errorMessage", "Error al cargar el menú: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Show order history for customer
     */
    @GetMapping("/orders")
    public String showOrderHistory(Authentication authentication, Model model) {
        log.debug("Customer {} accessing order history", authentication.getName());
        
        try {
            // Get customer orders
            List<Order> orders = orderService.findAll();
            
            // Sort by created date descending
            orders.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
            
            // Calculate statistics
            long totalOrders = orders.size();
            long activeOrders = orders.stream()
                    .filter(o -> o.getStatus() != OrderStatus.CANCELLED && 
                               o.getStatus() != OrderStatus.PAID)
                    .count();
            long completedOrders = orders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.PAID)
                    .count();
            
            model.addAttribute("orders", orders);
            model.addAttribute("totalOrders", totalOrders);
            model.addAttribute("activeOrders", activeOrders);
            model.addAttribute("completedOrders", completedOrders);
            model.addAttribute("orderStatuses", OrderStatus.values());
            
            return "client/orders";
            
        } catch (Exception e) {
            log.error("Error loading order history for customer", e);
            model.addAttribute("errorMessage", "Error al cargar el historial: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Show order details
     */
    @GetMapping("/orders/{id}")
    public String showOrderDetail(@PathVariable Long id, Authentication authentication, Model model,
                                   RedirectAttributes redirectAttributes) {
        log.debug("Customer {} accessing order detail: {}", authentication.getName(), id);
        
        try {
            Order order = orderService.findByIdWithDetails(id)
                    .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
            
            model.addAttribute("order", order);
            model.addAttribute("orderDetails", order.getOrderDetails());
            
            return "client/order-detail";
            
        } catch (Exception e) {
            log.error("Error loading order detail", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
            return "redirect:/client/orders";
        }
    }

    /**
     * Create new order (AJAX endpoint)
     */
    @PostMapping("/orders/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestBody Map<String, Object> orderData,
            Authentication authentication) {
        
        log.info("Customer {} creating new order", authentication.getName());
        
        try {
            // Validate restaurant is open
            if (!businessHoursService.isOpenNow()) {
                log.warn("Attempt to create order outside business hours by customer: {}", authentication.getName());
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "No se puede crear el pedido. El restaurante no se encuentra en horario laborable en este momento."
                ));
            }
            
            // Get customer
            Customer customer = customerService.findByUsernameOrEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Cliente no encontrado"));
            
            // Parse order data
            OrderType orderType = OrderType.valueOf((String) orderData.get("orderType"));
            PaymentMethodType paymentMethod = PaymentMethodType.valueOf((String) orderData.get("paymentMethod"));
            
            // For DELIVERY orders, get selected address
            String deliveryAddress = null;
            String deliveryReferences = null;
            Double deliveryLatitude = null;
            Double deliveryLongitude = null;
            
            if (orderType == OrderType.DELIVERY) {
                // Get address from frontend selection (new map-based system)
                String addressFromFrontend = (String) orderData.get("deliveryAddress");
                Object addressIdObj = orderData.get("deliveryAddressId");
                
                if (addressIdObj != null && !addressIdObj.toString().isEmpty()) {
                    // Verify the address belongs to the customer
                    Long addressId = Long.valueOf(addressIdObj.toString());
                    var selectedAddress = customerAddressService.getAddressById(addressId, customer.getIdCustomer());
                    
                    if (selectedAddress.isPresent()) {
                        CustomerAddress addr = selectedAddress.get();
                        deliveryAddress = addr.getDisplayAddress();
                        deliveryLatitude = addr.getLatitude();
                        deliveryLongitude = addr.getLongitude();
                        // Get reference from saved address if not provided from frontend
                        String frontendReferences = (String) orderData.get("deliveryReferences");
                        if (frontendReferences == null || frontendReferences.trim().isEmpty()) {
                            deliveryReferences = addr.getReference();
                        } else {
                            deliveryReferences = frontendReferences;
                        }
                    } else if (addressFromFrontend != null && !addressFromFrontend.trim().isEmpty()) {
                        deliveryAddress = addressFromFrontend;
                        deliveryReferences = (String) orderData.get("deliveryReferences");
                    }
                } else if (addressFromFrontend != null && !addressFromFrontend.trim().isEmpty()) {
                    deliveryAddress = addressFromFrontend;
                    deliveryReferences = (String) orderData.get("deliveryReferences");
                }
                
                // Validate delivery address is provided
                if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Para pedidos a domicilio debes seleccionar una dirección guardada"
                    ));
                }
            }
            
            BigDecimal taxRate = new BigDecimal(systemConfigurationService.getConfiguration().getTaxRate().toString());
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) orderData.get("items");
            
            // Create order
            Order order = Order.builder()
                    .orderType(orderType)
                    .paymentMethod(paymentMethod)
                    .deliveryAddress(deliveryAddress)
                    .deliveryReferences(deliveryReferences)
                    .deliveryLatitude(deliveryLatitude)
                    .deliveryLongitude(deliveryLongitude)
                    .taxRate(taxRate)
                    .status(OrderStatus.PENDING)
                    .customer(customer)
                    .customerName(customer.getFullName())
                    .customerPhone(customer.getPhone())
                    .createdBy(authentication.getName())
                    .build();
            
            // Create order details
            List<OrderDetail> orderDetails = new ArrayList<>();
            for (Map<String, Object> itemData : items) {
                Long itemId = Long.valueOf(itemData.get("itemId").toString());
                Integer quantity = Integer.valueOf(itemData.get("quantity").toString());
                String comments = (String) itemData.get("comments");
                
                ItemMenu itemMenu = itemMenuService.findById(itemId)
                        .orElseThrow(() -> new IllegalArgumentException("Item no encontrado: " + itemId));
                
                // IMPORTANT: ALWAYS recalculate prices in backend (don't trust frontend values)
                // Get promotion ID from frontend
                Long promotionId = null;
                Object promotionIdObj = itemData.get("promotionId");
                if (promotionIdObj != null && !promotionIdObj.toString().isEmpty()) {
                    try {
                        promotionId = Long.valueOf(promotionIdObj.toString());
                        log.debug("Promotion ID from frontend: {} for item {}", promotionId, itemId);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid promotion ID format: {}", promotionIdObj);
                    }
                }
                
                // Recalculate promotion price in backend (NEVER trust frontend values)
                BigDecimal unitPrice = itemMenu.getPrice();
                BigDecimal promotionAppliedPrice = null;
                
                if (promotionId != null) {
                    // Fetch promotion from database
                    Promotion promotion = promotionService.findById(promotionId)
                            .orElse(null);
                    
                    if (promotion != null && promotion.isValidNow()) {
                        // Recalculate based on promotion type
                        switch (promotion.getPromotionType()) {
                            case FIXED_AMOUNT_DISCOUNT:
                                // Fixed discount: newPrice = originalPrice - discount
                                promotionAppliedPrice = promotion.calculateFixedDiscount(unitPrice);
                                log.debug("FIXED_AMOUNT_DISCOUNT: original={}, discount={}, final={}", 
                                    unitPrice, promotion.getDiscountAmount(), promotionAppliedPrice);
                                break;
                                
                            case PERCENTAGE_DISCOUNT:
                                // Percentage discount: newPrice = originalPrice * (1 - discount%)
                                promotionAppliedPrice = promotion.calculatePercentageDiscount(unitPrice);
                                log.debug("PERCENTAGE_DISCOUNT: original={}, percentage={}, final={}", 
                                    unitPrice, promotion.getDiscountAmount(), promotionAppliedPrice);
                                break;
                                
                            case BUY_X_PAY_Y:
                                // BUY_X_PAY_Y: only applies if quantity >= buyQuantity
                                if (quantity >= promotion.getBuyQuantity()) {
                                    promotionAppliedPrice = promotion.calculateBuyXPayY(unitPrice);
                                    log.debug("BUY_X_PAY_Y: original={}, buy={}, pay={}, final={}", 
                                        unitPrice, promotion.getBuyQuantity(), promotion.getPayQuantity(), promotionAppliedPrice);
                                } else {
                                    log.debug("BUY_X_PAY_Y not applied: quantity {} < buyQuantity {}", 
                                        quantity, promotion.getBuyQuantity());
                                }
                                break;
                        }
                    } else {
                        log.warn("Promotion {} is not active or doesn't exist, ignoring", promotionId);
                    }
                }
                
                // Build the order detail with recalculated values
                OrderDetail detail = OrderDetail.builder()
                        .itemMenu(itemMenu)
                        .quantity(quantity)
                        .unitPrice(unitPrice)
                        .promotionAppliedPrice(promotionAppliedPrice)
                        .appliedPromotionId(promotionId)
                        .comments(comments)
                        .itemStatus(OrderStatus.PENDING)
                        .build();
                
                // Calculate subtotal (this will use promotionAppliedPrice if present)
                detail.calculateSubtotal();
                orderDetails.add(detail);
            }
            
            // Validate stock
            Map<Long, String> stockErrors = orderService.validateStock(orderDetails);
            if (!stockErrors.isEmpty()) {
                // Build error message with item names
                String itemNames = stockErrors.values().stream()
                    .collect(Collectors.joining(", "));
                
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Stock insuficiente para: " + itemNames + ". ¡Te invitamos a ver otras opciones deliciosas en nuestro menú!",
                    "errorType", "STOCK_ERROR"
                ));
            }
            
            // Create order
            Order createdOrder = orderService.create(order, orderDetails);
            
            log.info("Order created successfully: {}", createdOrder.getOrderNumber());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Pedido creado exitosamente",
                "orderNumber", createdOrder.getOrderNumber(),
                "orderId", createdOrder.getIdOrder()
            ));
            
        } catch (Exception e) {
            log.error("Error creating order", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error al crear el pedido: " + e.getMessage()
            ));
        }
    }

    /**
     * Show menu to add items to existing order
     * GET /client/orders/{orderId}/add-items
     */
    @GetMapping("/orders/{orderId}/add-items")
    public String showMenuToAddItems(
            @PathVariable Long orderId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        log.debug("Customer {} adding items to order {}", authentication.getName(), orderId);

        try {
            // Get customer
            Customer customer = customerService.findByUsernameOrEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Cliente no encontrado"));

            // Get the order
            Order order = orderService.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));

            // Validate order belongs to customer
            if (!order.getCustomer().getIdCustomer().equals(customer.getIdCustomer())) {
                redirectAttributes.addFlashAttribute("errorMessage", "No tienes permiso para modificar este pedido");
                return "redirect:/client/orders";
            }

            // Validate order can accept new items
            if (!order.canAcceptNewItems()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    String.format("No se pueden agregar items a este pedido. Tipo: %s, Estado: %s",
                        order.getOrderType().getDisplayName(),
                        order.getStatus().getDisplayName()));
                return "redirect:/client/orders";
            }

            // Update availability for all items based on current stock
            itemMenuService.updateAllItemsAvailability();
            
            // Get all active categories with their menu items
            List<Category> categories = categoryService.getAllActiveCategories();
            
            // Get available menu items grouped by category
            List<ItemMenu> availableItems = itemMenuService.findAvailableItems();
            
            // Group items by category for easier display
            Map<Long, List<ItemMenu>> itemsByCategory = availableItems.stream()
                .collect(Collectors.groupingBy(item -> item.getCategory().getIdCategory()));

            // Get system configuration
            SystemConfiguration config = systemConfigurationService.getConfiguration();
            
            // Get enabled payment methods based on order type
            // For DELIVERY orders, use deliveryPaymentMethods; for others use regular paymentMethods
            Map<PaymentMethodType, Boolean> paymentMethodsMap = order.getOrderType() == OrderType.DELIVERY 
                    ? config.getDeliveryPaymentMethods() 
                    : config.getPaymentMethods();
            List<PaymentMethodType> enabledPaymentMethods = paymentMethodsMap.entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            // Set model attributes - similar to new order but with existing order context
            model.addAttribute("orderType", order.getOrderType());
            model.addAttribute("paymentMethod", order.getPaymentMethod());
            model.addAttribute("customerName", order.getCustomerName());
            model.addAttribute("customerPhone", order.getCustomerPhone());
            model.addAttribute("deliveryAddress", order.getDeliveryAddress());
            model.addAttribute("deliveryReferences", order.getDeliveryReferences());
            model.addAttribute("categories", categories);
            model.addAttribute("itemsByCategory", itemsByCategory);
            model.addAttribute("allItems", availableItems);
            model.addAttribute("customer", customer);
            model.addAttribute("currentRole", "client");
            model.addAttribute("config", config);
            model.addAttribute("enabledPaymentMethods", enabledPaymentMethods);
            
            // Add active promotions for items
            List<Promotion> activePromotions = promotionService.findActivePromotions();
            model.addAttribute("activePromotions", activePromotions);
            
            // IMPORTANT: Add existing order ID and number so the template knows it's "add mode"
            model.addAttribute("existingOrderId", order.getIdOrder());
            model.addAttribute("existingOrderNumber", order.getOrderNumber());

            return "client/add-items-menu";
            
        } catch (Exception e) {
            log.error("Error showing add items menu", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
            return "redirect:/client/orders";
        }
    }

    /**
     * Add items to existing order (AJAX endpoint)
     * POST /client/orders/{orderId}/add-items
     */
    @PostMapping("/orders/{orderId}/add-items")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addItemsToOrder(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> requestData,
            Authentication authentication) {
        
        log.info("Customer {} adding items to order {}", authentication.getName(), orderId);

        try {
            // Validate restaurant is open
            if (!businessHoursService.isOpenNow()) {
                log.warn("Attempt to add items to order outside business hours by customer: {}", authentication.getName());
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "No se pueden agregar items al pedido. El restaurante no se encuentra en horario laborable en este momento."
                ));
            }
            // Get customer
            Customer customer = customerService.findByUsernameOrEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Cliente no encontrado"));

            // Get the order
            Order order = orderService.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));

            // Validate order belongs to customer
            if (!order.getCustomer().getIdCustomer().equals(customer.getIdCustomer())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "No tienes permiso para modificar este pedido"
                ));
            }

            // Validate order can accept new items
            if (!order.canAcceptNewItems()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", String.format("No se pueden agregar items a este pedido. Tipo: %s, Estado: %s",
                        order.getOrderType().getDisplayName(),
                        order.getStatus().getDisplayName())
                ));
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) requestData.get("items");

            // Create new order details
            List<OrderDetail> newItems = new ArrayList<>();
            for (Map<String, Object> itemData : items) {
                Long itemId = Long.valueOf(itemData.get("itemId").toString());
                Integer quantity = Integer.valueOf(itemData.get("quantity").toString());
                String comments = (String) itemData.get("comments");

                ItemMenu itemMenu = itemMenuService.findById(itemId)
                        .orElseThrow(() -> new IllegalArgumentException("Item no encontrado: " + itemId));

                // IMPORTANT: ALWAYS recalculate prices in backend (don't trust frontend values)
                // Get promotion ID from frontend
                Long promotionId = null;
                Object promotionIdObj = itemData.get("promotionId");
                if (promotionIdObj != null && !promotionIdObj.toString().isEmpty()) {
                    try {
                        promotionId = Long.valueOf(promotionIdObj.toString());
                        log.debug("Promotion ID from frontend: {} for item {}", promotionId, itemId);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid promotion ID format: {}", promotionIdObj);
                    }
                }
                
                // Recalculate promotion price in backend (NEVER trust frontend values)
                BigDecimal unitPrice = itemMenu.getPrice();
                BigDecimal promotionAppliedPrice = null;
                
                if (promotionId != null) {
                    // Fetch promotion from database
                    Promotion promotion = promotionService.findById(promotionId)
                            .orElse(null);
                    
                    if (promotion != null && promotion.isValidNow()) {
                        // Recalculate based on promotion type
                        switch (promotion.getPromotionType()) {
                            case FIXED_AMOUNT_DISCOUNT:
                                promotionAppliedPrice = promotion.calculateFixedDiscount(unitPrice);
                                log.debug("FIXED_AMOUNT_DISCOUNT: original={}, discount={}, final={}", 
                                    unitPrice, promotion.getDiscountAmount(), promotionAppliedPrice);
                                break;
                                
                            case PERCENTAGE_DISCOUNT:
                                promotionAppliedPrice = promotion.calculatePercentageDiscount(unitPrice);
                                log.debug("PERCENTAGE_DISCOUNT: original={}, percentage={}, final={}", 
                                    unitPrice, promotion.getDiscountAmount(), promotionAppliedPrice);
                                break;
                                
                            case BUY_X_PAY_Y:
                                // BUY_X_PAY_Y: only applies if quantity >= buyQuantity
                                if (quantity >= promotion.getBuyQuantity()) {
                                    promotionAppliedPrice = promotion.calculateBuyXPayY(unitPrice);
                                    log.debug("BUY_X_PAY_Y: original={}, buy={}, pay={}, final={}", 
                                        unitPrice, promotion.getBuyQuantity(), promotion.getPayQuantity(), promotionAppliedPrice);
                                } else {
                                    log.debug("BUY_X_PAY_Y not applied: quantity {} < buyQuantity {}", 
                                        quantity, promotion.getBuyQuantity());
                                }
                                break;
                        }
                    } else {
                        log.warn("Promotion {} is not active or doesn't exist, ignoring", promotionId);
                    }
                }

                // Build the order detail with recalculated values
                OrderDetail detail = OrderDetail.builder()
                        .itemMenu(itemMenu)
                        .quantity(quantity)
                        .unitPrice(unitPrice)
                        .promotionAppliedPrice(promotionAppliedPrice)
                        .appliedPromotionId(promotionId)
                        .comments(comments)
                        .itemStatus(OrderStatus.PENDING)
                        .build();
                
                // Calculate subtotal (this will use promotionAppliedPrice if present)
                detail.calculateSubtotal();
                newItems.add(detail);
            }

            // Add items to order
            Order updatedOrder = orderService.addItemsToExistingOrder(orderId, newItems, authentication.getName());

            log.info("Items added successfully to order: {}", updatedOrder.getOrderNumber());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Items agregados exitosamente al pedido",
                "orderNumber", updatedOrder.getOrderNumber(),
                "orderId", updatedOrder.getIdOrder()
            ));

        } catch (Exception e) {
            log.error("Error adding items to order", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error al agregar items: " + e.getMessage()
            ));
        }
    }

    /**
     * Cancel order (AJAX endpoint)
     * POST /client/orders/{orderId}/cancel
     */
    @PostMapping("/orders/{orderId}/cancel")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelOrder(
            @PathVariable Long orderId,
            Authentication authentication) {
        
        log.info("Customer {} cancelling order {}", authentication.getName(), orderId);

        try {
            // Get customer
            Customer customer = customerService.findByUsernameOrEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Cliente no encontrado"));

            // Get the order
            Order order = orderService.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));

            // Validate order belongs to customer
            if (!order.getCustomer().getIdCustomer().equals(customer.getIdCustomer())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "No tienes permiso para cancelar este pedido"
                ));
            }

            // Cancel the order - CustomerOrderServiceImpl.cancel() handles all validation:
            // - Order not in final states (CANCELLED, PAID, DELIVERED, ON_THE_WAY)
            // - Items with preparation (Chef/Barista) must be PENDING
            // - Items without preparation must be READY
            Order cancelledOrder = orderService.cancel(orderId, authentication.getName());

            log.info("Order {} cancelled successfully by customer {}", cancelledOrder.getOrderNumber(), authentication.getName());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Pedido " + cancelledOrder.getOrderNumber() + " cancelado exitosamente. El stock ha sido devuelto automáticamente.",
                "orderNumber", cancelledOrder.getOrderNumber()
            ));

        } catch (Exception e) {
            log.error("Error cancelling order", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Delete item from order (AJAX endpoint)
     * POST /client/orders/{orderId}/items/{itemId}/delete
     */
    @PostMapping("/orders/{orderId}/items/{itemId}/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteOrderItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            Authentication authentication) {
        
        log.info("Customer {} deleting item {} from order {}", authentication.getName(), itemId, orderId);

        try {
            // Get customer
            Customer customer = customerService.findByUsernameOrEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Cliente no encontrado"));

            // Get the order to validate ownership
            Order order = orderService.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));

            // Validate order belongs to customer
            if (!order.getCustomer().getIdCustomer().equals(customer.getIdCustomer())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "No tienes permiso para modificar este pedido"
                ));
            }

            // Try to delete the item
            OrderDetail deletedItem = orderService.deleteOrderItem(orderId, itemId, authentication.getName());

            log.info("Item '{}' deleted from order {} by customer {}", 
                    deletedItem.getItemMenu().getName(), order.getOrderNumber(), authentication.getName());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Item '" + deletedItem.getItemMenu().getName() + "' eliminado exitosamente. El stock ha sido devuelto.",
                "itemName", deletedItem.getItemMenu().getName()
            ));

        } catch (IllegalStateException e) {
            // Check if it's the last item - should cancel order instead
            if ("LAST_ITEM_CANCEL_ORDER".equals(e.getMessage())) {
                log.info("Last item deletion requested - cancelling order {} instead", orderId);
                
                try {
                    Order cancelledOrder = orderService.cancel(orderId, authentication.getName());
                    
                    return ResponseEntity.ok(Map.of(
                        "success", true,
                        "isLastItem", true,
                        "orderCancelled", true,
                        "message", "Era el último item del pedido. El pedido " + cancelledOrder.getOrderNumber() + " ha sido cancelado y el stock fue devuelto.",
                        "orderNumber", cancelledOrder.getOrderNumber()
                    ));
                } catch (Exception cancelError) {
                    log.error("Error cancelling order for last item deletion", cancelError);
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "isLastItem", true,
                        "message", "No se puede eliminar el item ni cancelar el pedido: " + cancelError.getMessage()
                    ));
                }
            }
            
            log.error("Cannot delete item from order", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error deleting item from order", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error al eliminar el item: " + e.getMessage()
            ));
        }
    }

    /**
     * Get active promotions (AJAX endpoint)
     */
    @GetMapping("/promotions/active")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getActivePromotions() {
        log.debug("Fetching active promotions for customer");
        
        try {
            List<Promotion> promotions = promotionService.findActivePromotions();
            
            List<Map<String, Object>> promotionData = promotions.stream()
                    .map(this::convertPromotionToMap)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(promotionData);
            
        } catch (Exception e) {
            log.error("Error fetching active promotions", e);
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * Get maximum available quantity for a menu item based on ingredient stock (AJAX)
     */
    @GetMapping("/menu-items/{id}/max-quantity")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMaxQuantity(@PathVariable Long id) {
        log.debug("Getting max available quantity for menu item {}", id);
        
        Map<String, Object> response = new java.util.HashMap<>();
        try {
            int maxQuantity = itemMenuService.getMaxAvailableQuantity(id);
            response.put("maxQuantity", maxQuantity);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting max quantity for item {}", id, e);
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("maxQuantity", 0);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Update customer profile (WITHOUT password) - Using DTO
     */
    @PostMapping("/profile/update")
    public String updateProfile(
            @Valid @ModelAttribute UpdateProfileDTO profileDTO,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        log.info("Customer {} updating profile", authentication.getName());
        
        try {
            Customer existing = customerService.findByUsernameOrEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Cliente no encontrado"));
            
            // Validate form errors
            if (bindingResult.hasErrors()) {
                log.warn("Validation errors found during profile update");
                model.addAttribute("customer", existing);
                model.addAttribute("profileDTO", profileDTO);
                model.addAttribute("passwordDTO", new ChangePasswordDTO());
                return "client/profile";
            }
            
            // Validate unique constraints (except for current customer)
            if (!existing.getUsername().equalsIgnoreCase(profileDTO.getUsername()) && 
                customerService.existsByUsername(profileDTO.getUsername())) {
                bindingResult.rejectValue("username", "error.customer", "El nombre de usuario ya está en uso");
                model.addAttribute("customer", existing);
                model.addAttribute("profileDTO", profileDTO);
                model.addAttribute("passwordDTO", new ChangePasswordDTO());
                return "client/profile";
            }
            
            if (!existing.getPhone().equals(profileDTO.getPhone()) && 
                customerService.existsByPhone(profileDTO.getPhone())) {
                bindingResult.rejectValue("phone", "error.customer", "El teléfono ya está registrado");
                model.addAttribute("customer", existing);
                model.addAttribute("profileDTO", profileDTO);
                model.addAttribute("passwordDTO", new ChangePasswordDTO());
                return "client/profile";
            }
            
            // Update only allowed fields from DTO
            existing.setFullName(profileDTO.getFullName());
            existing.setUsername(profileDTO.getUsername());
            existing.setPhone(profileDTO.getPhone());
            
            customerService.update(existing.getIdCustomer(), existing);
            
            log.info("Customer profile updated successfully: {}", existing.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Perfil actualizado exitosamente");
            return "redirect:/client/profile";
            
        } catch (Exception e) {
            log.error("Error updating customer profile", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error al actualizar perfil: " + e.getMessage());
            return "redirect:/client/profile";
        }
    }

    /**
     * Change customer password (SEPARATE endpoint) - Using DTO
     */
    @PostMapping("/profile/change-password")
    public String changePassword(
            @Valid @ModelAttribute ChangePasswordDTO passwordDTO,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        log.info("Customer {} changing password", authentication.getName());
        
        try {
            // Validate form errors
            if (bindingResult.hasErrors()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Por favor corrige los errores en el formulario");
                return "redirect:/client/profile";
            }
            
            // Validate passwords match
            if (!passwordDTO.passwordsMatch()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Las contraseñas no coinciden");
                return "redirect:/client/profile";
            }
            
            Customer existing = customerService.findByUsernameOrEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Cliente no encontrado"));
            
            // Encode the new password before updating
            String encodedPassword = passwordEncoder.encode(passwordDTO.getNewPassword());
            existing.setPassword(encodedPassword);
            customerService.update(existing.getIdCustomer(), existing);
            
            log.info("Customer password changed successfully: {}", existing.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Contraseña actualizada exitosamente");
            return "redirect:/client/profile";
            
        } catch (Exception e) {
            log.error("Error changing customer password", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error al cambiar contraseña: " + e.getMessage());
            return "redirect:/client/profile";
        }
    }

    /**
     * Show customer profile
     */
    @GetMapping("/profile")
    public String showProfile(Authentication authentication, Model model) {
        log.debug("Customer {} accessing profile", authentication.getName());
        
        try {
            Customer customer = customerService.findByUsernameOrEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Cliente no encontrado"));
            
            // Create DTOs for form binding
            UpdateProfileDTO profileDTO = new UpdateProfileDTO();
            profileDTO.setFullName(customer.getFullName());
            profileDTO.setUsername(customer.getUsername());
            profileDTO.setPhone(customer.getPhone());
            
            // Get customer addresses for the map
            var addresses = customerAddressService.getAddressesByCustomerId(customer.getIdCustomer());
            
            model.addAttribute("customer", customer); // For display (email, etc.)
            model.addAttribute("profileDTO", profileDTO); // For profile form binding
            model.addAttribute("passwordDTO", new ChangePasswordDTO()); // For password form binding
            model.addAttribute("addresses", addresses); // For addresses section
            return "client/profile";
            
        } catch (Exception e) {
            log.error("Error loading customer profile", e);
            model.addAttribute("errorMessage", "Error al cargar perfil: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Show customer review form
     */
    @GetMapping("/review")
    public String showReviewForm(Authentication authentication, Model model) {
        log.debug("Customer {} accessing review form", authentication.getName());
        
        try {
            Customer customer = customerService.findByUsernameOrEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Cliente no encontrado"));
            
            // Check if customer has at least one PAID order
            List<Order> customerOrders = orderService.findAll();
            boolean hasPaidOrder = customerOrders.stream()
                    .anyMatch(order -> order.getStatus() == OrderStatus.PAID);
            
            if (!hasPaidOrder) {
                model.addAttribute("noPurchase", true);
                return "client/review";
            }
            
            // Check if customer already has a review
            Optional<Review> existingReview = reviewService.getReviewByCustomer(customer);
            
            model.addAttribute("customer", customer);
            model.addAttribute("existingReview", existingReview.orElse(null));
            model.addAttribute("hasReview", existingReview.isPresent());
            model.addAttribute("noPurchase", false);
            
            return "client/review";
            
        } catch (Exception e) {
            log.error("Error loading review form", e);
            model.addAttribute("errorMessage", "Error al cargar formulario: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Submit or update customer review
     */
    @PostMapping("/review")
    public String submitReview(
            @RequestParam Integer rating,
            @RequestParam String comment,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        log.info("Customer {} submitting review", authentication.getName());
        
        try {
            Customer customer = customerService.findByUsernameOrEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Cliente no encontrado"));
            
            // Verify customer has at least one PAID order
            List<Order> customerOrders = orderService.findAll();
            boolean hasPaidOrder = customerOrders.stream()
                    .anyMatch(order -> order.getStatus() == OrderStatus.PAID);
            
            if (!hasPaidOrder) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                        "Debes realizar al menos una compra antes de dejar una reseña");
                return "redirect:/client/review";
            }
            
            // Validate input
            if (rating == null || rating < 1 || rating > 5) {
                redirectAttributes.addFlashAttribute("errorMessage", "La calificación debe estar entre 1 y 5 estrellas");
                return "redirect:/client/review";
            }
            
            if (comment == null || comment.trim().length() < 10) {
                redirectAttributes.addFlashAttribute("errorMessage", "El comentario debe tener al menos 10 caracteres");
                return "redirect:/client/review";
            }
            
            if (comment.trim().length() > 500) {
                redirectAttributes.addFlashAttribute("errorMessage", "El comentario no puede exceder 500 caracteres");
                return "redirect:/client/review";
            }
            
            // Create or update review
            reviewService.createOrUpdateReview(customer, rating, comment.trim());
            
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Su reseña se ha enviado correctamente. ¡Gracias!");
            
            return "redirect:/client/review";
            
        } catch (Exception e) {
            log.error("Error submitting review", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error al enviar reseña: " + e.getMessage());
            return "redirect:/client/review";
        }
    }

    /**
     * Download PDF ticket for paid order
     * GET /client/orders/{orderId}/download-ticket
     */
    @GetMapping("/orders/{orderId}/download-ticket")
    public ResponseEntity<byte[]> downloadTicket(
            @PathVariable Long orderId,
            Authentication authentication) {
        
        log.info("Customer {} downloading ticket for order {}", authentication.getName(), orderId);
        
        try {
            // Find the order
            Order order = orderService.findByIdWithDetails(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));
            
            // Validate that order belongs to the customer
            Customer customer = customerService.findByUsernameOrEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Cliente no encontrado"));
            
            if (!order.getCustomer().getIdCustomer().equals(customer.getIdCustomer())) {
                log.warn("Customer {} attempted to download ticket for order {} that doesn't belong to them", 
                         authentication.getName(), orderId);
                return ResponseEntity.status(403).build();
            }
            
            // Validate that order is PAID
            if (order.getStatus() != OrderStatus.PAID) {
                log.warn("Attempted to download ticket for unpaid order: {}", order.getOrderNumber());
                return ResponseEntity.badRequest().build();
            }
            
            // Generate PDF ticket
            byte[] pdfBytes = ticketPdfService.generateTicket(order);
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "ticket_" + order.getOrderNumber() + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Error downloading ticket: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error generating ticket PDF for order ID: " + orderId, e);
            return ResponseEntity.status(500).build();
        }
    }

    // ========== Helper Methods ==========

    private Map<String, Object> convertPromotionToMap(Promotion promotion) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", promotion.getIdPromotion());
        map.put("name", promotion.getName());
        map.put("description", promotion.getDescription());
        map.put("type", promotion.getPromotionType().name());
        map.put("promotionType", promotion.getPromotionType().name()); // Added for JavaScript compatibility
        map.put("imageUrl", promotion.getImageUrl());
        
        // Add type-specific fields and display label
        if (promotion.getPromotionType() == PromotionType.BUY_X_PAY_Y) {
            map.put("buyQuantity", promotion.getBuyQuantity());
            map.put("payQuantity", promotion.getPayQuantity());
            map.put("displayLabel", promotion.getBuyQuantity() + "x" + promotion.getPayQuantity());
        } else if (promotion.getPromotionType() == PromotionType.PERCENTAGE_DISCOUNT) {
            map.put("discountPercentage", promotion.getDiscountPercentage());
            map.put("displayLabel", promotion.getDiscountPercentage().setScale(2) + "% OFF");
        } else if (promotion.getPromotionType() == PromotionType.FIXED_AMOUNT_DISCOUNT) {
            map.put("discountAmount", promotion.getDiscountAmount());
            map.put("displayLabel", "$" + promotion.getDiscountAmount() + " OFF");
        }
        
        // Add item IDs
        List<Long> itemIds = promotion.getItems().stream()
                .map(ItemMenu::getIdItemMenu)
                .collect(Collectors.toList());
        map.put("itemIds", itemIds);
        
        return map;
    }

    // ========== ADDRESS MANAGEMENT ENDPOINTS ==========

    /**
     * Get all addresses for the current customer (AJAX)
     */
    @GetMapping("/addresses")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAddresses(Authentication authentication) {
        try {
            Customer customer = customerService.findByUsernameOrEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Cliente no encontrado"));
            
            var addresses = customerAddressService.getAddressesByCustomerId(customer.getIdCustomer());
            
            List<Map<String, Object>> addressList = addresses.stream()
                    .map(addr -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", addr.getIdAddress());
                        map.put("label", addr.getLabel());
                        map.put("address", addr.getAddress());
                        map.put("reference", addr.getReference());
                        map.put("latitude", addr.getLatitude());
                        map.put("longitude", addr.getLongitude());
                        map.put("isDefault", addr.getIsDefault());
                        map.put("displayAddress", addr.getDisplayAddress());
                        return map;
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(addressList);
        } catch (Exception e) {
            log.error("Error fetching addresses", e);
            return ResponseEntity.badRequest().body(List.of());
        }
    }

    /**
     * Create a new address (AJAX)
     */
    @PostMapping("/addresses")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createAddress(
            @RequestBody Map<String, Object> addressData,
            Authentication authentication) {
        try {
            Customer customer = customerService.findByUsernameOrEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Cliente no encontrado"));
            
            String label = (String) addressData.get("label");
            String address = (String) addressData.get("address");
            String reference = (String) addressData.get("reference");
            Double latitude = ((Number) addressData.get("latitude")).doubleValue();
            Double longitude = ((Number) addressData.get("longitude")).doubleValue();
            boolean setAsDefault = Boolean.TRUE.equals(addressData.get("setAsDefault"));
            
            var newAddress = customerAddressService.createAddress(
                    customer.getIdCustomer(), label, address, reference, 
                    latitude, longitude, setAsDefault);
            
            log.info("Address created for customer {}: {}", customer.getUsername(), label);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Dirección guardada exitosamente",
                    "addressId", newAddress.getIdAddress()
            ));
        } catch (Exception e) {
            log.error("Error creating address", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error al guardar dirección: " + e.getMessage()
            ));
        }
    }

    /**
     * Update an existing address (AJAX)
     */
    @PutMapping("/addresses/{addressId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateAddress(
            @PathVariable Long addressId,
            @RequestBody Map<String, Object> addressData,
            Authentication authentication) {
        try {
            Customer customer = customerService.findByUsernameOrEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Cliente no encontrado"));
            
            String label = (String) addressData.get("label");
            String address = (String) addressData.get("address");
            String reference = (String) addressData.get("reference");
            Double latitude = ((Number) addressData.get("latitude")).doubleValue();
            Double longitude = ((Number) addressData.get("longitude")).doubleValue();
            boolean setAsDefault = Boolean.TRUE.equals(addressData.get("setAsDefault"));
            
            customerAddressService.updateAddress(
                    addressId, customer.getIdCustomer(), label, address, 
                    reference, latitude, longitude, setAsDefault);
            
            log.info("Address {} updated for customer {}", addressId, customer.getUsername());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Dirección actualizada exitosamente"
            ));
        } catch (Exception e) {
            log.error("Error updating address", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error al actualizar dirección: " + e.getMessage()
            ));
        }
    }

    /**
     * Delete an address (AJAX)
     */
    @DeleteMapping("/addresses/{addressId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteAddress(
            @PathVariable Long addressId,
            Authentication authentication) {
        try {
            Customer customer = customerService.findByUsernameOrEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Cliente no encontrado"));
            
            boolean deleted = customerAddressService.deleteAddress(addressId, customer.getIdCustomer());
            
            if (deleted) {
                log.info("Address {} deleted for customer {}", addressId, customer.getUsername());
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Dirección eliminada exitosamente"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "No se encontró la dirección"
                ));
            }
        } catch (Exception e) {
            log.error("Error deleting address", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error al eliminar dirección: " + e.getMessage()
            ));
        }
    }

    /**
     * Set an address as default (AJAX)
     */
    @PostMapping("/addresses/{addressId}/set-default")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> setDefaultAddress(
            @PathVariable Long addressId,
            Authentication authentication) {
        try {
            Customer customer = customerService.findByUsernameOrEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Cliente no encontrado"));
            
            customerAddressService.setAsDefault(addressId, customer.getIdCustomer());
            
            log.info("Address {} set as default for customer {}", addressId, customer.getUsername());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Dirección establecida como predeterminada"
            ));
        } catch (Exception e) {
            log.error("Error setting default address", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error: " + e.getMessage()
            ));
        }
    }
}
