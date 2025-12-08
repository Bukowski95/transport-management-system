package com.koustav.tms.mapper;

import com.koustav.tms.dto.TransporterResponse;
import com.koustav.tms.entity.Transporter;

public class TransporterMapper {

    private TransporterMapper() {
        // Private constructor to prevent instantiation
    }

    public static TransporterResponse toResponse(Transporter transporter) {
        return TransporterResponse.builder()
            .transporterId(transporter.getTransporterId())
            .companyName(transporter.getCompanyName())
            .rating(transporter.getRating())
            .availableTrucks(transporter.getAvailableTrucks())
            .build();
    }
}
