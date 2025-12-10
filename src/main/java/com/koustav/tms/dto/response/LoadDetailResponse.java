package com.koustav.tms.dto.response;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import com.koustav.tms.entity.LoadStatus;
import com.koustav.tms.entity.WeightUnit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadDetailResponse {

    private UUID loadId;
    private String shipperId;
    private String loadingCity;
    private String unloadingCity;
    private Timestamp loadingDate;
    private String productType;
    private double weight;
    private WeightUnit weightUnit;
    private String truckType;
    private int noOfTrucks;
    private LoadStatus status;
    private Long version;
    private Timestamp datePosted;

    // Additional details
    private int remainingTrucks;
    private List<BidResponse> activeBids;
}
