package com.koustav.tms.entity;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "load", indexes = {
    @Index(name = "idx_load_shipper_id", columnList = "shipper_id"),
    @Index(name = "idx_load_status", columnList = "status"),
    @Index(name = "idx_load_date_posted", columnList = "date_posted DESC"),
    @Index(name = "idx_load_composite_shipper_status", columnList = "shipper_id, status"),
    @Index(name = "idx_load_loading_date", columnList = "loading_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Load {

    @Id  // ← Primary key
    @GeneratedValue(strategy=GenerationType.UUID)  // ← Auto-generate UUID
    @Column(name="load_id", updatable=false, nullable=false)
    private UUID loadId;
    
    @Column(name="shipper_id", nullable=false)
    private String shipperId;
    
    @Column(name="loading_city", nullable=false, length=100)
    private String loadingCity;
    
    @Column(name="unloading_city", nullable=false, length=100)
    private String unloadingCity;
    
    @Column(name="loading_date", nullable=false)
    private Timestamp loadingDate;
    
    @Column(name="product_type", nullable=false)
    private String productType;
    
    @Column(name="weight", nullable=false)
    private double weight;
    
    @Enumerated(EnumType.STRING)  // ← Store enum as string ("KG" or "TON"), not integer
    @Column(name="weight_unit", nullable=false)
    private WeightUnit weightUnit;
    
    @Column(name="truck_type", nullable=false)
    private String truckType;  // Not enum - remember our discussion!
    
    @Column(name="no_of_trucks", nullable=false)
    private int noOfTrucks;
    
    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false)
    private LoadStatus status;
    
    @Version  // ← OPTIMISTIC LOCKING - auto-increments on each update
    @Column(name="version")
    private Long version;
    
    @Column(name="date_posted", nullable=false, updatable=false)
    private Timestamp datePosted;

    @OneToMany(mappedBy = "load", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Bid> bids = new ArrayList<>();

    @PrePersist  // ← JPA lifecycle callback: runs before INSERT
    protected void onCreate() {
        if (datePosted == null) {
            datePosted = new Timestamp(System.currentTimeMillis());
        }
        if (status == null) {
            status = LoadStatus.POSTED;
        }
    }


}
