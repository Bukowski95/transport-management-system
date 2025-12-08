package com.koustav.tms.controller;

import com.koustav.tms.dto.BidRequest;
import com.koustav.tms.dto.BidResponse;
import com.koustav.tms.entity.BidStatus;
import com.koustav.tms.service.BidService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/bid")
public class BidController {
    
    @Autowired
    private BidService bidService;
    
    @PostMapping
    public ResponseEntity<BidResponse> submitBid(@Valid @RequestBody BidRequest request) {
        BidResponse response = bidService.submitBid(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping
    public ResponseEntity<Page<BidResponse>> getBids(
            @RequestParam(required = false) UUID loadId,
            @RequestParam(required = false) UUID transporterId,
            @RequestParam(required = false) BidStatus status,
            Pageable pageable) {
        
        Page<BidResponse> bids = bidService.getBids(loadId, transporterId, status, pageable);
        return ResponseEntity.ok(bids);
    }
    
    @GetMapping("/{bidId}")
    public ResponseEntity<BidResponse> getBid(@PathVariable UUID bidId) {
        BidResponse response = bidService.getBid(bidId);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{bidId}/reject")
    public ResponseEntity<Void> rejectBid(@PathVariable UUID bidId) {
        bidService.rejectBid(bidId);
        return ResponseEntity.noContent().build();
    }
}