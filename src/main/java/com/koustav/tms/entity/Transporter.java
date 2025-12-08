package com.koustav.tms.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="transporter")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transporter {
    
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    @Column(name="transporter_id", updatable=false, nullable=false)
    private UUID transporterId;

    @Column(name="company_name", updatable=false, nullable=false)
    private String companyName;
    
    @Column(name="rating", nullable=false)
    private double rating;

    @Convert(converter = TruckMapConverter.class)
    @Column(name="available_trucks", nullable=false, columnDefinition="jsonb")
    private Map<String, Integer> availableTrucks;

    // Business Logics
    
    /**
     * a bid can be made if (No. of trucks offered <= total available trucks)
     * allows optimistic bidding
     */
    public boolean canBid(String truckType, int trucksOffered) {
        Integer trucksAvailable = availableTrucks.getOrDefault(truckType, 0);
        return trucksOffered > 0 && trucksOffered <= trucksAvailable;
    }

    /**
     * Deduct trucks when Booking is confired
     */
    public void deductTrucks(String truckType, int count) {
        if (!availableTrucks.containsKey(truckType)) {
            throw new IllegalStateException("Transporter don't have that truck");
        }

        Integer current = availableTrucks.get(truckType);
        
        if (current < count) {
            throw new IllegalStateException(
                String.format("Insufficient trucks. Available: %d, Required: %d", current, count)
            );
        } 
        availableTrucks.put(truckType, current - count);
    }

    /**
     * restore trucks when Booking is cancelled
     */
    public void restoreTrucks(String truckType, int count) {
        if (!availableTrucks.containsKey(truckType)) {
            throw new IllegalStateException(truckType + "type doesn't exist");
        }

        int current = availableTrucks.get(truckType);
        availableTrucks.put(truckType, current + count);
    }

    /**
     * update the no of trucks 
     */
    public void updateTruckCount(String truckType, int count) {
        availableTrucks.put(truckType, count);
    }

    /**
     * can accept booking iff there are enough trucks
     * prevents overbooking
     */
    public boolean canAcceptBooking(String truckType, int count) {
        return count <= availableTrucks.getOrDefault(truckType, 0);
    }

    @PrePersist  // ← JPA lifecycle callback: runs before INSERT
    protected void onCreate() {
        if (availableTrucks == null) {
            availableTrucks = new HashMap<> ();
        }
        rating = 0.0;
    }
}
