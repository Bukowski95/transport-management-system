package com.koustav.tms.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.koustav.tms.entity.Load;
import com.koustav.tms.entity.LoadStatus;

@Repository
public interface LoadRepository extends JpaRepository<Load, UUID> {

    /**
     * Admin       : view all the loads
     * Shipper     : view all loads by shipperId
     *               view all loads with shipperId ans status X
     * Transporter : view all Loads possible to bid on
     * @param shipperId
     * @param status
     * @param pageable
     * @return
     */

    @Query("SELECT l FROM Load l WHERE " +
           "(:shipperId IS NULL OR l.shipperId = :shipperId) AND " +
           "(:status IS NULL OR l.status = :status)")
    Page<Load> findByFilters(
        @Param("shipperId") String shipperId,
        @Param("status") LoadStatus status,
        Pageable pageable
    );
    
}
