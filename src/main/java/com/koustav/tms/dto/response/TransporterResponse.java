package com.koustav.tms.dto.response;

import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransporterResponse {

    private UUID transporterId;
    private String companyName;
    private double rating;
    private Map<String, Integer> availableTrucks;
}
