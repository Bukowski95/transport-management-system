package com.koustav.tms.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.koustav.tms.dto.TransporterRequest;
import com.koustav.tms.dto.TransporterResponse;
import com.koustav.tms.dto.UpdateTrucksRequest;
import com.koustav.tms.entity.Transporter;
import com.koustav.tms.exception.ResourceNotFoundException;
import com.koustav.tms.mapper.TransporterMapper;
import com.koustav.tms.repository.TransporterRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TransporterService {

    @Autowired
    private TransporterRepository transporterRepository;

    public TransporterResponse registerTransporter(TransporterRequest request) {
        Transporter transporter = Transporter.builder()
            .companyName(request.getCompanyName())
            .rating(request.getRating())
            .availableTrucks(request.getAvailableTrucks())
            .build();

        Transporter saved = transporterRepository.save(transporter);
        return TransporterMapper.toResponse(saved);
    }

    public TransporterResponse getTransporter(UUID transporterId) {
        Transporter transporter = transporterRepository.findById(transporterId)
            .orElseThrow(() -> new ResourceNotFoundException("Transporter", "transporterId", transporterId));

        return TransporterMapper.toResponse(transporter);
    }

    public TransporterResponse updateTrucks(UUID transporterId, UpdateTrucksRequest request) {
        Transporter transporter = transporterRepository.findById(transporterId)
            .orElseThrow(() -> new ResourceNotFoundException("Transporter", "transporterId", transporterId));
        
        transporter.setAvailableTrucks(request.getAvailableTrucks());

        Transporter updated = transporterRepository.save(transporter);
        return TransporterMapper.toResponse(updated);
    }
}
