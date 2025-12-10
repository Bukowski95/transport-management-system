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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bid", indexes = {
    @Index(name = "idx_bid_load_id", columnList = "load_id"),
    @Index(name = "idx_bid_transporter_id", columnList = "transporter_id"),
    @Index(name = "idx_bid_status", columnList = "bid_status"),
    @Index(name = "idx_bid_date_submitted", columnList = "date_submitted DESC"),
    @Index(name = "idx_bid_composite_load_status", columnList = "load_id, bid_status"),
    @Index(name = "idx_bid_composite_transporter_status", columnList = "transporter_id, bid_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bid {

    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="bid_id", nullable=false, updatable=false)
    private UUID bidId;

    @ManyToOne
    @JoinColumn(name="load_id", nullable=false,
                foreignKey = @ForeignKey(name = "fk_bid_load"))
    private Load load;

    @ManyToOne
    @JoinColumn(name="transporter_id", nullable=false, 
                foreignKey = @ForeignKey(name = "fk_bid_transporter"))
    private Transporter transporter;

    @Column(name="proposed_rate", updatable=false, nullable=false)
    private double proposedRate;

    @Column(name="trucks_offered", updatable=false, nullable=false)
    private int trucksOffered;

    @Enumerated(EnumType.STRING)
    @Column(name="bid_status", nullable=false)
    private BidStatus status;

    @Column(name="date_submitted", nullable=false, updatable=false)
    private Timestamp dateSubmitted;

    @PrePersist  // ← JPA lifecycle callback: runs before INSERT
    protected void onCreate() {
        if (dateSubmitted == null) {
            dateSubmitted = new Timestamp(System.currentTimeMillis());
        }
        if (status == null) {
            status = BidStatus.PENDING;
        }
    }
    
}
