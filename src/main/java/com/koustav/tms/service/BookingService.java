package com.koustav.tms.service;

import java.sql.Timestamp;
import java.util.UUID;

import jakarta.persistence.OptimisticLockException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koustav.tms.dto.BookingRequest;
import com.koustav.tms.dto.BookingResponse;
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
@Transactional
public class BookingService {
    
    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private LoadRepository loadRepository;

    @Autowired
    private TransporterRepository transporterRepository;

    public BookingResponse getBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", "bookingId", bookingId));
        
            return BookingMapper.toResponse(booking);
    }

    /**
     * accept the best bid
     * @param request
     * @return
     */
    public BookingResponse acceptBid(BookingRequest request) {
        try {
            Bid bid = bidRepository.findById(request.getBidId())
                .orElseThrow(() -> new ResourceNotFoundException("Bid", "bidId", request.getBidId()));
            
            //validate bid status
            if (bid.getStatus() != BidStatus.PENDING) {
                throw new InvalidStatusTransitionException("Can only accept PENDING bids. Current status: " + bid.getStatus());
            }

            Load load = bid.getLoad();
            Transporter transporter = bid.getTransporter();

            // How many more trucks needed for the load to be shipped?
            Integer allocatedSum = bookingRepository.sumAllocatedTrucksByLoadIdAndStatus(
                load.getLoadId(), BookingStatus.CONFIRMED);
            int currentlyAllocated = allocatedSum != null ? allocatedSum : 0;
            int remainingTrucks = load.getNoOfTrucks() - currentlyAllocated;

            // does the load need these many trucks offered?
            if (bid.getTrucksOffered() > remainingTrucks) {
                throw new InsufficientCapacityException(
                    String.format("Load only needs %d more trucks, but bid offers %d",
                        remainingTrucks, bid.getTrucksOffered()));
            }

            // Prevent overBooking
            if (!transporter.canAcceptBooking(load.getTruckType(), bid.getTrucksOffered())) {
                // Bid is auto rejected, actually by the transporter
                bid.setStatus(BidStatus.REJECTED);
                bidRepository.save(bid);

                throw new InsufficientCapacityException(
                    String.format("Transporter no longer has %s capacity",
                        load.getTruckType()));
            }

            // deduct booked no. of trucks(triggers optimistic lock on save)
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

            // Update BidStatus
            bid.setStatus(BidStatus.ACCEPTED);
            bidRepository.save(bid);

            // if all the remaining trucks are booked change load status to booked
            if (remainingTrucks - bid.getTrucksOffered() == 0) {
                load.setStatus(LoadStatus.BOOKED);
                loadRepository.save(load);
            }

            return BookingMapper.toResponse(saved);
        
        } catch (OptimisticLockException e) {
            throw new ConflictException(
                "Transporter",
                "Another transaction modified the transporter capacity. Please retry."
            );
        }
    }


    public void cancelBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", "bokingId", bookingId));
        
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Booking is already cancelled.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Restore transporter trucks
        Load load = booking.getLoad();
        Transporter transporter = booking.getTransporter();
        transporter.restoreTrucks(load.getTruckType(), booking.getAllocatedTrucks());
        transporterRepository.save(transporter);

        //update Load status after cancellation
        updateStatusAfterCancellation(load);
    }

    private void updateStatusAfterCancellation(Load load) {
        Integer allocatedSum = bookingRepository.sumAllocatedTrucksByLoadIdAndStatus(
            load.getLoadId(), BookingStatus.CONFIRMED);
        int currentlyAllocated = allocatedSum != null ? allocatedSum : 0;
        int remainingTrucks = load.getNoOfTrucks() - currentlyAllocated;

        if (remainingTrucks == load.getNoOfTrucks()) {
            long pendingBids = bidRepository.countByLoad_LoadIdAndStatus(
                load.getLoadId(), BidStatus.PENDING);
            if (pendingBids > 0) {
                load.setStatus(LoadStatus.OPEN_FOR_BIDS);
            } else {
                load.setStatus(LoadStatus.POSTED);
            }
        } else if (remainingTrucks > 0) {
            load.setStatus(LoadStatus.OPEN_FOR_BIDS);
        }

        loadRepository.save(load);
    }

}
