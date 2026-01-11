package com.aatechsolutions.elgransazon.presentation.controller;

import com.aatechsolutions.elgransazon.application.service.LicenseService;
import com.aatechsolutions.elgransazon.domain.entity.LicenseEvent;
import com.aatechsolutions.elgransazon.domain.entity.SystemError;
import com.aatechsolutions.elgransazon.domain.entity.SystemLicense;
import com.aatechsolutions.elgransazon.domain.repository.CustomerRepository;
import com.aatechsolutions.elgransazon.domain.repository.EmployeeRepository;
import com.aatechsolutions.elgransazon.domain.repository.ItemMenuRepository;
import com.aatechsolutions.elgransazon.domain.repository.OrderRepository;
import com.aatechsolutions.elgransazon.domain.repository.SystemErrorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller for programmer/vendor dashboard
 * Access restricted to PROGRAMMER role only
 */
@Controller
@RequestMapping("/programmer")
@PreAuthorize("hasRole('PROGRAMMER')")
@RequiredArgsConstructor
@Slf4j
public class ProgrammerController {

    private final LicenseService licenseService;
    private final SystemErrorRepository errorRepository;
    private final EmployeeRepository employeeRepository;
    private final ItemMenuRepository itemMenuRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    /**
     * Programmer dashboard
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        log.info("Programmer {} accessed dashboard", authentication.getName());

        SystemLicense license = licenseService.getLicense();
        
        if (license == null) {
            model.addAttribute("noLicense", true);
            return "programmer/dashboard";
        }

        // License information
        model.addAttribute("license", license);
        model.addAttribute("daysLeft", license.daysUntilExpiration());
        model.addAttribute("daysActive", license.daysActive());
        model.addAttribute("isExpired", license.isExpired());
        model.addAttribute("needsWarning", license.daysUntilExpiration() <= 5);

        // System statistics
        long totalEmployees = employeeRepository.count();
        long totalMenuItems = itemMenuRepository.count();
        long totalOrders = orderRepository.count();
        long totalCustomers = customerRepository.count();

        model.addAttribute("totalEmployees", totalEmployees);
        model.addAttribute("totalMenuItems", totalMenuItems);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalCustomers", totalCustomers);

        // Recent errors (last 7 days)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<SystemError> recentErrors = errorRepository.findByOccurredAtBetweenOrderByOccurredAtDesc(
            sevenDaysAgo, LocalDateTime.now()
        );
        
        long unresolvedCount = recentErrors.stream().filter(e -> !e.getResolved()).count();
        long criticalCount = recentErrors.stream()
            .filter(e -> e.getSeverity() == SystemError.Severity.CRITICAL && !e.getResolved())
            .count();

        model.addAttribute("recentErrors", recentErrors);
        model.addAttribute("unresolvedErrorCount", unresolvedCount);
        model.addAttribute("criticalErrorCount", criticalCount);

        // License events
        List<LicenseEvent> events = licenseService.getRecentEvents(10);
        model.addAttribute("licenseEvents", events);

        // Financial summary
        Double totalRevenue = licenseService.getTotalRevenue();
        List<LicenseEvent> renewalsWithAmount = licenseService.getRenewalEventsWithAmount();
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("renewalsWithAmount", renewalsWithAmount);

        return "programmer/dashboard";
    }

    /**
     * Renew license
     */
    @PostMapping("/renew")
    public String renewLicense(@RequestParam int months,
                              @RequestParam(required = false) Double amount,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            licenseService.renewLicense(months, amount, username);
            
            String action = months > 0 ? "renovada" : "ajustada";
            String monthsText = Math.abs(months) + " mes(es)";
            redirectAttributes.addFlashAttribute("successMessage", 
                "Licencia " + action + " exitosamente por " + monthsText);
            
            log.info("License renewed/adjusted for {} months by {}", months, username);
        } catch (Exception e) {
            log.error("Error renewing license", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error al renovar la licencia: " + e.getMessage());
        }

        return "redirect:/programmer/dashboard";
    }

    /**
     * Suspend license
     */
    @PostMapping("/suspend")
    public String suspendLicense(@RequestParam(required = false) String reason,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            licenseService.suspendLicense(username, reason);
            
            redirectAttributes.addFlashAttribute("warningMessage", 
                "Licencia suspendida exitosamente");
            
            log.info("License suspended by {}", username);
        } catch (Exception e) {
            log.error("Error suspending license", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error al suspender la licencia: " + e.getMessage());
        }

        return "redirect:/programmer/dashboard";
    }

    /**
     * Reactivate license
     */
    @PostMapping("/reactivate")
    public String reactivateLicense(Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            licenseService.reactivateLicense(username);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Licencia reactivada exitosamente");
            
            log.info("License reactivated by {}", username);
        } catch (Exception e) {
            log.error("Error reactivating license", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error al reactivar la licencia: " + e.getMessage());
        }

        return "redirect:/programmer/dashboard";
    }

    /**
     * Change package type
     */
    @PostMapping("/change-package")
    public String changePackage(@RequestParam String packageType,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            SystemLicense.PackageType newPackage = SystemLicense.PackageType.valueOf(packageType);
            licenseService.changePackageType(newPackage, username);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Paquete cambiado exitosamente a " + newPackage.getDisplayName());
            
            log.info("Package changed to {} by {}", newPackage, username);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid package change attempt: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error changing package", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error al cambiar el paquete: " + e.getMessage());
        }

        return "redirect:/programmer/dashboard";
    }

    /**
     * Update license notes
     */
    @PostMapping("/update-notes")
    public String updateNotes(@RequestParam String notes,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            licenseService.updateNotes(notes, username);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Notas actualizadas exitosamente");
            
            log.info("License notes updated by {}", username);
        } catch (Exception e) {
            log.error("Error updating notes", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error al actualizar las notas: " + e.getMessage());
        }

        return "redirect:/programmer/dashboard";
    }

    /**
     * Update license information
     */
    @PostMapping("/update-info")
    public String updateLicenseInfo(@RequestParam String ownerName,
                                   @RequestParam String ownerEmail,
                                   @RequestParam String ownerPhone,
                                   @RequestParam(required = false) String ownerRfc,
                                   @RequestParam String restaurantName,
                                   @RequestParam(required = false) Integer maxUsers,
                                   @RequestParam int maxBranches,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            licenseService.updateLicenseInfo(
                ownerName,
                ownerEmail,
                ownerPhone,
                ownerRfc,
                restaurantName,
                maxUsers,
                maxBranches,
                username
            );
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Información de licencia actualizada exitosamente");
            
            log.info("License info updated by {}", username);
        } catch (Exception e) {
            log.error("Error updating license info", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error al actualizar la información: " + e.getMessage());
        }

        return "redirect:/programmer/dashboard";
    }

    /**
     * View error details
     */
    @GetMapping("/errors/{id}")
    public String viewError(@PathVariable Long id, Model model) {
        SystemError error = errorRepository.findById(id).orElse(null);
        
        if (error == null) {
            return "redirect:/programmer/dashboard";
        }

        model.addAttribute("error", error);
        return "programmer/error-detail";
    }

    /**
     * Mark error as resolved
     */
    @PostMapping("/errors/{id}/resolve")
    public String resolveError(@PathVariable Long id,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            SystemError error = errorRepository.findById(id).orElse(null);
            
            if (error != null) {
                error.markAsResolved(authentication.getName());
                errorRepository.save(error);
                
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Error marcado como resuelto");
            }
        } catch (Exception e) {
            log.error("Error resolving error", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error al marcar como resuelto: " + e.getMessage());
        }

        return "redirect:/programmer/dashboard";
    }

    /**
     * Create initial license
     */
    @PostMapping("/create-license")
    public String createLicense(@RequestParam String licenseKey,
                               @RequestParam String packageType,
                               @RequestParam String billingCycle,
                               @RequestParam int months,
                               @RequestParam String ownerName,
                               @RequestParam String ownerEmail,
                               @RequestParam String ownerPhone,
                               @RequestParam String ownerRfc,
                               @RequestParam String restaurantName,
                               @RequestParam(required = false) Integer maxUsers,
                               @RequestParam(required = false, defaultValue = "1") int maxBranches,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            // Check if license already exists
            SystemLicense existingLicense = licenseService.getLicense();
            if (existingLicense != null) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Ya existe una licencia en el sistema. Use renovar para extender la vigencia.");
                return "redirect:/programmer/dashboard";
            }

            String username = authentication.getName();
            licenseService.createInitialLicense(
                licenseKey,
                packageType,
                billingCycle,
                months,
                ownerName,
                ownerEmail,
                ownerPhone,
                ownerRfc,
                restaurantName,
                maxUsers,
                maxBranches,
                username
            );
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Licencia creada exitosamente para " + months + " mes(es)");
            
            log.info("Initial license created by {}", username);
        } catch (Exception e) {
            log.error("Error creating initial license", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error al crear la licencia: " + e.getMessage());
        }

        return "redirect:/programmer/dashboard";
    }
}
