package com.koustav.tms.dto.request;

import java.util.Map;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTrucksRequest {

    @NotNull(message = "Available trucks map is required")
    private Map<String, Integer> availableTrucks;
}
