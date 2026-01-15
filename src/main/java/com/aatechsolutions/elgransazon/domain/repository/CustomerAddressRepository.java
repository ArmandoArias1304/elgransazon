package com.aatechsolutions.elgransazon.domain.repository;

import com.aatechsolutions.elgransazon.domain.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {

    /**
     * Find all active addresses for a customer
     */
    List<CustomerAddress> findByCustomerIdCustomerAndActiveTrueOrderByIsDefaultDescCreatedAtDesc(Long customerId);

    /**
     * Find default address for a customer
     */
    Optional<CustomerAddress> findByCustomerIdCustomerAndIsDefaultTrueAndActiveTrue(Long customerId);

    /**
     * Find address by id and customer id (for security)
     */
    Optional<CustomerAddress> findByIdAddressAndCustomerIdCustomerAndActiveTrue(Long addressId, Long customerId);

    /**
     * Count active addresses for a customer
     */
    long countByCustomerIdCustomerAndActiveTrue(Long customerId);

    /**
     * Reset all default flags for a customer's addresses
     */
    @Modifying
    @Query("UPDATE CustomerAddress a SET a.isDefault = false WHERE a.customer.idCustomer = :customerId")
    void resetDefaultForCustomer(@Param("customerId") Long customerId);

    /**
     * Soft delete an address
     */
    @Modifying
    @Query("UPDATE CustomerAddress a SET a.active = false WHERE a.idAddress = :addressId AND a.customer.idCustomer = :customerId")
    int softDeleteByIdAndCustomerId(@Param("addressId") Long addressId, @Param("customerId") Long customerId);
}
