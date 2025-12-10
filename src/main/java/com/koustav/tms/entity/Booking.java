package com.koustav.tms.entity;

import java.sql.Timestamp;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "booking", indexes = {
    @Index(name = "idx_booking_load_id", columnList = "load_id"),
    @Index(name = "idx_booking_transporter_id", columnList = "transporter_id"),
    @Index(name = "idx_booking_bid_id", columnList = "bid_id"),
    @Index(name = "idx_booking_status", columnList = "booking_status"),
    @Index(name = "idx_booking_booked_at", columnList = "booked_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {
    
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="booking_id", nullable=false, updatable=false) 
    private UUID bookingId;

    @ManyToOne
    @JoinColumn(name="load_id", nullable=false, 
                foreignKey = @ForeignKey(name = "fk_booking_load"))
    private Load load;

    @OneToOne
    @JoinColumn(name="bid_id", nullable=false, unique=true,
                foreignKey = @ForeignKey(name = "fk_booking_bid"))
    private Bid bid;

    @ManyToOne
    @JoinColumn(name="transporter_id", nullable=false, 
                foreignKey = @ForeignKey(name = "fk_booking_transporter"))
    private Transporter transporter;

    @Column(name="allocated_trucks", updatable=false, nullable=false)
    private int allocatedTrucks;

    @Column(name="final_rate", updatable=false, nullable=false)
    private double finalRate;

    @Enumerated(EnumType.STRING)
    @Column(name="booking_status", nullable=false)
    private BookingStatus status;

    @Column(name="booked_at", updatable=false, nullable=false)
    private Timestamp bookedAt;

    @PrePersist  // ← JPA lifecycle callback: runs before INSERT
    protected void onCreate() {
        if (bookedAt == null) {
            bookedAt = new Timestamp(System.currentTimeMillis());
        }
        if (status == null) {
            status = BookingStatus.CONFIRMED;
        }
    }
    
}
