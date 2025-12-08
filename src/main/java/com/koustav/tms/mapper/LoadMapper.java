package com.koustav.tms.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.koustav.tms.dto.BidResponse;
import com.koustav.tms.dto.LoadDetailResponse;
import com.koustav.tms.dto.LoadResponse;
import com.koustav.tms.entity.Bid;
import com.koustav.tms.entity.BookingStatus;
import com.koustav.tms.entity.Load;
import com.koustav.tms.repository.BookingRepository;

@Component
public class LoadMapper {

    @Autowired
    private BookingRepository bookingRepository;

    public LoadResponse toResponse(Load load) {
        return LoadResponse.builder()
            .loadId(load.getLoadId())
            .shipperId(load.getShipperId())
            .loadingCity(load.getLoadingCity())
            .unloadingCity(load.getUnloadingCity())
            .loadingDate(load.getLoadingDate())
            .productType(load.getProductType())
            .weight(load.getWeight())
            .weightUnit(load.getWeightUnit())
            .truckType(load.getTruckType())
            .noOfTrucks(load.getNoOfTrucks())
            .status(load.getStatus())
            .version(load.getVersion())
            .datePosted(load.getDatePosted())
            .build();
    }

    public LoadDetailResponse toDetailResponse(Load load, List<Bid> activeBids) {
        // how many more trucks does this load need?
        Integer allocatedSum = bookingRepository.sumAllocatedTrucksByLoadIdAndStatus(
            load.getLoadId(), BookingStatus.CONFIRMED);
        int remainingTrucks = load.getNoOfTrucks() - (allocatedSum != null ? allocatedSum : 0);

        List<BidResponse> bidResponses = new ArrayList<>();
        for (Bid b : activeBids) {
            bidResponses.add(BidMapper.toResponse(b));
        }

        return LoadDetailResponse.builder()
            .loadId(load.getLoadId())
            .shipperId(load.getShipperId())
            .loadingCity(load.getLoadingCity())
            .unloadingCity(load.getUnloadingCity())
            .productType(load.getProductType())
            .truckType(load.getTruckType())
            .noOfTrucks(load.getNoOfTrucks())
            .remainingTrucks(remainingTrucks)
            .weight(load.getWeight())
            .weightUnit(load.getWeightUnit())
            .loadingDate(load.getLoadingDate())
            .status(load.getStatus())
            .datePosted(load.getDatePosted())
            .activeBids(bidResponses)
            .build();
    }
}
