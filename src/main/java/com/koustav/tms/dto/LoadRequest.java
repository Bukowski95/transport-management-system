package com.koustav.tms.dto;

import java.sql.Timestamp;

import com.koustav.tms.entity.WeightUnit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadRequest {

    @NotBlank(message = "Shipper ID is required")
    private String shipperId;

    @NotBlank(message = "Loading city is required")
    private String loadingCity;

    @NotBlank(message = "Unloading city is required")
    private String unLoadingCity;

    @NotNull(message = "Loading date is required")
    private Timestamp loadingDate;

    @NotBlank(message = "Product type is required")
    private String productType;

    @Positive(message = "Weight must be positive")
    private double weight;

    @NotNull(message = "Weight unit is required")
    private WeightUnit weightUnit;

    @NotBlank(message = "Truck type is required")
    private String truckType;

    @Positive(message = "Number of trucks must be positive")
    private int noOfTrucks;
}
