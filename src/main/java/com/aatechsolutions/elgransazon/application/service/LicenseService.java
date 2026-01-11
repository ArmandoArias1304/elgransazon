package com.aatechsolutions.elgransazon.application.service;

import com.aatechsolutions.elgransazon.domain.entity.LicenseEvent;
import com.aatechsolutions.elgransazon.domain.entity.SystemLicense;
import com.aatechsolutions.elgransazon.domain.entity.SystemLicense.LicenseStatus;
import com.aatechsolutions.elgransazon.domain.repository.LicenseEventRepository;
import com.aatechsolutions.elgransazon.domain.repository.SystemLicenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing system licenses
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LicenseService {

    private final SystemLicenseRepository licenseRepository;
    private final LicenseEventRepository eventRepository;

    /**
     * Get the system license (there should be only one)
     */
    public SystemLicense getLicense() {
        return licenseRepository.findFirstByOrderByIdAsc().orElse(null);
    }

    /**
     * Get license by ID
     */
    public Optional<SystemLicense> getLicenseById(Long id) {
        return licenseRepository.findById(id);
    }

    /**
     * Check if the license is valid (active and not expired)
     */
    public boolean isLicenseValid() {
        SystemLicense license = getLicense();
        if (license == null) {
            log.warn("No license found in the system");
            return false;
        }

        boolean isValid = !license.isExpired() && license.getStatus() == LicenseStatus.ACTIVE;
        
        if (!isValid) {
            log.warn("License is not valid. Status: {}, Expired: {}", 
                license.getStatus(), license.isExpired());
        }

        return isValid;
    }

    /**
     * Get comprehensive license information
     */
    public Map<String, Object> getLicenseInfo() {
        SystemLicense license = getLicense();
        if (license == null) {
            log.warn("No license found");
            return null;
        }

        Map<String, Object> info = new HashMap<>();
        info.put("license", license);
        info.put("daysLeft", license.daysUntilExpiration());
        info.put("daysActive", license.daysActive());
        info.put("isExpired", license.isExpired());
        info.put("needsWarning", license.daysUntilExpiration() <= 5);
        info.put("isCritical", license.daysUntilExpiration() <= 3);

        return info;
    }

    /**
     * Renew the license for specified months (can be negative to subtract time)
     */
    @Transactional
    public void renewLicense(int months, Double amount, String performedBy) {
        SystemLicense license = getLicense();
        if (license == null) {
            throw new RuntimeException("No license found to renew");
        }

        LocalDate newExpiration = license.getExpirationDate().plusMonths(months);
        license.setExpirationDate(newExpiration);
        license.setStatus(LicenseStatus.ACTIVE);
        licenseRepository.save(license);

        // Create event with amount and months
        String action = months > 0 ? "renovada" : "ajustada (tiempo restado)";
        String description = "Licencia " + action + " por " + Math.abs(months) + " mes(es). Nueva fecha de vencimiento: " + newExpiration;
        if (amount != null && amount > 0) {
            description += ". Monto: $" + String.format("%.2f", amount) + " MXN";
        }
        
        createLicenseEvent(
            license.getId(),
            LicenseEvent.EventType.RENEWED,
            description,
            performedBy,
            amount,
            months
        );

        log.info("License renewed/adjusted for {} months by {}. New expiration: {}", months, performedBy, newExpiration);
    }

    /**
     * Suspend the license
     */
    @Transactional
    public void suspendLicense(String performedBy, String reason) {
        SystemLicense license = getLicense();
        if (license == null) {
            throw new RuntimeException("No license found to suspend");
        }

        license.setStatus(LicenseStatus.SUSPENDED);
        licenseRepository.save(license);

        // Create event
        createLicenseEvent(
            license.getId(),
            LicenseEvent.EventType.SUSPENDED,
            "Licencia suspendida. Razón: " + (reason != null ? reason : "No especificada"),
            performedBy
        );

        log.info("License suspended by {}. Reason: {}", performedBy, reason);
    }

    /**
     * Reactivate a suspended license
     */
    @Transactional
    public void reactivateLicense(String performedBy) {
        SystemLicense license = getLicense();
        if (license == null) {
            throw new RuntimeException("No license found to reactivate");
        }

        license.setStatus(LicenseStatus.ACTIVE);
        licenseRepository.save(license);

        // Create event
        createLicenseEvent(
            license.getId(),
            LicenseEvent.EventType.REACTIVATED,
            "Licencia reactivada",
            performedBy
        );

        log.info("License reactivated by {}", performedBy);
    }

    /**
     * Update license notes
     */
    @Transactional
    public void updateNotes(String notes, String performedBy) {
        SystemLicense license = getLicense();
        if (license == null) {
            throw new RuntimeException("No license found to update");
        }

        license.setNotes(notes);
        licenseRepository.save(license);

        log.info("License notes updated by {}", performedBy);
    }

    /**
     * Update license information (owner, restaurant, limits)
     */
    @Transactional
    public void updateLicenseInfo(String ownerName,
                                  String ownerEmail,
                                  String ownerPhone,
                                  String ownerRfc,
                                  String restaurantName,
                                  Integer maxUsers,
                                  Integer maxBranches,
                                  String performedBy) {
        SystemLicense license = getLicense();
        if (license == null) {
            throw new RuntimeException("No se encontró licencia para actualizar");
        }

        // Update owner information
        if (ownerName != null && !ownerName.trim().isEmpty()) {
            license.setOwnerName(ownerName);
        }
        if (ownerEmail != null && !ownerEmail.trim().isEmpty()) {
            license.setOwnerEmail(ownerEmail);
        }
        if (ownerPhone != null && !ownerPhone.trim().isEmpty()) {
            license.setOwnerPhone(ownerPhone);
        }
        if (ownerRfc != null) {
            license.setOwnerRfc(ownerRfc);
        }
        if (restaurantName != null && !restaurantName.trim().isEmpty()) {
            license.setRestaurantName(restaurantName);
        }

        // Update limits
        // maxUsers can be null (unlimited) or a positive number
        license.setMaxUsers(maxUsers);
        
        if (maxBranches != null && maxBranches > 0) {
            license.setMaxBranches(maxBranches);
        }

        licenseRepository.save(license);

        // Create event
        createLicenseEvent(
            license.getId(),
            LicenseEvent.EventType.UPDATED,
            "Información de licencia actualizada",
            performedBy
        );

        log.info("License information updated by {}", performedBy);
    }

    /**
     * Change license package type
     * @param newPackageType The new package type
     * @param performedBy Username who performs the change
     * @throws IllegalArgumentException if trying to change to same package
     */
    @Transactional
    public void changePackageType(SystemLicense.PackageType newPackageType, String performedBy) {
        SystemLicense license = getLicense();
        if (license == null) {
            throw new IllegalStateException("No existe una licencia en el sistema");
        }

        SystemLicense.PackageType currentPackage = license.getPackageType();
        
        // Prevent changing to same package
        if (currentPackage == newPackageType) {
            throw new IllegalArgumentException("El paquete ya es " + newPackageType.getDisplayName());
        }

        String oldPackage = currentPackage.getDisplayName();
        license.setPackageType(newPackageType);
        licenseRepository.save(license);

        // Create event
        createLicenseEvent(
            license.getId(),
            LicenseEvent.EventType.UPDATED,
            String.format("Paquete cambiado de %s a %s", oldPackage, newPackageType.getDisplayName()),
            performedBy
        );

        log.info("Package changed from {} to {} by {}", oldPackage, newPackageType, performedBy);
    }

    /**
     * Mark license as expired
     */
    @Transactional
    public void markAsExpired() {
        SystemLicense license = getLicense();
        if (license == null) {
            log.warn("No license found to mark as expired");
            return;
        }

        if (license.getStatus() != LicenseStatus.EXPIRED) {
            license.setStatus(LicenseStatus.EXPIRED);
            licenseRepository.save(license);

            // Create event
            createLicenseEvent(
                license.getId(),
                LicenseEvent.EventType.EXPIRED,
                "Licencia expirada automáticamente",
                "SYSTEM"
            );

            log.warn("License marked as expired");
        }
    }

    /**
     * Update last check date
     */
    @Transactional
    public void updateLastCheck() {
        SystemLicense license = getLicense();
        if (license != null) {
            license.setLastCheckDate(LocalDate.now());
            licenseRepository.save(license);
        }
    }

    /**
     * Update last notification sent date
     */
    @Transactional
    public void updateLastNotification() {
        SystemLicense license = getLicense();
        if (license != null) {
            license.setLastNotificationSent(LocalDate.now());
            licenseRepository.save(license);

            // Create event
            createLicenseEvent(
                license.getId(),
                LicenseEvent.EventType.NOTIFICATION_SENT,
                "Notificación de vencimiento enviada. Días restantes: " + license.daysUntilExpiration(),
                "SYSTEM"
            );
        }
    }

    /**
     * Create a license event
     */
    @Transactional
    public void createLicenseEvent(Long licenseId, LicenseEvent.EventType eventType, 
                                   String description, String performedBy) {
        createLicenseEvent(licenseId, eventType, description, performedBy, null, null);
    }

    /**
     * Create a license event with amount and months
     */
    @Transactional
    public void createLicenseEvent(Long licenseId, LicenseEvent.EventType eventType, 
                                   String description, String performedBy, Double amount, Integer months) {
        LicenseEvent event = LicenseEvent.builder()
            .licenseId(licenseId)
            .eventType(eventType)
            .eventDate(LocalDateTime.now())
            .description(description)
            .performedBy(performedBy)
            .amount(amount)
            .months(months)
            .build();

        eventRepository.save(event);
        log.debug("License event created: {} - {}", eventType, description);
    }

    /**
     * Get recent license events
     */
    public List<LicenseEvent> getRecentEvents(int limit) {
        if (limit <= 0) {
            return eventRepository.findTop10ByOrderByEventDateDesc();
        }
        
        List<LicenseEvent> events = eventRepository.findTop10ByOrderByEventDateDesc();
        return events.subList(0, Math.min(limit, events.size()));
    }

    /**
     * Get all events for the current license
     */
    public List<LicenseEvent> getLicenseEvents() {
        SystemLicense license = getLicense();
        if (license == null) {
            return List.of();
        }
        return eventRepository.findByLicenseIdOrderByEventDateDesc(license.getId());
    }

    /**
     * Get total revenue from renewals
     */
    public Double getTotalRevenue() {
        SystemLicense license = getLicense();
        if (license == null) {
            return 0.0;
        }
        
        List<LicenseEvent> renewalEvents = eventRepository.findByLicenseIdOrderByEventDateDesc(license.getId())
            .stream()
            .filter(e -> e.getEventType() == LicenseEvent.EventType.RENEWED && e.getAmount() != null)
            .toList();
        
        return renewalEvents.stream()
            .mapToDouble(LicenseEvent::getAmount)
            .sum();
    }

    /**
     * Get renewal events with amount
     */
    public List<LicenseEvent> getRenewalEventsWithAmount() {
        SystemLicense license = getLicense();
        if (license == null) {
            return List.of();
        }
        
        return eventRepository.findByLicenseIdOrderByEventDateDesc(license.getId())
            .stream()
            .filter(e -> e.getEventType() == LicenseEvent.EventType.RENEWED && e.getAmount() != null)
            .toList();
    }

    /**
     * Create a new license (initial setup)
     */
    @Transactional
    public SystemLicense createLicense(SystemLicense license, String performedBy) {
        // Check if a license already exists
        SystemLicense existing = getLicense();
        if (existing != null) {
            throw new RuntimeException("A license already exists in the system");
        }

        SystemLicense saved = licenseRepository.save(license);

        // Create creation event
        createLicenseEvent(
            saved.getId(),
            LicenseEvent.EventType.CREATED,
            "Licencia creada - Paquete: " + saved.getPackageType() + 
            ", Ciclo: " + saved.getBillingCycle(),
            performedBy
        );

        log.info("New license created: {}", saved.getLicenseKey());
        return saved;
    }

    /**
     * Generate a unique license key
     */
    public String generateLicenseKey(String restaurantName) {
        String prefix = "ELGS";
        String year = String.valueOf(LocalDate.now().getYear());
        String restaurant = restaurantName.replaceAll("[^A-Za-z]", "")
            .toUpperCase()
            .substring(0, Math.min(4, restaurantName.length()));
        String random = String.valueOf(System.currentTimeMillis()).substring(7);

        return String.format("%s-%s-%s-%s", prefix, year, restaurant, random);
    }

    /**
     * Create initial license with all parameters
     */
    @Transactional
    public SystemLicense createInitialLicense(String licenseKey,
                                             String packageType,
                                             String billingCycle,
                                             int months,
                                             String ownerName,
                                             String ownerEmail,
                                             String ownerPhone,
                                             String ownerRfc,
                                             String restaurantName,
                                             int maxUsers,
                                             int maxBranches,
                                             String performedBy) {
        // Check if a license already exists
        SystemLicense existing = getLicense();
        if (existing != null) {
            throw new RuntimeException("Ya existe una licencia en el sistema");
        }

        LocalDate purchaseDate = LocalDate.now();
        LocalDate expirationDate = purchaseDate.plusMonths(months);

        SystemLicense license = SystemLicense.builder()
            .licenseKey(licenseKey)
            .packageType(SystemLicense.PackageType.valueOf(packageType))
            .billingCycle(SystemLicense.BillingCycle.valueOf(billingCycle))
            .purchaseDate(purchaseDate)
            .expirationDate(expirationDate)
            .installationDate(LocalDate.now())
            .status(LicenseStatus.ACTIVE)
            .ownerName(ownerName)
            .ownerEmail(ownerEmail)
            .ownerPhone(ownerPhone)
            .ownerRfc(ownerRfc)
            .restaurantName(restaurantName)
            .maxUsers(maxUsers)
            .maxBranches(maxBranches)
            .version("1.0.0")
            .lastCheckDate(LocalDate.now())
            .build();

        SystemLicense saved = licenseRepository.save(license);

        // Create creation event
        createLicenseEvent(
            saved.getId(),
            LicenseEvent.EventType.CREATED,
            String.format("Licencia inicial creada - Paquete: %s, Ciclo: %s, Vigencia: %d meses", 
                saved.getPackageDisplayName(), saved.getBillingCycleDisplayName(), months),
            performedBy
        );

        log.info("Initial license created: {} for {} months by {}", 
            saved.getLicenseKey(), months, performedBy);
        return saved;
    }

    /**
     * Check if license needs notification
     */
    public boolean needsNotification() {
        SystemLicense license = getLicense();
        return license != null && license.needsNotification();
    }

    /**
     * Get days until expiration
     */
    public long getDaysUntilExpiration() {
        SystemLicense license = getLicense();
        return license != null ? license.daysUntilExpiration() : -1;
    }

    /**
     * Check if license has landing page access (WEB or ECOMMERCE)
     */
    public boolean hasLandingPageAccess() {
        SystemLicense license = getLicense();
        if (license == null) {
            return false; // Sin licencia, no tiene acceso
        }
        return license.getPackageType() == SystemLicense.PackageType.WEB ||
               license.getPackageType() == SystemLicense.PackageType.ECOMMERCE;
    }

    /**
     * Check if license has customer/client module access (ECOMMERCE only)
     */
    public boolean hasCustomerModuleAccess() {
        SystemLicense license = getLicense();
        if (license == null) {
            return false; // Sin licencia, no tiene acceso
        }
        return license.getPackageType() == SystemLicense.PackageType.ECOMMERCE;
    }

    /**
     * Check if more users can be created (based on license limit)
     * @param currentUserCount Current number of active employees
     * @return true if more users can be created, false if limit reached
     */
    public boolean canCreateMoreUsers(long currentUserCount) {
        SystemLicense license = getLicense();
        if (license == null) {
            return false; // No license, no users allowed
        }
        
        // If maxUsers is null, unlimited users allowed
        if (license.getMaxUsers() == null) {
            return true;
        }
        
        // Check if current count is below limit
        return currentUserCount < license.getMaxUsers();
    }

    /**
     * Get the maximum number of users allowed
     * @return max users or null if unlimited
     */
    public Integer getMaxUsers() {
        SystemLicense license = getLicense();
        if (license == null) {
            return 0;
        }
        return license.getMaxUsers();
    }

    /**
     * Get package type
     */
    public SystemLicense.PackageType getPackageType() {
        SystemLicense license = getLicense();
        return license != null ? license.getPackageType() : null;
    }
}
