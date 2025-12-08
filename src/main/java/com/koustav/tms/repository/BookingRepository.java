package com.koustav.tms.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koustav.tms.entity.Booking;
import com.koustav.tms.entity.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, UUID>{
    
    /**
     * get all allocated trucks for CONFIRMED
     * if noOfTrucks in load - allocatedTrucks in booking = 0, Load is booked
     * @param loadId
     * @param status
     * @return
     */
    @Query("SELECT SUM(b.allocatedTrucks) FROM Booking b " +
           "WHERE b.load.loadId = :loadId AND b.status = :status")
    Integer sumAllocatedTrucksByLoadIdAndStatus(
        @Param("loadId") UUID loadId,
        @Param("status") BookingStatus status
    );
}
