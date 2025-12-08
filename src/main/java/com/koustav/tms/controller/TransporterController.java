package com.koustav.tms.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koustav.tms.dto.TransporterRequest;
import com.koustav.tms.dto.TransporterResponse;
import com.koustav.tms.dto.UpdateTrucksRequest;
import com.koustav.tms.service.TransporterService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/transporter")
public class TransporterController {
    
    @Autowired
    private TransporterService transporterService;

    @PostMapping
    public ResponseEntity<TransporterResponse> registerTransporter(
            @Valid @RequestBody TransporterRequest request) {
        
        TransporterResponse response = transporterService.registerTransporter(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{transporterId}")
    public ResponseEntity<TransporterResponse> getTransporter(@PathVariable UUID transporterId) {
        TransporterResponse response = transporterService.getTransporter(transporterId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{transporterId}/trucks")
    public ResponseEntity<TransporterResponse> updateTrucks(
            @PathVariable UUID transporterId,
            @Valid @RequestBody UpdateTrucksRequest request) {
        
        TransporterResponse response = transporterService.updateTrucks(transporterId, request);
        return ResponseEntity.ok(response);
    }

}
