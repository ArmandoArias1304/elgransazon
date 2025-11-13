package com.aatechsolutions.elgransazon.application.service;

import com.aatechsolutions.elgransazon.domain.entity.Order;
import com.aatechsolutions.elgransazon.domain.entity.OrderDetail;
import com.aatechsolutions.elgransazon.domain.entity.OrderStatus;
import com.aatechsolutions.elgransazon.domain.entity.Ingredient;
import com.aatechsolutions.elgransazon.domain.repository.EmployeeRepository;
import com.aatechsolutions.elgransazon.domain.repository.OrderRepository;
import com.aatechsolutions.elgransazon.domain.repository.IngredientRepository;
import com.aatechsolutions.elgransazon.presentation.dto.DashboardStatsDTO;
import com.aatechsolutions.elgransazon.presentation.dto.DashboardStatsDTO.PopularItemDTO;
import com.aatechsolutions.elgransazon.presentation.dto.DashboardStatsDTO.InventoryAlertDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of Dashboard service
 * Provides aggregated statistics for the admin dashboard
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository orderRepository;
    private final EmployeeRepository employeeRepository;
    private final IngredientRepository ingredientRepository;

    @Override
    public DashboardStatsDTO getDashboardStats() {
        log.debug("Calculating dashboard statistics");

        // Get today's date range
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        
        // Get yesterday's date range
        LocalDateTime yesterdayStart = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime yesterdayEnd = LocalDate.now().minusDays(1).atTime(LocalTime.MAX);

        // Get orders for today and yesterday
        List<Order> todayOrders = orderRepository.findByDateRange(todayStart, todayEnd);
        List<Order> yesterdayOrders = orderRepository.findByDateRange(yesterdayStart, yesterdayEnd);

        // Calculate sales statistics
        BigDecimal todaySales = calculateSales(todayOrders);
        BigDecimal yesterdaySales = calculateSales(yesterdayOrders);
        Double salesChangePercentage = calculatePercentageChange(todaySales, yesterdaySales);

        // Calculate orders statistics
        Long todayOrdersCount = (long) todayOrders.size();
        Long yesterdayOrdersCount = (long) yesterdayOrders.size();
        Double ordersChangePercentage = calculatePercentageChange(
            BigDecimal.valueOf(todayOrdersCount), 
            BigDecimal.valueOf(yesterdayOrdersCount)
        );

        // Calculate customers statistics
        Long todayCustomers = countUniqueCustomers(todayOrders);
        Long yesterdayCustomers = countUniqueCustomers(yesterdayOrders);
        Double customersChangePercentage = calculatePercentageChange(
            BigDecimal.valueOf(todayCustomers), 
            BigDecimal.valueOf(yesterdayCustomers)
        );

        // Calculate projected revenue
        BigDecimal totalHistoricalRevenue = calculateTotalHistoricalRevenue();

        // Get popular items
        List<PopularItemDTO> popularItems = getPopularItems(todayOrders);

        // Get active employees
        Long totalEmployees = employeeRepository.count();
        Long activeEmployees = employeeRepository.countByEnabledTrue();
        Double capacityPercentage = totalEmployees > 0 
            ? (activeEmployees.doubleValue() / totalEmployees.doubleValue()) * 100 
            : 0.0;
        
        List<String> employeeInitials = employeeRepository.findByEnabledTrue()
            .stream()
            .limit(4)
            .map(emp -> {
                String firstName = emp.getNombre() != null && !emp.getNombre().isEmpty() 
                    ? emp.getNombre().substring(0, 1).toUpperCase() 
                    : "";
                String lastName = emp.getApellido() != null && !emp.getApellido().isEmpty() 
                    ? emp.getApellido().substring(0, 1).toUpperCase() 
                    : "";
                return firstName + lastName;
            })
            .collect(Collectors.toList());

        // Get inventory alerts (out of stock, low stock)
        List<InventoryAlertDTO> inventoryAlerts = getInventoryAlerts();

        return DashboardStatsDTO.builder()
            .todaySales(todaySales)
            .salesChangePercentage(Math.abs(salesChangePercentage))
            .salesIncreased(salesChangePercentage >= 0)
            .todayOrders(todayOrdersCount)
            .ordersChangePercentage(Math.abs(ordersChangePercentage))
            .ordersIncreased(ordersChangePercentage >= 0)
            .todayCustomers(todayCustomers)
            .customersChangePercentage(Math.abs(customersChangePercentage))
            .customersIncreased(customersChangePercentage >= 0)
            .totalHistoricalRevenue(totalHistoricalRevenue)
            .popularItems(popularItems)
            .activeEmployees(activeEmployees.intValue())
            .totalEmployees(totalEmployees.intValue())
            .capacityPercentage(capacityPercentage)
            .employeeInitials(employeeInitials)
            .inventoryAlerts(inventoryAlerts)
            .build();
    }

    /**
     * Calculate total sales from orders (only PAID orders)
     * Only includes subtotal + tax, excludes tips
     */
    private BigDecimal calculateSales(List<Order> orders) {
        return orders.stream()
            .filter(order -> order.getStatus() == OrderStatus.PAID)
            .map(Order::getTotal) // Only subtotal + tax, no tips
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate percentage change between two values
     */
    private Double calculatePercentageChange(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        
        BigDecimal change = current.subtract(previous);
        BigDecimal percentage = change
            .divide(previous, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
        
        return percentage.doubleValue();
    }

    /**
     * Count unique customers from orders
     * Only counts orders with PAID status (completed visits)
     * Each PAID order = one customer/group that came, consumed, and left
     */
    private Long countUniqueCustomers(List<Order> orders) {
        return orders.stream()
            .filter(order -> order.getStatus() == OrderStatus.PAID)
            .count();
    }

    /**
     * Calculate total historical revenue from all PAID orders (all time)
     */
    private BigDecimal calculateTotalHistoricalRevenue() {
        // Get ALL orders (no date filter)
        List<Order> allOrders = orderRepository.findAll();
        
        return allOrders.stream()
            .filter(order -> order.getStatus() == OrderStatus.PAID)
            .map(Order::getTotal) // Subtotal + tax, no tips
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get top 10 popular items
     */
    private List<PopularItemDTO> getPopularItems(List<Order> todayOrders) {
        // Count items ordered today
        Map<String, Long> itemCounts = new HashMap<>();
        
        for (Order order : todayOrders) {
            if (order.getOrderDetails() != null) {
                for (OrderDetail detail : order.getOrderDetails()) {
                    if (detail.getItemMenu() != null) {
                        String itemName = detail.getItemMenu().getName();
                        Long quantity = detail.getQuantity().longValue();
                        itemCounts.merge(itemName, quantity, Long::sum);
                    }
                }
            }
        }
        
        // If no items today, try to get from all-time orders
        if (itemCounts.isEmpty()) {
            List<Order> allOrders = orderRepository.findAll();
            for (Order order : allOrders) {
                if (order.getOrderDetails() != null) {
                    for (OrderDetail detail : order.getOrderDetails()) {
                        if (detail.getItemMenu() != null) {
                            String itemName = detail.getItemMenu().getName();
                            Long quantity = detail.getQuantity().longValue();
                            itemCounts.merge(itemName, quantity, Long::sum);
                        }
                    }
                }
            }
        }
        
        // If still no items, return empty list
        if (itemCounts.isEmpty()) {
            return List.of();
        }
        
        // Sort by count and get top 10
        List<Map.Entry<String, Long>> sortedItems = itemCounts.entrySet()
            .stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toList());
        
        // Get max count for percentage calculation
        Long maxCount = sortedItems.isEmpty() ? 1L : sortedItems.get(0).getValue();
        
        // Create DTOs
        List<PopularItemDTO> popularItems = new ArrayList<>();
        String[] colors = {"primary", "blue-500", "purple-500", "orange-500", "pink-500", "indigo-500", "teal-500", "amber-500", "rose-500", "cyan-500"};
        String[] gradients = {
            "from-primary to-primary-dark",
            "from-blue-400 to-blue-600",
            "from-purple-400 to-purple-600",
            "from-orange-400 to-orange-600",
            "from-pink-400 to-pink-600",
            "from-indigo-400 to-indigo-600",
            "from-teal-400 to-teal-600",
            "from-amber-400 to-amber-600",
            "from-rose-400 to-rose-600",
            "from-cyan-400 to-cyan-600"
        };
        
        for (int i = 0; i < sortedItems.size(); i++) {
            Map.Entry<String, Long> entry = sortedItems.get(i);
            Double percentage = (entry.getValue().doubleValue() / maxCount.doubleValue()) * 100;
            
            popularItems.add(PopularItemDTO.builder()
                .rank(i + 1)
                .itemName(entry.getKey())
                .orderCount(entry.getValue())
                .maxOrderCount(maxCount)
                .percentage(percentage)
                .color(colors[i % colors.length])
                .badgeGradient(gradients[i % gradients.length])
                .build());
        }
        
        return popularItems;
    }

    /**
     * Get inventory alerts (out of stock, low stock, healthy stock)
     * Returns top 3 items with most critical status
     */
    private List<InventoryAlertDTO> getInventoryAlerts() {
        // Get all active ingredients
        List<Ingredient> ingredients = ingredientRepository.findByActiveTrue();
        
        // If no ingredients, return empty list
        if (ingredients.isEmpty()) {
            return List.of();
        }
        
        // Separate by status
        List<Ingredient> outOfStock = new ArrayList<>();
        List<Ingredient> lowStock = new ArrayList<>();
        List<Ingredient> healthyStock = new ArrayList<>();
        
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isOutOfStock()) {
                outOfStock.add(ingredient);
            } else if (ingredient.isLowStock()) {
                lowStock.add(ingredient);
            } else if (ingredient.isHealthyStock()) {
                healthyStock.add(ingredient);
            }
        }
        
        // Build alert list (prioritize: out of stock > low stock > healthy)
        List<InventoryAlertDTO> alerts = new ArrayList<>();
        
        // Add out of stock (red)
        outOfStock.stream()
            .limit(5)
            .forEach(ingredient -> alerts.add(InventoryAlertDTO.builder()
                .ingredientName(ingredient.getName())
                .status("out-of-stock")
                .statusText("Agotado")
                .icon("error")
                .colorClass("red")
                .build()));
        
        // Add low stock (yellow) if we have less than 5 items
        if (alerts.size() < 5) {
            lowStock.stream()
                .limit(5 - alerts.size())
                .forEach(ingredient -> alerts.add(InventoryAlertDTO.builder()
                    .ingredientName(ingredient.getName())
                    .status("low-stock")
                    .statusText("Bajo stock")
                    .icon("warning")
                    .colorClass("yellow")
                    .build()));
        }
        
        // Add healthy stock (green) if we still have less than 5 items
        if (alerts.size() < 5) {
            healthyStock.stream()
                .limit(5 - alerts.size())
                .forEach(ingredient -> alerts.add(InventoryAlertDTO.builder()
                    .ingredientName(ingredient.getName())
                    .status("healthy")
                    .statusText("En stock")
                    .icon("check_circle")
                    .colorClass("green")
                    .build()));
        }
        
        return alerts;
    }
    
    @Override
    public List<PopularItemDTO> getPopularItemsByPeriod(String period) {
        LocalDateTime startDate;
        LocalDateTime endDate = LocalDateTime.now();
        
        switch (period.toLowerCase()) {
            case "week":
                startDate = LocalDate.now().minusWeeks(1).atStartOfDay();
                break;
            case "month":
                startDate = LocalDate.now().minusMonths(1).atStartOfDay();
                break;
            case "today":
            default:
                startDate = LocalDate.now().atStartOfDay();
                break;
        }
        
        // Get orders for the period
        List<Order> orders = orderRepository.findByDateRange(startDate, endDate);
        
        return getPopularItems(orders);
    }
}
