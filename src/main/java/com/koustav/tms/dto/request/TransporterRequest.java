package com.koustav.tms.dto.request;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransporterRequest {

    @NotBlank(message = "Company name is required")
    private String companyName;

    private double rating;

    @NotNull(message = "Available trucks map is required")
    private Map<String, Integer> availableTrucks;
}
