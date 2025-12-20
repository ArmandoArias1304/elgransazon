package com.aatechsolutions.elgransazon.presentation.controller;

import com.aatechsolutions.elgransazon.application.service.EmployeeService;
import com.aatechsolutions.elgransazon.application.service.IngredientCategoryService;
import com.aatechsolutions.elgransazon.application.service.IngredientService;
import com.aatechsolutions.elgransazon.application.service.SupplierService;
import com.aatechsolutions.elgransazon.domain.entity.Employee;
import com.aatechsolutions.elgransazon.domain.entity.Ingredient;
import com.aatechsolutions.elgransazon.domain.entity.IngredientCategory;
import com.aatechsolutions.elgransazon.domain.entity.IngredientStockHistory;
import com.aatechsolutions.elgransazon.domain.entity.Supplier;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller for Ingredient management
 * Accessible by ADMIN and MANAGER roles
 */
@Controller
@RequestMapping("/admin/ingredients")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@RequiredArgsConstructor
@Slf4j
public class IngredientController {

    private final IngredientService ingredientService;
    private final IngredientCategoryService categoryService;
    private final SupplierService supplierService;
    private final EmployeeService employeeService;

    /**
     * List all ingredients with optional filters
     */
    @GetMapping
    public String listIngredients(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) Boolean active,
            Model model) {

        log.info("Listing ingredients with filters - search: {}, categoryId: {}, supplierId: {}, sortBy: {}, active: {}",
                search, categoryId, supplierId, sortBy, active);

        // Default to showing only active ingredients if no filter is specified
        Boolean activeFilter = (active != null) ? active : true;

        // Get filtered ingredients
        List<Ingredient> ingredients = ingredientService.searchWithAllFilters(
                search, categoryId, supplierId, sortBy, activeFilter);

        // Get statistics for alerts
        long lowStockCount = ingredientService.countLowStock();
        long outOfStockCount = ingredientService.countOutOfStock();
        
        // Get general statistics
        long activeCount = ingredientService.getActiveCount();
        long inactiveCount = ingredientService.getInactiveCount();
        long totalCount = activeCount + inactiveCount; // Total de ingredientes (activos + inactivos)

        // Get all categories for filter dropdown
        List<IngredientCategory> allCategories = categoryService.findAllActive();

        // Get all suppliers for filter dropdown
        List<Supplier> allSuppliers = supplierService.findAllActive();

        model.addAttribute("ingredients", ingredients);
        model.addAttribute("search", search);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("supplierId", supplierId);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("active", activeFilter);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("inactiveCount", inactiveCount);
        model.addAttribute("lowStockCount", lowStockCount);
        model.addAttribute("outOfStockCount", outOfStockCount);
        model.addAttribute("allCategories", allCategories);
        model.addAttribute("allSuppliers", allSuppliers);

        return "admin/ingredients/list";
    }

    /**
     * Show form to create a new ingredient
     */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        log.info("Showing ingredient create form");

        Ingredient ingredient = new Ingredient();
        ingredient.setActive(true);
        ingredient.setCurrency("MXN");

        model.addAttribute("ingredient", ingredient);
        model.addAttribute("isEdit", false);
        model.addAttribute("allCategories", categoryService.findAllActive());

        return "admin/ingredients/form";
    }

    /**
     * Create a new ingredient
     */
    @PostMapping
    public String createIngredient(
            @Valid @ModelAttribute("ingredient") Ingredient ingredient,
            @RequestParam Long categoryId,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        log.info("Creating ingredient: {} with categoryId: {}", ingredient.getName(), categoryId);

        // Manually set the category from categoryId
        try {
            IngredientCategory category = categoryService.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
            ingredient.setCategory(category);
        } catch (IllegalArgumentException e) {
            log.error("Category not found with id: {}", categoryId);
            result.rejectValue("category", "error.ingredient", "Debe seleccionar una categoría válida");
        }

        if (result.hasErrors()) {
            log.error("Validation errors creating ingredient");
            model.addAttribute("isEdit", false);
            model.addAttribute("allCategories", categoryService.findAllActive());
            return "admin/ingredients/form";
        }

        try {
            // Create the ingredient
            Ingredient savedIngredient = ingredientService.create(ingredient);
            
            // If the ingredient has initial stock and cost, create a history record
            if (savedIngredient.getCurrentStock() != null && 
                savedIngredient.getCurrentStock().compareTo(BigDecimal.ZERO) > 0 &&
                savedIngredient.getCostPerUnit() != null && 
                savedIngredient.getCostPerUnit().compareTo(BigDecimal.ZERO) > 0) {
                
                try {
                    String username = authentication.getName();
                    Employee currentEmployee = employeeService.findByUsername(username)
                            .orElse(null);
                    
                    ingredientService.addStock(
                            savedIngredient.getIdIngredient(),
                            savedIngredient.getCurrentStock(),
                            savedIngredient.getCostPerUnit(),
                            currentEmployee
                    );
                    
                    log.info("Initial stock history record created for ingredient: {}", savedIngredient.getName());
                } catch (Exception e) {
                    log.warn("Could not create initial stock history for ingredient {}: {}", 
                            savedIngredient.getName(), e.getMessage());
                    // Continue without failing the ingredient creation
                }
            }
            
            redirectAttributes.addFlashAttribute("successMessage", "Ingrediente creado exitosamente");
            return "redirect:/admin/ingredients";
        } catch (DataAccessException e) {
            // JPA wrapped validation errors (from @PrePersist/@PreUpdate)
            log.error("Data access error creating ingredient: {}", e.getMessage());
            String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            model.addAttribute("errorMessage", errorMsg);
            model.addAttribute("isEdit", false);
            model.addAttribute("allCategories", categoryService.findAllActive());
            return "admin/ingredients/form";
        } catch (IllegalStateException e) {
            // Stock validation errors from entity
            log.error("Stock validation error creating ingredient: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", false);
            model.addAttribute("allCategories", categoryService.findAllActive());
            return "admin/ingredients/form";
        } catch (IllegalArgumentException e) {
            log.error("Error creating ingredient: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", false);
            model.addAttribute("allCategories", categoryService.findAllActive());
            return "admin/ingredients/form";
        }
    }

    /**
     * Show form to edit an existing ingredient
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        log.info("Showing ingredient edit form for id: {}", id);

        try {
            Ingredient ingredient = ingredientService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Ingrediente no encontrado"));

            model.addAttribute("ingredient", ingredient);
            model.addAttribute("isEdit", true);
            model.addAttribute("allCategories", categoryService.findAllActive());

            return "admin/ingredients/form";
        } catch (IllegalArgumentException e) {
            log.error("Ingredient not found with id: {}", id);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/ingredients";
        }
    }

    /**
     * Update an existing ingredient
     */
    @PostMapping("/{id}")
    public String updateIngredient(
            @PathVariable Long id,
            @Valid @ModelAttribute("ingredient") Ingredient ingredient,
            @RequestParam Long categoryId,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        log.info("Updating ingredient with id: {} and categoryId: {}", id, categoryId);

        // Set the id from path variable to ensure it's preserved
        ingredient.setIdIngredient(id);

        // Manually set the category from categoryId
        try {
            IngredientCategory category = categoryService.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
            ingredient.setCategory(category);
        } catch (IllegalArgumentException e) {
            log.error("Category not found with id: {}", categoryId);
            result.rejectValue("category", "error.ingredient", "Debe seleccionar una categoría válida");
        }

        if (result.hasErrors()) {
            log.error("Validation errors updating ingredient");
            model.addAttribute("isEdit", true);
            model.addAttribute("allCategories", categoryService.findAllActive());
            return "admin/ingredients/form";
        }

        try {
            ingredientService.update(id, ingredient);
            redirectAttributes.addFlashAttribute("successMessage", "Ingrediente actualizado exitosamente");
            return "redirect:/admin/ingredients";
        } catch (DataAccessException e) {
            // JPA wrapped validation errors (from @PrePersist/@PreUpdate)
            log.error("Data access error updating ingredient: {}", e.getMessage());
            String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            ingredient.setIdIngredient(id); // Preserve ID for form
            model.addAttribute("errorMessage", errorMsg);
            model.addAttribute("isEdit", true);
            model.addAttribute("allCategories", categoryService.findAllActive());
            return "admin/ingredients/form";
        } catch (IllegalStateException e) {
            // Stock validation errors from entity
            log.error("Stock validation error updating ingredient: {}", e.getMessage());
            ingredient.setIdIngredient(id); // Preserve ID for form
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", true);
            model.addAttribute("allCategories", categoryService.findAllActive());
            return "admin/ingredients/form";
        } catch (IllegalArgumentException e) {
            log.error("Error updating ingredient: {}", e.getMessage());
            ingredient.setIdIngredient(id); // Preserve ID for form
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", true);
            model.addAttribute("allCategories", categoryService.findAllActive());
            return "admin/ingredients/form";
        }
    }

    /**
     * Deactivate an ingredient (soft delete)
     */
    @PostMapping("/{id}/delete")
    public String deleteIngredient(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("Deactivating ingredient with id: {}", id);

        try {
            ingredientService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Ingrediente desactivado exitosamente");
        } catch (IllegalArgumentException e) {
            log.error("Error deactivating ingredient: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/ingredients";
    }

    /**
     * Activate an ingredient
     */
    @PostMapping("/{id}/activate")
    public String activateIngredient(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("Activating ingredient with id: {}", id);

        try {
            ingredientService.activate(id);
            redirectAttributes.addFlashAttribute("successMessage", "Ingrediente activado exitosamente");
        } catch (IllegalArgumentException e) {
            log.error("Error activating ingredient: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/ingredients";
    }

    /**
     * Add stock to an existing ingredient
     */
    @PostMapping("/{id}/add-stock")
    public String addStock(
            @PathVariable Long id,
            @RequestParam BigDecimal quantityToAdd,
            @RequestParam BigDecimal costPerUnit,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        log.info("Adding stock to ingredient ID: {} - Quantity: {} - Cost: ${}", id, quantityToAdd, costPerUnit);

        try {
            if (quantityToAdd.compareTo(BigDecimal.ZERO) <= 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "La cantidad debe ser mayor a 0");
                return "redirect:/admin/ingredients/" + id + "/edit";
            }

            if (costPerUnit.compareTo(BigDecimal.ZERO) <= 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "El costo por unidad debe ser mayor a 0");
                return "redirect:/admin/ingredients/" + id + "/edit";
            }

            Employee currentEmployee = employeeService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

            ingredientService.addStock(id, quantityToAdd, costPerUnit, currentEmployee);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Stock agregado exitosamente. Se ha registrado en el historial.");

        } catch (Exception e) {
            log.error("Error al agregar stock al ingrediente {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error al agregar stock: " + e.getMessage());
        }

        return "redirect:/admin/ingredients/" + id + "/edit";
    }

    /**
     * Show stock history for an ingredient
     */
    @GetMapping("/{id}/stock-history")
    public String showStockHistory(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        log.info("Showing stock history for ingredient ID: {}", id);

        try {
            Ingredient ingredient = ingredientService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Ingrediente no encontrado"));

            List<IngredientStockHistory> history = ingredientService.getStockHistory(id);
            BigDecimal totalCost = ingredientService.getTotalCostByIngredient(id);

            model.addAttribute("ingredient", ingredient);
            model.addAttribute("history", history);
            model.addAttribute("totalCost", totalCost);

            return "admin/ingredients/stock-history";

        } catch (Exception e) {
            log.error("Error al cargar historial de stock: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error al cargar el historial: " + e.getMessage());
            return "redirect:/admin/ingredients";
        }
    }
}
