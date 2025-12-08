package com.koustav.tms.mapper;

import com.koustav.tms.dto.BookingResponse;
import com.koustav.tms.entity.Booking;

public class BookingMapper {

    private BookingMapper() {
        // Private constructor to prevent instantiation
    }

    public static BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
            .bookingId(booking.getBookingId())
            .bidId(booking.getBid().getBidId())
            .loadId(booking.getLoad().getLoadId())
            .transporterId(booking.getTransporter().getTransporterId())
            .transporterName(booking.getTransporter().getCompanyName())
            .allocatedTrucks(booking.getAllocatedTrucks())
            .finalRate(booking.getFinalRate())
            .status(booking.getStatus())
            .bookedAt(booking.getBookedAt())
            .build();
    }
}
