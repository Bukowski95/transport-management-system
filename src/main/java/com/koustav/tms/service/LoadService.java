package com.koustav.tms.service;

import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koustav.tms.dto.request.LoadRequest;
import com.koustav.tms.dto.response.BidResponse;
import com.koustav.tms.dto.response.LoadDetailResponse;
import com.koustav.tms.dto.response.LoadResponse;
import com.koustav.tms.entity.Bid;
import com.koustav.tms.entity.BidStatus;
import com.koustav.tms.entity.Load;
import com.koustav.tms.entity.LoadStatus;
import com.koustav.tms.exception.InvalidStatusTransitionException;
import com.koustav.tms.exception.ResourceNotFoundException;
import com.koustav.tms.mapper.BidMapper;
import com.koustav.tms.mapper.LoadMapper;
import com.koustav.tms.repository.BidRepository;
import com.koustav.tms.repository.LoadRepository;
import com.koustav.tms.strategy.BidScoringStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class LoadService {

    @Autowired
    private LoadRepository loadRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private LoadMapper loadMapper;

    @Autowired
    private BidScoringStrategy bidScoringStrategy;

    public LoadResponse createLoad(LoadRequest request) {
        Load load = Load.builder()
            .shipperId(request.getShipperId())
            .loadingCity(request.getLoadingCity())
            .unloadingCity(request.getUnloadingCity())
            .productType(request.getProductType())
            .truckType(request.getTruckType())
            .noOfTrucks(request.getNoOfTrucks())
            .weight(request.getWeight())
            .weightUnit(request.getWeightUnit())
            .loadingDate(request.getLoadingDate())
            .datePosted(new Timestamp(System.currentTimeMillis()))
            .status(LoadStatus.POSTED)
            .build();
        
        Load saved = this.loadRepository.save(load);
        return loadMapper.toResponse(saved);

    }

    public Page<LoadResponse> listLoads(String shipperId, LoadStatus status, Pageable pageable) {
        Page<Load> loads = loadRepository.findByFilters(shipperId, status, pageable);
        return loads.map(loadMapper::toResponse);
    }

    public LoadDetailResponse getLoad(UUID loadId) {
        Load load = loadRepository.findById(loadId)
            .orElseThrow(() -> new ResourceNotFoundException("Load", "loadId", loadId));

        // Get active bids (PENDING only)
        List<Bid> activeBids = bidRepository.findByLoad_LoadIdAndStatus(loadId, BidStatus.PENDING);

        return loadMapper.toDetailResponse(load, activeBids);
    }

    public void cancelLoad(UUID loadId) {
        Load load = loadRepository.findById(loadId)
            .orElseThrow(() -> new ResourceNotFoundException("Load", "loadId", loadId));
        
        // only cancel POSTED or OPEN_FOR_BIDS loads
        if (load.getStatus() == LoadStatus.BOOKED) {
            throw new InvalidStatusTransitionException("Can't cancel a BOOKED load");
        }

        if (load.getStatus() == LoadStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Load is already cancelled");
        }

        load.setStatus(LoadStatus.CANCELLED);
        loadRepository.save(load);
    }

    public List<BidResponse> getBestBids(UUID loadId) {
        Load load = loadRepository.findById(loadId)
            .orElseThrow(() -> new ResourceNotFoundException("Load", "loadId", loadId));
        
            List<Bid> pendingBids = bidRepository.findByLoad_LoadIdAndStatus(loadId, BidStatus.PENDING);

            if (pendingBids.isEmpty()) {
                return new ArrayList<> ();
            }

            // Sort bids by score in descending order (highest score first)
            pendingBids.sort((b1, b2) -> {
                double score1 = bidScoringStrategy.calculateScore(b1);
                double score2 = bidScoringStrategy.calculateScore(b2);
                return Double.compare(score2, score1); // Descending order
            });
            
            List <BidResponse> bidResponses = new ArrayList<>();
            for(Bid x : pendingBids) {
                BidResponse bidResponse = BidMapper.toResponse(x);
                bidResponses.add(bidResponse);
            }

            return bidResponses;
    }
}
