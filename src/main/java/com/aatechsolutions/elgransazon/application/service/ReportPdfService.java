package com.aatechsolutions.elgransazon.application.service;

import com.aatechsolutions.elgransazon.domain.entity.*;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating PDF reports
 * Generates 3 types of reports: Executive, Products, and Employees
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportPdfService {

    private final SystemConfigurationService systemConfigurationService;

    // Color palette - matching your theme
    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(56, 224, 123); // #38e07b
    private static final DeviceRgb PRIMARY_DARK = new DeviceRgb(43, 200, 102); // #2bc866
    private static final DeviceRgb DARK_COLOR = new DeviceRgb(45, 45, 45);
    private static final DeviceRgb GRAY_COLOR = new DeviceRgb(107, 114, 128);
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(249, 250, 251);
    private static final DeviceRgb WHITE = new DeviceRgb(255, 255, 255);

    /**
     * Generate Executive Report (All-in-one summary)
     */
    public byte[] generateExecutiveReport(
            java.util.List<Order> paidOrders,
            String startDate,
            String endDate,
            BigDecimal totalSales,
            long totalOrders,
            Map<String, BigDecimal> salesByCategory,
            Map<String, BigDecimal> salesByEmployee,
            Map<String, Long> ordersByPaymentMethod,
            java.util.List<Map<String, Object>> topSellingItems) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.LETTER);
        document.setMargins(40, 40, 40, 40);

        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // Header
        addHeader(document, boldFont, regularFont, "REPORTE EJECUTIVO DE VENTAS");
        addDateRange(document, regularFont, startDate, endDate);
        document.add(new Paragraph("\n"));

        // Summary Section
        addSectionTitle(document, boldFont, "Resumen General");
        Table summaryTable = new Table(new float[]{1, 1, 1});
        summaryTable.setWidth(UnitValue.createPercentValue(100));
        
        addSummaryCell(summaryTable, boldFont, regularFont, "Total de Ventas", 
            String.format("$%,.2f", totalSales));
        addSummaryCell(summaryTable, boldFont, regularFont, "Órdenes Pagadas", 
            String.valueOf(totalOrders));
        addSummaryCell(summaryTable, boldFont, regularFont, "Ticket Promedio", 
            totalOrders > 0 ? String.format("$%,.2f", totalSales.divide(BigDecimal.valueOf(totalOrders), 2, java.math.RoundingMode.HALF_UP)) : "$0.00");
        
        document.add(summaryTable);
        document.add(new Paragraph("\n"));

        // Top 5 Products
        addSectionTitle(document, boldFont, "Top 5 Productos Más Vendidos");
        Table productsTable = new Table(new float[]{3, 1, 1, 2});
        productsTable.setWidth(UnitValue.createPercentValue(100));
        addTableHeader(productsTable, boldFont, "Producto", "Cant.", "Cat.", "Total");
        
        topSellingItems.stream().limit(5).forEach(item -> {
            addTableRow(productsTable, regularFont,
                item.get("name").toString(),
                item.get("quantity").toString(),
                item.get("category").toString(),
                String.format("$%,.2f", item.get("total"))
            );
        });
        document.add(productsTable);
        document.add(new Paragraph("\n"));

        // Sales by Category
        addSectionTitle(document, boldFont, "Ventas por Categoría");
        Table categoryTable = new Table(new float[]{3, 2, 2});
        categoryTable.setWidth(UnitValue.createPercentValue(100));
        addTableHeader(categoryTable, boldFont, "Categoría", "Total", "% Part.");
        
        salesByCategory.entrySet().stream()
            .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
            .forEach(entry -> {
                BigDecimal percentage = totalSales.compareTo(BigDecimal.ZERO) > 0
                    ? entry.getValue().multiply(BigDecimal.valueOf(100)).divide(totalSales, 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
                addTableRow(categoryTable, regularFont,
                    entry.getKey(),
                    String.format("$%,.2f", entry.getValue()),
                    String.format("%.2f%%", percentage)
                );
            });
        document.add(categoryTable);
        document.add(new Paragraph("\n"));

        // Sales by Employee
        if (!salesByEmployee.isEmpty()) {
            addSectionTitle(document, boldFont, "Ventas por Empleado");
            Table employeeTable = new Table(new float[]{4, 2, 1});
            employeeTable.setWidth(UnitValue.createPercentValue(100));
            addTableHeader(employeeTable, boldFont, "Empleado", "Total Ventas", "Órdenes");
            
            Map<String, Long> ordersByEmployee = paidOrders.stream()
                .filter(o -> o.getEmployee() != null)
                .collect(Collectors.groupingBy(
                    o -> o.getEmployee().getNombre() + " " + o.getEmployee().getApellido(),
                    Collectors.counting()
                ));
            
            salesByEmployee.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .forEach(entry -> {
                    addTableRow(employeeTable, regularFont,
                        entry.getKey(),
                        String.format("$%,.2f", entry.getValue()),
                        String.valueOf(ordersByEmployee.getOrDefault(entry.getKey(), 0L))
                    );
                });
            document.add(employeeTable);
            document.add(new Paragraph("\n"));
        }

        // Payment Methods
        addSectionTitle(document, boldFont, "Métodos de Pago");
        Table paymentTable = new Table(new float[]{3, 2, 2});
        paymentTable.setWidth(UnitValue.createPercentValue(100));
        addTableHeader(paymentTable, boldFont, "Método", "Órdenes", "% Part.");
        
        ordersByPaymentMethod.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .forEach(entry -> {
                double percentage = totalOrders > 0 
                    ? (entry.getValue() * 100.0 / totalOrders) 
                    : 0.0;
                addTableRow(paymentTable, regularFont,
                    entry.getKey(),
                    String.valueOf(entry.getValue()),
                    String.format("%.2f%%", percentage)
                );
            });
        document.add(paymentTable);
        document.add(new Paragraph("\n"));

        // Web Orders Section (Orders created by customers)
        java.util.List<Order> webOrders = paidOrders.stream()
            .filter(order -> order.getCustomer() != null && order.getEmployee() == null)
            .collect(Collectors.toList());
        
        if (!webOrders.isEmpty()) {
            addSectionTitle(document, boldFont, "Pedidos Web (Clientes)");
            
            // Web orders summary
            BigDecimal webOrdersTotal = webOrders.stream()
                .map(order -> order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal webOrdersAverage = webOrders.size() > 0 
                ? webOrdersTotal.divide(BigDecimal.valueOf(webOrders.size()), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            
            Table webSummaryTable = new Table(new float[]{1, 1, 1});
            webSummaryTable.setWidth(UnitValue.createPercentValue(100));
            
            addSummaryCell(webSummaryTable, boldFont, regularFont, "Total Pedidos Web", 
                String.valueOf(webOrders.size()));
            addSummaryCell(webSummaryTable, boldFont, regularFont, "Ventas Totales", 
                String.format("$%,.2f", webOrdersTotal));
            addSummaryCell(webSummaryTable, boldFont, regularFont, "Ticket Promedio", 
                String.format("$%,.2f", webOrdersAverage));
            
            document.add(webSummaryTable);
            document.add(new Paragraph("\n"));
            
            // Web orders detail table
            Table webOrdersTable = new Table(new float[]{2, 3, 2, 2, 2});
            webOrdersTable.setWidth(UnitValue.createPercentValue(100));
            addTableHeader(webOrdersTable, boldFont, "Orden", "Cliente", "Tipo", "Total", "Pago");
            
            webOrders.stream()
                .sorted((o1, o2) -> {
                    LocalDateTime date1 = o1.getUpdatedAt() != null ? o1.getUpdatedAt() : o1.getCreatedAt();
                    LocalDateTime date2 = o2.getUpdatedAt() != null ? o2.getUpdatedAt() : o2.getCreatedAt();
                    return date2.compareTo(date1); // Most recent first
                })
                .forEach(order -> {
                    String customerName = order.getCustomer() != null 
                        ? order.getCustomer().getFullName()
                        : "N/A";
                    String orderType = order.getOrderType() != null 
                        ? order.getOrderType().getDisplayName()
                        : "N/A";
                    String paymentMethod = order.getPaymentMethod() != null
                        ? order.getPaymentMethod().getDisplayName()
                        : "N/A";
                    
                    addTableRow(webOrdersTable, regularFont,
                        order.getOrderNumber(),
                        customerName,
                        orderType,
                        String.format("$%,.2f", order.getTotal()),
                        paymentMethod
                    );
                });
            
            document.add(webOrdersTable);
            document.add(new Paragraph("\n"));
        }

        // Footer
        addFooter(document, regularFont);

        document.close();
        return baos.toByteArray();
    }

    /**
     * Generate Products Report (Top selling products detailed)
     */
    public byte[] generateProductsReport(
            java.util.List<Order> paidOrders,
            String startDate,
            String endDate,
            java.util.List<Map<String, Object>> topSellingItems) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.LETTER);
        document.setMargins(40, 40, 40, 40);

        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // Header
        addHeader(document, boldFont, regularFont, "REPORTE DE PRODUCTOS MÁS VENDIDOS");
        addDateRange(document, regularFont, startDate, endDate);
        document.add(new Paragraph("\n"));

        // Summary
        BigDecimal totalProductSales = topSellingItems.stream()
            .map(item -> (BigDecimal) item.get("total"))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int totalQuantity = topSellingItems.stream()
            .mapToInt(item -> (Integer) item.get("quantity"))
            .sum();

        addSectionTitle(document, boldFont, "Resumen");
        Table summaryTable = new Table(new float[]{1, 1, 1});
        summaryTable.setWidth(UnitValue.createPercentValue(100));
        
        addSummaryCell(summaryTable, boldFont, regularFont, "Total Productos Vendidos", 
            String.valueOf(totalQuantity));
        addSummaryCell(summaryTable, boldFont, regularFont, "Variedades Diferentes", 
            String.valueOf(topSellingItems.size()));
        addSummaryCell(summaryTable, boldFont, regularFont, "Ingresos Generados", 
            String.format("$%,.2f", totalProductSales));
        
        document.add(summaryTable);
        document.add(new Paragraph("\n"));

        // Products Table
        addSectionTitle(document, boldFont, "Detalle de Productos");
        Table table = new Table(new float[]{0.5f, 3, 2, 1, 2, 2});
        table.setWidth(UnitValue.createPercentValue(100));
        addTableHeader(table, boldFont, "#", "Producto", "Categoría", "Cant.", "Total", "% Part.");
        
        int rank = 1;
        for (Map<String, Object> item : topSellingItems) {
            BigDecimal itemTotal = (BigDecimal) item.get("total");
            double percentage = totalProductSales.compareTo(BigDecimal.ZERO) > 0
                ? itemTotal.multiply(BigDecimal.valueOf(100)).divide(totalProductSales, 2, java.math.RoundingMode.HALF_UP).doubleValue()
                : 0.0;
            
            addTableRow(table, regularFont,
                String.valueOf(rank++),
                item.get("name").toString(),
                item.get("category").toString(),
                item.get("quantity").toString(),
                String.format("$%,.2f", itemTotal),
                String.format("%.2f%%", percentage)
            );
        }
        document.add(table);

        // Footer
        addFooter(document, regularFont);

        document.close();
        return baos.toByteArray();
    }

    /**
     * Generate Employees Report (Employee performance by role)
     * Creates separate tables for each employee role with specific metrics
     * Excludes ONLINE (web) orders
     */
    public byte[] generateEmployeesReport(
            java.util.List<Order> paidOrders,
            String startDate,
            String endDate,
            Map<String, BigDecimal> salesByEmployee,
            BigDecimal totalSales) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.LETTER);
        document.setMargins(40, 40, 40, 40);

        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // Header
        addHeader(document, boldFont, regularFont, "REPORTE DE DESEMPEÑO POR EMPLEADO");
        addDateRange(document, regularFont, startDate, endDate);
        document.add(new Paragraph("\n"));

        // Note about excluded orders
        Paragraph note = new Paragraph("📌 Nota: Este reporte excluye pedidos creados por clientes (pedidos web)")
            .setFont(regularFont)
            .setFontSize(9)
            .setFontColor(GRAY_COLOR)
            .setItalic()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(10);
        document.add(note);

        // Get all unique employees from paid orders (including all roles)
        Set<Employee> allEmployees = new HashSet<>();
        for (Order order : paidOrders) {
            if (order.getEmployee() != null) allEmployees.add(order.getEmployee());
            if (order.getPreparedBy() != null) allEmployees.add(order.getPreparedBy());
            if (order.getPaidBy() != null) allEmployees.add(order.getPaidBy());
            if (order.getDeliveredBy() != null) allEmployees.add(order.getDeliveredBy());
        }

        // Group employees by their roles
        java.util.List<Employee> waiters = new ArrayList<>();
        java.util.List<Employee> chefs = new ArrayList<>();
        java.util.List<Employee> cashiers = new ArrayList<>();
        java.util.List<Employee> deliveryPersons = new ArrayList<>();
        java.util.List<Employee> admins = new ArrayList<>();
        
        for (Employee emp : allEmployees) {
            if (emp.hasRole(Role.WAITER)) waiters.add(emp);
            if (emp.hasRole(Role.CHEF)) chefs.add(emp);
            if (emp.hasRole(Role.CASHIER)) cashiers.add(emp);
            if (emp.hasRole(Role.DELIVERY)) deliveryPersons.add(emp);
            if (emp.hasRole(Role.ADMIN) || emp.hasRole(Role.MANAGER)) admins.add(emp);
        }

        // Summary
        addSectionTitle(document, boldFont, "Resumen General");
        Table summaryTable = new Table(new float[]{1, 1, 1});
        summaryTable.setWidth(UnitValue.createPercentValue(100));
        
        long totalEmployees = allEmployees.size();
        addSummaryCell(summaryTable, boldFont, regularFont, "Empleados Activos", 
            String.valueOf(totalEmployees));
        addSummaryCell(summaryTable, boldFont, regularFont, "Total Ventas (Sin Web)", 
            String.format("$%,.2f", totalSales));
        addSummaryCell(summaryTable, boldFont, regularFont, "Promedio por Empleado", 
            totalEmployees > 0 ? String.format("$%,.2f", totalSales.divide(BigDecimal.valueOf(totalEmployees), 2, java.math.RoundingMode.HALF_UP)) : "$0.00");
        
        document.add(summaryTable);
        document.add(new Paragraph("\n"));

        // === MESEROS (Waiters) ===
        if (!waiters.isEmpty()) {
            addSectionTitle(document, boldFont, "👔 Meseros - Rendimiento de Ventas");
            Table waitersTable = new Table(new float[]{0.5f, 3, 2, 1, 2, 1.5f});
            waitersTable.setWidth(UnitValue.createPercentValue(100));
            addTableHeader(waitersTable, boldFont, "#", "Nombre", "Ventas", "Órdenes", "Promedio", "Propinas");
            
            int rank = 1;
            
            // Sort by sales
            waiters.sort((a, b) -> {
                BigDecimal salesA = salesByEmployee.getOrDefault(a.getFullName(), BigDecimal.ZERO);
                BigDecimal salesB = salesByEmployee.getOrDefault(b.getFullName(), BigDecimal.ZERO);
                return salesB.compareTo(salesA);
            });
            
            for (Employee emp : waiters) {
                String empName = emp.getFullName();
                BigDecimal sales = salesByEmployee.getOrDefault(empName, BigDecimal.ZERO);
                
                long orders = paidOrders.stream()
                    .filter(o -> o.getEmployee() != null && o.getEmployee().getIdEmpleado().equals(emp.getIdEmpleado()))
                    .count();
                    
                BigDecimal avgPerOrder = orders > 0 ? sales.divide(BigDecimal.valueOf(orders), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
                
                BigDecimal tips = paidOrders.stream()
                    .filter(o -> o.getEmployee() != null && o.getEmployee().getIdEmpleado().equals(emp.getIdEmpleado()))
                    .map(o -> o.getTip() != null ? o.getTip() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                addTableRow(waitersTable, regularFont,
                    String.valueOf(rank++),
                    empName,
                    String.format("$%,.2f", sales),
                    String.valueOf(orders),
                    String.format("$%,.2f", avgPerOrder),
                    String.format("$%,.2f", tips)
                );
            }
            document.add(waitersTable);
            document.add(new Paragraph("\n"));
        }

        // === CHEFS ===
        if (!chefs.isEmpty()) {
            addSectionTitle(document, boldFont, "👨‍🍳 Chefs - Órdenes Preparadas");
            Table chefsTable = new Table(new float[]{0.5f, 3, 2, 2, 2});
            chefsTable.setWidth(UnitValue.createPercentValue(100));
            addTableHeader(chefsTable, boldFont, "#", "Nombre", "Órdenes Preparadas", "Platos Totales", "Promedio/Orden");
            
            int rank = 1;
            
            for (Employee emp : chefs) {
                // Count orders where this chef was preparedBy
                long ordersPrep = paidOrders.stream()
                    .filter(o -> o.getPreparedBy() != null && o.getPreparedBy().getIdEmpleado().equals(emp.getIdEmpleado()))
                    .count();
                
                // Count total dishes (order details)
                long totalDishes = paidOrders.stream()
                    .filter(o -> o.getPreparedBy() != null && o.getPreparedBy().getIdEmpleado().equals(emp.getIdEmpleado()))
                    .mapToLong(o -> o.getOrderDetails().size())
                    .sum();
                
                double avgDishesPerOrder = ordersPrep > 0 ? (double) totalDishes / ordersPrep : 0.0;
                
                addTableRow(chefsTable, regularFont,
                    String.valueOf(rank++),
                    emp.getFullName(),
                    String.valueOf(ordersPrep),
                    String.valueOf(totalDishes),
                    String.format("%.1f", avgDishesPerOrder)
                );
            }
            document.add(chefsTable);
            document.add(new Paragraph("\n"));
        }

        // === CAJEROS (Cashiers) ===
        if (!cashiers.isEmpty()) {
            addSectionTitle(document, boldFont, "💰 Cajeros - Cobros Realizados");
            Table cashiersTable = new Table(new float[]{0.5f, 3, 2, 1.5f, 2});
            cashiersTable.setWidth(UnitValue.createPercentValue(100));
            addTableHeader(cashiersTable, boldFont, "#", "Nombre", "Total Cobrado", "Órdenes", "Ticket Prom.");
            
            int rank = 1;
            
            // Sort by total collected
            cashiers.sort((a, b) -> {
                BigDecimal salesA = salesByEmployee.getOrDefault(a.getFullName(), BigDecimal.ZERO);
                BigDecimal salesB = salesByEmployee.getOrDefault(b.getFullName(), BigDecimal.ZERO);
                return salesB.compareTo(salesA);
            });
            
            for (Employee emp : cashiers) {
                String empName = emp.getFullName();
                BigDecimal totalCollected = salesByEmployee.getOrDefault(empName, BigDecimal.ZERO);
                
                long orders = paidOrders.stream()
                    .filter(o -> o.getEmployee() != null && o.getEmployee().getIdEmpleado().equals(emp.getIdEmpleado()))
                    .count();
                    
                BigDecimal avgTicket = orders > 0 ? totalCollected.divide(BigDecimal.valueOf(orders), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
                
                addTableRow(cashiersTable, regularFont,
                    String.valueOf(rank++),
                    empName,
                    String.format("$%,.2f", totalCollected),
                    String.valueOf(orders),
                    String.format("$%,.2f", avgTicket)
                );
            }
            document.add(cashiersTable);
            document.add(new Paragraph("\n"));
        }

        // === REPARTIDORES (Delivery) ===
        if (!deliveryPersons.isEmpty()) {
            addSectionTitle(document, boldFont, "🚗 Repartidores - Entregas Realizadas");
            Table deliveryTable = new Table(new float[]{0.5f, 3, 1.5f, 2, 1.5f});
            deliveryTable.setWidth(UnitValue.createPercentValue(100));
            addTableHeader(deliveryTable, boldFont, "#", "Nombre", "Entregas", "Total Entregado", "Propinas");
            
            int rank = 1;
            
            for (Employee emp : deliveryPersons) {
                // Count DELIVERY orders delivered by this person
                long deliveries = paidOrders.stream()
                    .filter(o -> o.getOrderType() == OrderType.DELIVERY)
                    .filter(o -> o.getDeliveredBy() != null && o.getDeliveredBy().getIdEmpleado().equals(emp.getIdEmpleado()))
                    .count();
                
                BigDecimal totalDelivered = paidOrders.stream()
                    .filter(o -> o.getOrderType() == OrderType.DELIVERY)
                    .filter(o -> o.getDeliveredBy() != null && o.getDeliveredBy().getIdEmpleado().equals(emp.getIdEmpleado()))
                    .map(o -> o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal tips = paidOrders.stream()
                    .filter(o -> o.getOrderType() == OrderType.DELIVERY)
                    .filter(o -> o.getDeliveredBy() != null && o.getDeliveredBy().getIdEmpleado().equals(emp.getIdEmpleado()))
                    .map(o -> o.getTip() != null ? o.getTip() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                addTableRow(deliveryTable, regularFont,
                    String.valueOf(rank++),
                    emp.getFullName(),
                    String.valueOf(deliveries),
                    String.format("$%,.2f", totalDelivered),
                    String.format("$%,.2f", tips)
                );
            }
            document.add(deliveryTable);
            document.add(new Paragraph("\n"));
        }

        // === ADMINISTRADORES y GERENTES ===
        if (!admins.isEmpty()) {
            admins = admins.stream().distinct().collect(Collectors.toList());
            
            addSectionTitle(document, boldFont, "👨‍💼 Administradores y Gerentes - Gestión");
            Table adminsTable = new Table(new float[]{0.5f, 3, 2, 1.5f, 2});
            adminsTable.setWidth(UnitValue.createPercentValue(100));
            addTableHeader(adminsTable, boldFont, "#", "Nombre", "Rol", "Órdenes", "Total Gestionado");
            
            int rank = 1;
            
            for (Employee emp : admins) {
                String role = emp.hasRole(Role.ADMIN) ? "Administrador" : "Gerente";
                String empName = emp.getFullName();
                BigDecimal sales = salesByEmployee.getOrDefault(empName, BigDecimal.ZERO);
                
                long orders = paidOrders.stream()
                    .filter(o -> o.getEmployee() != null && o.getEmployee().getIdEmpleado().equals(emp.getIdEmpleado()))
                    .count();
                
                addTableRow(adminsTable, regularFont,
                    String.valueOf(rank++),
                    empName,
                    role,
                    String.valueOf(orders),
                    String.format("$%,.2f", sales)
                );
            }
            document.add(adminsTable);
            document.add(new Paragraph("\n"));
        }

        // Footer
        addFooter(document, regularFont);

        document.close();
        return baos.toByteArray();
    }

    // ========== Helper Methods ==========

    private void addHeader(Document document, PdfFont boldFont, PdfFont regularFont, String title) {
        SystemConfiguration config = systemConfigurationService.getConfiguration();
        
        // Restaurant name with modern styling
        Paragraph restaurantName = new Paragraph(config.getRestaurantName())
            .setFont(boldFont)
            .setFontSize(24)
            .setFontColor(PRIMARY_COLOR)
            .setTextAlignment(TextAlignment.CENTER)
            .setBold()
            .setMarginBottom(2);
        document.add(restaurantName);

        // Subtitle line
        Paragraph subtitle = new Paragraph("Sistema de Reportes")
            .setFont(regularFont)
            .setFontSize(9)
            .setFontColor(GRAY_COLOR)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(15);
        document.add(subtitle);

        // Report title with background
        Table titleTable = new Table(1);
        titleTable.setWidth(UnitValue.createPercentValue(100));
        
        Cell titleCell = new Cell()
            .add(new Paragraph(title)
                .setFont(boldFont)
                .setFontSize(16)
                .setFontColor(WHITE)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER))
            .setBackgroundColor(PRIMARY_COLOR)
            .setPadding(12)
            .setBorder(Border.NO_BORDER)
            .setMarginBottom(5);
        
        titleTable.addCell(titleCell);
        document.add(titleTable);
    }

    private void addDateRange(Document document, PdfFont font, String startDate, String endDate) {
        String dateRange;
        if (startDate != null && !startDate.isEmpty()) {
            dateRange = "📅 Periodo: " + startDate + " al " + (endDate != null && !endDate.isEmpty() ? endDate : startDate);
        } else {
            dateRange = "📅 Periodo: Todos los registros";
        }
        
        // Date range in a subtle box
        Table dateTable = new Table(1);
        dateTable.setWidth(UnitValue.createPercentValue(100));
        
        Cell dateCell = new Cell()
            .add(new Paragraph(dateRange)
                .setFont(font)
                .setFontSize(10)
                .setFontColor(GRAY_COLOR)
                .setTextAlignment(TextAlignment.CENTER))
            .setBackgroundColor(LIGHT_GRAY)
            .setPadding(8)
            .setBorder(Border.NO_BORDER)
            .setMarginBottom(15);
        
        dateTable.addCell(dateCell);
        document.add(dateTable);
    }

    private void addSectionTitle(Document document, PdfFont boldFont, String title) {
        // Section title with left border accent
        Table sectionTable = new Table(new float[]{0.05f, 0.95f});
        sectionTable.setWidth(UnitValue.createPercentValue(100));
        
        // Accent bar
        Cell accentCell = new Cell()
            .setBackgroundColor(PRIMARY_COLOR)
            .setBorder(Border.NO_BORDER)
            .setHeight(20);
        
        // Title text
        Cell titleCell = new Cell()
            .add(new Paragraph(title)
                .setFont(boldFont)
                .setFontSize(13)
                .setFontColor(DARK_COLOR)
                .setBold())
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setBorder(Border.NO_BORDER)
            .setPaddingLeft(10);
        
        sectionTable.addCell(accentCell);
        sectionTable.addCell(titleCell);
        
        document.add(sectionTable.setMarginTop(10).setMarginBottom(10));
    }

    private void addSummaryCell(Table table, PdfFont boldFont, PdfFont regularFont, String label, String value) {
        Cell cell = new Cell()
            .setBorder(Border.NO_BORDER)
            .setBackgroundColor(LIGHT_GRAY)
            .setPadding(15)
            .setMarginRight(5);
        
        // Label
        cell.add(new Paragraph(label)
            .setFont(regularFont)
            .setFontSize(9)
            .setFontColor(GRAY_COLOR)
            .setMarginBottom(8)
            .setTextAlignment(TextAlignment.CENTER));
        
        // Value
        cell.add(new Paragraph(value)
            .setFont(boldFont)
            .setFontSize(18)
            .setFontColor(PRIMARY_DARK)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER));
        
        table.addCell(cell);
    }

    private void addTableHeader(Table table, PdfFont boldFont, String... headers) {
        for (String header : headers) {
            Cell cell = new Cell()
                .add(new Paragraph(header)
                    .setFont(boldFont)
                    .setFontSize(9)
                    .setBold())
                .setBackgroundColor(PRIMARY_COLOR)
                .setFontColor(WHITE)
                .setPadding(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(Border.NO_BORDER);
            table.addHeaderCell(cell);
        }
    }

    private void addTableRow(Table table, PdfFont font, String... values) {
        for (int i = 0; i < values.length; i++) {
            Cell cell = new Cell()
                .add(new Paragraph(values[i])
                    .setFont(font)
                    .setFontSize(9))
                .setPadding(8)
                .setBackgroundColor(i % 2 == 0 ? WHITE : LIGHT_GRAY)
                .setBorder(new SolidBorder(new DeviceRgb(229, 231, 235), 0.5f));
            
            // Align numbers to the right
            if (values[i].contains("$") || values[i].contains("%") || values[i].matches("\\d+")) {
                cell.setTextAlignment(TextAlignment.RIGHT);
            } else if (i == 0 && values[i].matches("\\d+")) {
                // Row number centered
                cell.setTextAlignment(TextAlignment.CENTER);
            }
            
            table.addCell(cell);
        }
    }

    private void addFooter(Document document, PdfFont font) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm:ss");
        String generatedDate = LocalDateTime.now().format(formatter);
        
        // Footer with modern design
        Table footerTable = new Table(1);
        footerTable.setWidth(UnitValue.createPercentValue(100));
        
        Cell footerCell = new Cell()
            .add(new Paragraph("📄 Reporte generado el " + generatedDate)
                .setFont(font)
                .setFontSize(8)
                .setFontColor(GRAY_COLOR)
                .setTextAlignment(TextAlignment.CENTER))
            .setBackgroundColor(LIGHT_GRAY)
            .setPadding(10)
            .setBorder(Border.NO_BORDER)
            .setMarginTop(20);
        
        footerTable.addCell(footerCell);
        document.add(footerTable);
    }
}
