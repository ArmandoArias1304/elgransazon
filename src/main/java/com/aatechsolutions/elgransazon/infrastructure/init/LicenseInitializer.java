package com.aatechsolutions.elgransazon.infrastructure.init;

import com.aatechsolutions.elgransazon.domain.entity.LicenseEvent;
import com.aatechsolutions.elgransazon.domain.entity.SystemLicense;
import com.aatechsolutions.elgransazon.domain.entity.SystemLicense.BillingCycle;
import com.aatechsolutions.elgransazon.domain.entity.SystemLicense.LicenseStatus;
import com.aatechsolutions.elgransazon.domain.entity.SystemLicense.PackageType;
import com.aatechsolutions.elgransazon.domain.repository.LicenseEventRepository;
import com.aatechsolutions.elgransazon.domain.repository.SystemLicenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * License Initializer
 * Creates default license if none exists (Singleton pattern)
 * Similar to SystemConfigurationInitializer
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class LicenseInitializer implements CommandLineRunner {

    private final SystemLicenseRepository licenseRepository;
    private final LicenseEventRepository eventRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking if default license needs to be created...");

        // Check if license already exists (Singleton pattern)
        if (licenseRepository.count() > 0) {
            log.info("License already exists in the system. Skipping initialization.");
            return;
        }

        log.info("No license found. Creating default license...");
        createDefaultLicense();
    }

    /**
     * Create default license with real data
     */
    private void createDefaultLicense() {
        try {
            // Generate unique license key
            String licenseKey = generateLicenseKey();
            
            // Default dates: 1 year validity
            LocalDate purchaseDate = LocalDate.now();
            LocalDate expirationDate = purchaseDate.plusYears(1);
            LocalDate installationDate = purchaseDate;

            // Build default license
            SystemLicense defaultLicense = SystemLicense.builder()
                    .licenseKey(licenseKey)
                    .packageType(PackageType.ECOMMERCE) // Full package
                    .billingCycle(BillingCycle.ANNUAL)
                    .purchaseDate(purchaseDate)
                    .expirationDate(expirationDate)
                    .installationDate(installationDate)
                    .status(LicenseStatus.ACTIVE)
                    // Client information
                    .ownerName("El Gran Sazón - Propietario Demo")
                    .ownerEmail("admin@elgransazon.com")
                    .ownerPhone("+52 33 1234 5678")
                    .ownerRfc("EGS260110ABC")
                    .restaurantName("El Gran Sazón - Restaurante Demo")
                    // System limits - Unlimited
                    .maxUsers(null) // null = unlimited
                    .maxBranches(5)
                    // Technical info
                    .version("1.0.0")
                    .notes("Licencia predeterminada creada automáticamente al inicializar el sistema. " +
                           "Esta licencia utiliza el patrón Singleton para asegurar que solo exista " +
                           "una licencia en todo el sistema. Válida por 1 año desde la instalación.")
                    .lastCheckDate(LocalDate.now())
                    .lastNotificationSent(null)
                    .build();

            // Save license
            SystemLicense saved = licenseRepository.save(defaultLicense);
            log.info("✅ Default license created successfully: {}", saved.getLicenseKey());
            log.info("   - Package: {} ({})", saved.getPackageType(), saved.getPackageDisplayName());
            log.info("   - Valid until: {}", saved.getExpirationDate());
            log.info("   - Users: {}", saved.getMaxUsersDisplay());
            log.info("   - Branches: {}", saved.getMaxBranches());

            // Create initial event
            createInitialEvent(saved);

        } catch (Exception e) {
            log.error("❌ Error creating default license: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create default license", e);
        }
    }

    /**
     * Generate unique license key
     * Format: ELGS-YYYY-XXXX-RANDOM
     */
    private String generateLicenseKey() {
        int year = LocalDate.now().getYear();
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return String.format("ELGS-%d-DEMO-%s", year, random);
    }

    /**
     * Create initial license event
     */
    private void createInitialEvent(SystemLicense license) {
        try {
            LicenseEvent event = LicenseEvent.builder()
                    .licenseId(license.getId())
                    .eventType(LicenseEvent.EventType.CREATED)
                    .eventDate(LocalDateTime.now())
                    .description("Licencia predeterminada creada automáticamente por el sistema. " +
                                "Paquete: " + license.getPackageDisplayName() + ", " +
                                "Ciclo: " + license.getBillingCycleDisplayName() + ", " +
                                "Válida hasta: " + license.getExpirationDate())
                    .performedBy("SYSTEM")
                    .amount(null)
                    .months(12)
                    .build();

            eventRepository.save(event);
            log.info("   - Initial event registered for license");
        } catch (Exception e) {
            log.warn("Warning: Could not create initial event: {}", e.getMessage());
        }
    }
}
