package com.koustav.tms.service;

import java.sql.Timestamp;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koustav.tms.dto.request.BidRequest;
import com.koustav.tms.dto.response.BidResponse;
import com.koustav.tms.entity.Bid;
import com.koustav.tms.entity.BidStatus;
import com.koustav.tms.entity.Load;
import com.koustav.tms.entity.LoadStatus;
import com.koustav.tms.entity.Transporter;
import com.koustav.tms.exception.InsufficientCapacityException;
import com.koustav.tms.exception.InvalidStatusTransitionException;
import com.koustav.tms.exception.ResourceNotFoundException;
import com.koustav.tms.mapper.BidMapper;
import com.koustav.tms.repository.BidRepository;
import com.koustav.tms.repository.LoadRepository;
import com.koustav.tms.repository.TransporterRepository;

@Service
@Transactional
public class BidService {

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private LoadRepository loadRepository;

    @Autowired
    private TransporterRepository transporterRepository;


    public BidResponse submitBid(BidRequest request) {
        Load load = loadRepository.findById(request.getLoadId())
            .orElseThrow(() -> new ResourceNotFoundException("Load", "loadId", request.getLoadId()));
        
        // validate load status
        if (load.getStatus() == LoadStatus.BOOKED || load.getStatus() == LoadStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Can't bid on a load with status " + load.getStatus());
        }

        Transporter transporter = transporterRepository.findById(request.getTransporterId())
            .orElseThrow(() -> new ResourceNotFoundException("Transporter", "transporterId", request.getTransporterId()));
        
        // does the transporter have enouught trucks?
        if (!transporter.canBid(load.getTruckType(), request.getTrucksOffered())) {
            throw new InsufficientCapacityException(
                String.format("Transporter doesn't have %d %s trucks available",
                    request.getTrucksOffered(), load.getTruckType()));   
        }

        //create a Bid
        Bid bid = Bid.builder()
            .load(load)
            .transporter(transporter)
            .proposedRate(request.getProposedRate())
            .trucksOffered(request.getTrucksOffered())
            .status(BidStatus.PENDING)
            .dateSubmitted(new Timestamp(System.currentTimeMillis()))
            .build();

        Bid saved = bidRepository.save(bid);

        // upadte load status if this was the firstbid
        if (load.getStatus() == LoadStatus.POSTED) {
            load.setStatus(LoadStatus.OPEN_FOR_BIDS);
            loadRepository.save(load);
        }

        return BidMapper.toResponse(saved);
    }

    //get bids after applying filters
    public Page<BidResponse> getBids(UUID loadId, UUID transporterId, BidStatus status, Pageable pageable) {
        Page<Bid> bids = bidRepository.findByFilters(loadId, transporterId, status, pageable);
        return bids.map(BidMapper::toResponse);
    }

    // get a single bid
    public BidResponse getBid(UUID bidId) {
        Bid bid = bidRepository.findById(bidId)
            .orElseThrow(() -> new ResourceNotFoundException("Bid", "bidId", bidId));

        return BidMapper.toResponse(bid);
    }

    public void rejectBid(UUID bidId) {
        Bid bid = bidRepository.findById(bidId)
            .orElseThrow(() -> new ResourceNotFoundException("Bid", "bidId", bidId));
        
        if (bid.getStatus() != BidStatus.PENDING) {
            throw new InvalidStatusTransitionException("can only reject PENDING bids. Current status: " + bid.getStatus());
        }
        bid.setStatus(BidStatus.REJECTED);
        bidRepository.save(bid);
    }
}
