package com.aatechsolutions.elgransazon.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for Dashboard Statistics
 * Contains aggregated data for the admin dashboard
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {

    // ========== Sales Statistics ==========
    
    /**
     * Total sales for today
     */
    private BigDecimal todaySales;
    
    /**
     * Sales percentage change compared to yesterday
     */
    private Double salesChangePercentage;
    
    /**
     * Whether sales increased or decreased
     */
    private boolean salesIncreased;

    // ========== Orders Statistics ==========
    
    /**
     * Total orders for today
     */
    private Long todayOrders;
    
    /**
     * Orders percentage change compared to yesterday
     */
    private Double ordersChangePercentage;
    
    /**
     * Whether orders increased or decreased
     */
    private boolean ordersIncreased;

    // ========== Customers Statistics ==========
    
    /**
     * Total unique customers for today
     */
    private Long todayCustomers;
    
    /**
     * Customers percentage change compared to yesterday
     */
    private Double customersChangePercentage;
    
    /**
     * Whether customers increased or decreased
     */
    private boolean customersIncreased;

    // ========== Total Historical Revenue ==========
    
    /**
     * Total revenue from all time (all PAID orders ever)
     */
    private BigDecimal totalHistoricalRevenue;

    // ========== Popular Items ==========
    
    /**
     * List of most popular menu items today
     */
    private List<PopularItemDTO> popularItems;

    // ========== Active Employees ==========
    
    /**
     * Number of employees currently working
     */
    private Integer activeEmployees;
    
    /**
     * Total employee capacity
     */
    private Integer totalEmployees;
    
    /**
     * Capacity percentage
     */
    private Double capacityPercentage;
    
    /**
     * List of active employee initials
     */
    private List<String> employeeInitials;

    // ========== Inventory Alerts ==========
    
    /**
     * List of ingredients with stock issues (low stock, out of stock)
     */
    private List<InventoryAlertDTO> inventoryAlerts;

    /**
     * DTO for inventory alerts
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InventoryAlertDTO {
        
        /**
         * Ingredient name
         */
        private String ingredientName;
        
        /**
         * Stock status: "OUT_OF_STOCK", "LOW_STOCK", "HEALTHY"
         */
        private String status;
        
        /**
         * Status display text
         */
        private String statusText;
        
        /**
         * Icon name for Material Symbols
         */
        private String icon;
        
        /**
         * Color class for styling (red, yellow, green)
         */
        private String colorClass;
    }

    /**
     * DTO for popular menu items
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PopularItemDTO {
        
        /**
         * Ranking position (1-4)
         */
        private Integer rank;
        
        /**
         * Item name
         */
        private String itemName;
        
        /**
         * Number of orders for this item
         */
        private Long orderCount;
        
        /**
         * Maximum order count (for calculating percentage)
         */
        private Long maxOrderCount;
        
        /**
         * Percentage relative to the top item
         */
        private Double percentage;
        
        /**
         * Color for the progress bar
         */
        private String color;
        
        /**
         * Color gradient for the rank badge
         */
        private String badgeGradient;
    }
}
