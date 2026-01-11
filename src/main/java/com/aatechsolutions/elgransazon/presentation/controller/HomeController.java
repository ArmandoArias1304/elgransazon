package com.aatechsolutions.elgransazon.presentation.controller;

import com.aatechsolutions.elgransazon.application.service.BusinessHoursService;
import com.aatechsolutions.elgransazon.application.service.EmployeeMonthlyStatsService;
import com.aatechsolutions.elgransazon.application.service.LicenseService;
import com.aatechsolutions.elgransazon.application.service.PromotionService;
import com.aatechsolutions.elgransazon.application.service.ReviewService;
import com.aatechsolutions.elgransazon.application.service.SocialNetworkService;
import com.aatechsolutions.elgransazon.domain.entity.BusinessHours;
import com.aatechsolutions.elgransazon.domain.entity.DayOfWeek;
import com.aatechsolutions.elgransazon.domain.entity.EmployeeMonthlyStats;
import com.aatechsolutions.elgransazon.domain.entity.Promotion;
import com.aatechsolutions.elgransazon.domain.entity.PromotionType;
import com.aatechsolutions.elgransazon.domain.entity.Review;
import com.aatechsolutions.elgransazon.domain.entity.SocialNetwork;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Controller for home/dashboard views
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class HomeController {

    private final BusinessHoursService businessHoursService;
    private final SocialNetworkService socialNetworkService;
    private final PromotionService promotionService;
    private final ReviewService reviewService;
    private final EmployeeMonthlyStatsService monthlyStatsService;
    private final LicenseService licenseService;

    /**
     * Display home/landing page with system configuration data
     * Redirects to /login if license doesn't include landing page access
     * 
     * @param authentication Spring Security authentication object
     * @param model Spring MVC model
     * @return landing view name or redirect to login
     */
    @GetMapping({"/", "/home"})
    public String home(Authentication authentication, Model model) {
        // Check if license has landing page access (WEB or ECOMMERCE)
        if (!licenseService.hasLandingPageAccess()) {
            log.info("License doesn't have landing page access (BASIC package). Redirecting to /login");
            return "redirect:/login";
        }

        if (authentication != null) {
            String username = authentication.getName();
            log.info("User {} accessed home page", username);
            model.addAttribute("username", username);
        }
        
        // Note: systemConfig is already provided by GlobalControllerAdvice
        // Load additional data
        List<BusinessHours> businessHours = businessHoursService.getAllBusinessHours();
        List<SocialNetwork> activeSocialNetworks = socialNetworkService.getAllActiveSocialNetworks();
        
        // Sort business hours by week order (Monday to Sunday)
        businessHours.sort((h1, h2) -> h1.getDayOfWeek().compareTo(h2.getDayOfWeek()));
        
        // Check if restaurant is currently open
        LocalDateTime now = LocalDateTime.now();
        java.time.DayOfWeek javaDayOfWeek = now.getDayOfWeek();
        DayOfWeek currentDay = DayOfWeek.valueOf(javaDayOfWeek.name());
        LocalTime currentTime = now.toLocalTime();
        
        boolean isOpen = false;
        BusinessHours todayHours = null;
        
        for (BusinessHours hours : businessHours) {
            if (hours.getDayOfWeek() == currentDay) {
                todayHours = hours;
                if (!hours.getIsClosed()) {
                    isOpen = hours.isOpenAt(currentTime);
                }
                break;
            }
        }
        
        model.addAttribute("businessHours", businessHours);
        model.addAttribute("socialNetworks", activeSocialNetworks);
        model.addAttribute("isOpen", isOpen);
        model.addAttribute("todayHours", todayHours);
        model.addAttribute("currentDay", currentDay);
        
        // Check if license has customer module access (ECOMMERCE only)
        boolean hasCustomerModule = licenseService.hasCustomerModuleAccess();
        model.addAttribute("hasCustomerModule", hasCustomerModule);
        
        // Load active promotions (one of each type)
        Promotion promoCombo = promotionService.findActiveByType(PromotionType.BUY_X_PAY_Y)
            .stream().findFirst().orElse(null);
        
        Promotion promoPercent = promotionService.findActiveByType(PromotionType.PERCENTAGE_DISCOUNT)
            .stream().findFirst().orElse(null);
        
        Promotion promoFixed = promotionService.findActiveByType(PromotionType.FIXED_AMOUNT_DISCOUNT)
            .stream().findFirst().orElse(null);
        
        model.addAttribute("promoCombo", promoCombo);
        model.addAttribute("promoPercent", promoPercent);
        model.addAttribute("promoFixed", promoFixed);
        
        // Load approved reviews for testimonials section
        List<Review> approvedReviews = reviewService.getApprovedReviews();
        model.addAttribute("approvedReviews", approvedReviews);
        
        // Load employees of the month
        try {
            EmployeeMonthlyStats waiterOfMonth = monthlyStatsService.getWaiterOfCurrentMonth().orElse(null);
            EmployeeMonthlyStats chefOfMonth = monthlyStatsService.getChefOfCurrentMonth().orElse(null);
            
            model.addAttribute("waiterOfMonth", waiterOfMonth);
            model.addAttribute("chefOfMonth", chefOfMonth);
            
            log.debug("Employees of the month loaded: waiter={}, chef={}", 
                     waiterOfMonth != null ? waiterOfMonth.getEmployee().getFullName() : "none",
                     chefOfMonth != null ? chefOfMonth.getEmployee().getFullName() : "none");
        } catch (Exception e) {
            log.error("Error loading employees of the month: {}", e.getMessage(), e);
            model.addAttribute("waiterOfMonth", null);
            model.addAttribute("chefOfMonth", null);
        }
        
        return "home/landing";
    }
}
