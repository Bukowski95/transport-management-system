package com.koustav.tms.mapper;

import com.koustav.tms.dto.response.BidResponse;
import com.koustav.tms.entity.Bid;

public class BidMapper {

    private BidMapper() {
        // Private constructor to prevent instantiation
    }

    public static BidResponse toResponse(Bid bid) {
        return BidResponse.builder()
            .bidId(bid.getBidId())
            .loadId(bid.getLoad().getLoadId())
            .transporterId(bid.getTransporter().getTransporterId())
            .transporterName(bid.getTransporter().getCompanyName())
            .transporterRating(bid.getTransporter().getRating())
            .proposedRate(bid.getProposedRate())
            .trucksOffered(bid.getTrucksOffered())
            .status(bid.getStatus())
            .dateSubmitted(bid.getDateSubmitted())
            .build();
    }
}
