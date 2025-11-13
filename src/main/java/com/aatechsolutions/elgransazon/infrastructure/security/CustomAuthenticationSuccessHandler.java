package com.aatechsolutions.elgransazon.infrastructure.security;

import com.aatechsolutions.elgransazon.application.service.CustomerService;
import com.aatechsolutions.elgransazon.application.service.EmployeeService;
import com.aatechsolutions.elgransazon.application.service.EmailVerificationService;
import com.aatechsolutions.elgransazon.domain.entity.Customer;
import com.aatechsolutions.elgransazon.domain.entity.Role;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

/**
 * Custom authentication success handler
 * Redirects users to role-specific pages after successful login
 * and updates last access timestamp
 */
@Component
@Slf4j
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final EmployeeService employeeService;
    private final CustomerService customerService;
    private final EmailVerificationService emailVerificationService;
    
    public CustomAuthenticationSuccessHandler(@Lazy EmployeeService employeeService,
                                             @Lazy CustomerService customerService,
                                             @Lazy EmailVerificationService emailVerificationService) {
        this.employeeService = employeeService;
        this.customerService = customerService;
        this.emailVerificationService = emailVerificationService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        String username = authentication.getName();
        log.info("User {} logged in successfully", username);
        
        // Check if it's a customer (has ROLE_CLIENT) or employee
        boolean isCustomer = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(Role.CLIENT));
        
        // Get the login URL to validate if user is using correct login page
        String loginUrl = request.getRequestURI();
        String referer = request.getHeader("Referer");
        
        log.debug("Login URL: {}, Referer: {}", loginUrl, referer);
        
        // Validate: Customer trying to login via employee login
        if (isCustomer && (referer != null && !referer.contains("/client/login"))) {
            log.warn("Customer {} attempted to login via employee login page", username);
            // Invalidate the session
            request.getSession().invalidate();
            // Redirect to employee login with error
            response.sendRedirect("/login?error=clientAttempt");
            return;
        }
        
        // Validate: Employee trying to login via client login
        if (!isCustomer && (referer != null && referer.contains("/client/login"))) {
            log.warn("Employee {} attempted to login via client login page", username);
            // Invalidate the session
            request.getSession().invalidate();
            // Redirect to client login with error
            response.sendRedirect("/client/login?error=employeeAttempt");
            return;
        }
        
        // Validate: Customer email verification
        if (isCustomer) {
            try {
                Customer customer = customerService.findByUsernameOrEmail(username)
                        .orElseThrow(() -> new RuntimeException("Customer not found"));
                
                // Check if email is verified
                if (!Boolean.TRUE.equals(customer.getEmailVerified())) {
                    log.warn("Customer {} attempted to login without verifying email", username);
                    
                    // Try to send verification email (respects the token logic)
                    boolean emailSent = emailVerificationService.createOrReuseToken(customer);
                    
                    // Invalidate the session
                    request.getSession().invalidate();
                    
                    // Redirect to client login with error
                    if (emailSent) {
                        response.sendRedirect("/client/login?error=emailNotVerified&emailSent=true");
                    } else {
                        response.sendRedirect("/client/login?error=emailNotVerified&emailSent=false");
                    }
                    return;
                }
            } catch (Exception e) {
                log.error("Error checking email verification for customer {}", username, e);
                // Continue with normal flow if check fails
            }
        }
        
        // Update last access timestamp
        try {
            if (isCustomer) {
                customerService.updateLastAccess(username);
                log.debug("Updated last access for customer {}", username);
            } else {
                employeeService.updateLastAccess(username);
                log.debug("Updated last access for employee {}", username);
            }
        } catch (Exception e) {
            log.error("Error updating last access for user {}", username, e);
            // Don't fail login if last access update fails
        }
        
        String targetUrl = determineTargetUrl(authentication);
        log.debug("Redirecting user {} to {}", username, targetUrl);
        
        response.sendRedirect(targetUrl);
    }

    /**
     * Determine the target URL based on user's roles
     * Priority: CLIENT > ADMIN > MANAGER > CHEF > WAITER > CASHIER > DELIVERY > default
     */
    private String determineTargetUrl(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        
        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            log.debug("Checking role: {}", role);
            
            if (Role.CLIENT.equals(role)) {
                return "/client/dashboard";
            } else if (Role.ADMIN.equals(role)) {
                return "/admin/dashboard";
            } else if (Role.MANAGER.equals(role)) {
                return "/admin/dashboard";
            } else if (Role.CHEF.equals(role)) {
                return "/chef/dashboard";
            } else if (Role.WAITER.equals(role)) {
                return "/waiter/dashboard";
            } else if (Role.CASHIER.equals(role)) {
                return "/cashier/dashboard";
            } else if (Role.DELIVERY.equals(role)) {
                return "/delivery/dashboard";
            }
        }
        
        // Default redirect
        log.warn("No specific role found for user {}, redirecting to default home", authentication.getName());
        return "/home";
    }
}
