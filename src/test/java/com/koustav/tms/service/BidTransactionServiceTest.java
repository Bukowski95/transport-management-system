package com.koustav.tms.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.koustav.tms.entity.Bid;
import com.koustav.tms.entity.BidStatus;
import com.koustav.tms.exception.ResourceNotFoundException;
import com.koustav.tms.repository.BidRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("BidTransactionService Tests")
class BidTransactionServiceTest {

    @Mock
    private BidRepository bidRepository;

    @InjectMocks
    private BidTransactionService bidTransactionService;

    private UUID bidId;
    private Bid bid;

    @BeforeEach
    void setUp() {
        bidId = UUID.randomUUID();

        bid = Bid.builder()
            .bidId(bidId)
            .proposedRate(5000.0)
            .trucksOffered(3)
            .status(BidStatus.PENDING)
            .build();
    }

    @Test
    @DisplayName("Should successfully reject bid in new transaction")
    void rejectBidInNewTransaction_Success() {
        // Arrange
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));
        when(bidRepository.save(any(Bid.class))).thenReturn(bid);

        // Act
        bidTransactionService.rejectBidInNewTransaction(bidId);

        // Assert
        assertEquals(BidStatus.REJECTED, bid.getStatus());
        verify(bidRepository).findById(bidId);
        verify(bidRepository).save(bid);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when bid does not exist")
    void rejectBidInNewTransaction_NotFound_ThrowsException() {
        // Arrange
        when(bidRepository.findById(bidId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> bidTransactionService.rejectBidInNewTransaction(bidId)
        );
        assertTrue(exception.getMessage().contains("Bid"));
    }
}
