package com.koustav.tms.dto.response;

import java.sql.Timestamp;
import java.util.UUID;

import com.koustav.tms.entity.BookingStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private UUID bookingId;
    private UUID bidId;
    private UUID loadId;
    private UUID transporterId;
    private String transporterName;
    private int allocatedTrucks;
    private double finalRate;
    private BookingStatus status;
    private Timestamp bookedAt;
}
