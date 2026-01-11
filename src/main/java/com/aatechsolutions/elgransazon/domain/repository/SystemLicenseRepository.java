package com.aatechsolutions.elgransazon.domain.repository;

import com.aatechsolutions.elgransazon.domain.entity.SystemLicense;
import com.aatechsolutions.elgransazon.domain.entity.SystemLicense.LicenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SystemLicense entity
 */
@Repository
public interface SystemLicenseRepository extends JpaRepository<SystemLicense, Long> {

    /**
     * Find license by license key
     */
    Optional<SystemLicense> findByLicenseKey(String licenseKey);

    /**
     * Find licenses by status
     */
    List<SystemLicense> findByStatus(LicenseStatus status);

    /**
     * Find licenses expiring before a given date
     */
    @Query("SELECT sl FROM SystemLicense sl WHERE sl.expirationDate <= :date AND sl.status = 'ACTIVE'")
    List<SystemLicense> findLicensesExpiringBefore(@Param("date") LocalDate date);

    /**
     * Find licenses expiring in the next X days
     */
    @Query("SELECT sl FROM SystemLicense sl WHERE sl.expirationDate BETWEEN :today AND :futureDate AND sl.status = 'ACTIVE'")
    List<SystemLicense> findLicensesExpiringBetween(@Param("today") LocalDate today, @Param("futureDate") LocalDate futureDate);

    /**
     * Check if any active license exists
     */
    @Query("SELECT COUNT(sl) > 0 FROM SystemLicense sl WHERE sl.status = 'ACTIVE' AND sl.expirationDate > :today")
    boolean existsActiveLicense(@Param("today") LocalDate today);

    /**
     * Get the first (and should be only) license in the system
     */
    Optional<SystemLicense> findFirstByOrderByIdAsc();
}
