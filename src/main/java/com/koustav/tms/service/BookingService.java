package com.koustav.tms.service;

import java.sql.Timestamp;
import java.util.UUID;

import jakarta.persistence.OptimisticLockException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koustav.tms.dto.request.BookingRequest;
import com.koustav.tms.dto.response.BookingResponse;
import com.koustav.tms.entity.Bid;
import com.koustav.tms.entity.BidStatus;
import com.koustav.tms.entity.Booking;
import com.koustav.tms.entity.BookingStatus;
import com.koustav.tms.entity.Load;
import com.koustav.tms.entity.LoadStatus;
import com.koustav.tms.entity.Transporter;
import com.koustav.tms.exception.ConflictException;
import com.koustav.tms.exception.InsufficientCapacityException;
import com.koustav.tms.exception.InvalidStatusTransitionException;
import com.koustav.tms.exception.ResourceNotFoundException;
import com.koustav.tms.mapper.BookingMapper;
import com.koustav.tms.repository.BidRepository;
import com.koustav.tms.repository.BookingRepository;
import com.koustav.tms.repository.LoadRepository;
import com.koustav.tms.repository.TransporterRepository;

@Service
public class BookingService {
    
    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private LoadRepository loadRepository;

    @Autowired
    private TransporterRepository transporterRepository;
    
    @Autowired
    private BidTransactionService bidTransactionService;  // ← NEW: Inject separate service

    @Transactional(readOnly = true)
    public BookingResponse getBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", "bookingId", bookingId));
        
        return BookingMapper.toResponse(booking);
    }

    @Transactional
    public BookingResponse acceptBid(BookingRequest request) {
        try {
            Bid bid = bidRepository.findById(request.getBidId())
                .orElseThrow(() -> new ResourceNotFoundException("Bid", "bidId", request.getBidId()));
            
            // Validate bid status
            if (bid.getStatus() != BidStatus.PENDING) {
                throw new InvalidStatusTransitionException(
                    "Can only accept PENDING bids. Current status: " + bid.getStatus());
            }

            Load load = bid.getLoad();
            Transporter transporter = bid.getTransporter();

            // Calculate remaining capacity
            Integer allocatedSum = bookingRepository.sumAllocatedTrucksByLoadIdAndStatus(
                load.getLoadId(), BookingStatus.CONFIRMED);
            int currentlyAllocated = allocatedSum != null ? allocatedSum : 0;
            int remainingTrucks = load.getNoOfTrucks() - currentlyAllocated;

            // Validate load capacity
            if (bid.getTrucksOffered() > remainingTrucks) {
                throw new InsufficientCapacityException(
                    String.format("Load only needs %d more trucks, but bid offers %d",
                        remainingTrucks, bid.getTrucksOffered()));
            }

            // Phase 2: Prevent overbooking
            if (!transporter.canAcceptBooking(load.getTruckType(), bid.getTrucksOffered())) {
                // ✅ Call separate service (goes through Spring proxy!)
                bidTransactionService.rejectBidInNewTransaction(bid.getBidId());
                
                // Now throw exception (bid rejection already committed)
                throw new InsufficientCapacityException(
                    String.format("Transporter no longer has sufficient %s capacity. Bid automatically rejected.",
                        load.getTruckType()));
            }

            // Deduct trucks (triggers optimistic lock check on save)
            transporter.deductTrucks(load.getTruckType(), bid.getTrucksOffered());
            transporterRepository.save(transporter);

            // Create booking
            Booking booking = Booking.builder()
                .bid(bid)
                .load(load)
                .transporter(transporter)
                .allocatedTrucks(bid.getTrucksOffered())
                .finalRate(bid.getProposedRate())
                .status(BookingStatus.CONFIRMED)
                .bookedAt(new Timestamp(System.currentTimeMillis()))
                .build();
            
            Booking saved = bookingRepository.save(booking);

            // Update bid status
            bid.setStatus(BidStatus.ACCEPTED);
            bidRepository.save(bid);

            // Update load status if fully booked
            int newRemaining = remainingTrucks - bid.getTrucksOffered();
            if (newRemaining == 0) {
                load.setStatus(LoadStatus.BOOKED);
            }
            // ALWAYS save load (triggers version check for concurrent requests)
            //Shipper accepts overbooking on a same load, we should throw exception
            loadRepository.save(load); 

            return BookingMapper.toResponse(saved);
        
        } catch (OptimisticLockException e) {
            throw new ConflictException(
                "Transporter",
                "Another transaction modified the transporter capacity. Please retry."
            );
        }
    }

    @Transactional
    public void cancelBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", "bookingId", bookingId));
        
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Booking is already cancelled.");
        }

        // Cancel booking
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Restore transporter trucks
        Load load = booking.getLoad();
        Transporter transporter = booking.getTransporter();
        transporter.restoreTrucks(load.getTruckType(), booking.getAllocatedTrucks());
        transporterRepository.save(transporter);

        // Update load status
        updateStatusAfterCancellation(load);
    }

    private void updateStatusAfterCancellation(Load load) {
        Integer allocatedSum = bookingRepository.sumAllocatedTrucksByLoadIdAndStatus(
            load.getLoadId(), BookingStatus.CONFIRMED);
        int currentlyAllocated = allocatedSum != null ? allocatedSum : 0;
        int remainingTrucks = load.getNoOfTrucks() - currentlyAllocated;

        if (remainingTrucks == load.getNoOfTrucks()) {
            // All bookings cancelled - check for pending bids
            long pendingBids = bidRepository.countByLoad_LoadIdAndStatus(
                load.getLoadId(), BidStatus.PENDING);
            
            if (pendingBids > 0) {
                load.setStatus(LoadStatus.OPEN_FOR_BIDS);
            } else {
                load.setStatus(LoadStatus.POSTED);
            }
        } else if (remainingTrucks > 0) {
            // Partial cancellation
            load.setStatus(LoadStatus.OPEN_FOR_BIDS);
        }
        // else: remainingTrucks == 0, stay BOOKED

        loadRepository.save(load);
    }
}
