package com.koustav.tms.dto;

import java.sql.Timestamp;
import java.util.UUID;

import com.koustav.tms.entity.BidStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidResponse {

    private UUID bidId;
    private UUID loadId;
    private UUID transporterId;
    private String transporterName;
    private double transporterRating;
    private double proposedRate;
    private int trucksOffered;
    private BidStatus status;
    private Timestamp dateSubmitted;
    private Double score; // Optional field for ranking
}
