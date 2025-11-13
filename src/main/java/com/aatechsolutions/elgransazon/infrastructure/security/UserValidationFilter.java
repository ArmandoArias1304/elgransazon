package com.aatechsolutions.elgransazon.infrastructure.security;

import com.aatechsolutions.elgransazon.domain.entity.Customer;
import com.aatechsolutions.elgransazon.domain.entity.Employee;
import com.aatechsolutions.elgransazon.domain.entity.Role;
import com.aatechsolutions.elgransazon.domain.repository.CustomerRepository;
import com.aatechsolutions.elgransazon.domain.repository.EmployeeRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filter to validate user enabled status on each request
 * Validates both employees and customers
 * Invalidates session if user is disabled
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserValidationFilter extends OncePerRequestFilter {

    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                    @NonNull HttpServletResponse response, 
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Only validate if user is authenticated
        if (authentication != null && authentication.isAuthenticated() 
            && !"anonymousUser".equals(authentication.getPrincipal().toString())) {
            
            String username = authentication.getName();
            
            // Check if user is a customer (has ROLE_CLIENT)
            boolean isCustomer = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(Role.CLIENT));
            
            if (isCustomer) {
                // Validate customer by email
                Optional<Customer> customerOpt = customerRepository.findByEmailIgnoreCase(username);
                
                if (customerOpt.isEmpty() || !customerOpt.get().getActive()) {
                    log.warn("Customer {} is disabled or doesn't exist. Invalidating session.", username);
                    
                    // Invalidate session and clear security context
                    new SecurityContextLogoutHandler().logout(request, response, authentication);
                    
                    // Redirect to client login
                    response.sendRedirect(request.getContextPath() + "/client/login");
                    return;
                }
            } else {
                // Validate employee by username
                Optional<Employee> employeeOpt = employeeRepository.findByUsername(username);
                
                if (employeeOpt.isEmpty() || !employeeOpt.get().getEnabled()) {
                    log.warn("Employee {} is disabled or doesn't exist. Invalidating session.", username);
                    
                    // Invalidate session and clear security context
                    new SecurityContextLogoutHandler().logout(request, response, authentication);
                    
                    // Redirect to employee login
                    response.sendRedirect(request.getContextPath() + "/login");
                    return;
                }
            }
        }
        
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        // Don't filter login pages, static resources, logout, and registration
        return path.startsWith("/login") || 
               path.startsWith("/client/login") ||
               path.startsWith("/client/register") ||
               path.startsWith("/css/") || 
               path.startsWith("/js/") || 
               path.startsWith("/images/") ||
               path.equals("/logout") ||
               path.equals("/perform_login");
    }
}
