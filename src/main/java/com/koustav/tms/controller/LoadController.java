package com.koustav.tms.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.koustav.tms.dto.BidResponse;
import com.koustav.tms.dto.LoadDetailResponse;
import com.koustav.tms.dto.LoadRequest;
import com.koustav.tms.dto.LoadResponse;
import com.koustav.tms.entity.LoadStatus;
import com.koustav.tms.service.LoadService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/load")
public class LoadController {
    
    @Autowired 
    private LoadService loadService;
    
    //Create Load
    @PostMapping
    public ResponseEntity<LoadResponse> createLoad(@Valid @RequestBody LoadRequest request) {
        LoadResponse response = loadService.createLoad(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    //List all loads with pagiation
    @GetMapping
    public ResponseEntity<Page<LoadResponse>> listLoads(
            @RequestParam(required = false) String shipperId,
            @RequestParam(required = false) LoadStatus status,
            Pageable pageable) {
        
        Page<LoadResponse> loads = loadService.listLoads(shipperId, status, pageable);
        return ResponseEntity.ok(loads);
    }

    // Get load with active bids
    @GetMapping("/{loadId}")
    public ResponseEntity<LoadDetailResponse> getLoad(@PathVariable UUID loadId) {
        LoadDetailResponse response = loadService.getLoad(loadId);
        return ResponseEntity.ok(response);
    }

    //Cancel load
    @PatchMapping("/{loadId}/cancel")
    public ResponseEntity<Void> cancelLoad(@PathVariable UUID loadId) {
        loadService.cancelLoad(loadId);
        return ResponseEntity.noContent().build();
    }

    //Get sorted Bid Suggestions
    @GetMapping("/{loadId}/best-bids")
    public ResponseEntity<List<BidResponse>> getBestBids(@PathVariable UUID loadId) {
        List<BidResponse> bids = loadService.getBestBids(loadId);
        return ResponseEntity.ok(bids);
    }
    
}
