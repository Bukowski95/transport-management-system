package com.koustav.tms.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.koustav.tms.entity.Bid;
import com.koustav.tms.entity.BidStatus;
import com.koustav.tms.exception.ResourceNotFoundException;
import com.koustav.tms.repository.BidRepository;

@Service
public class BidTransactionService {
    
    @Autowired
    private BidRepository bidRepository;
    
    /**
     * Reject bid in a NEW transaction (separate from parent transaction)
     * This ensures the rejection is committed even if parent transaction rolls back
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rejectBidInNewTransaction(UUID bidId) {
        Bid bid = bidRepository.findById(bidId)
            .orElseThrow(() -> new ResourceNotFoundException("Bid", "bidId", bidId));
        
        bid.setStatus(BidStatus.REJECTED);
        bidRepository.save(bid);
        // This transaction commits immediately when method returns
    }
}