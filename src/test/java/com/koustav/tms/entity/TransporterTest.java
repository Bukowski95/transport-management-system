package com.koustav.tms.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * test transporter domain logic for transporter
 */
class TransporterTest {

    private Transporter transporter;
    private Map<String, Integer> availableTrucks;

    @BeforeEach
    void setup() {
        // setup: Transporter with 5 TRAILER trucks and 3 CONTAINER trucks
        availableTrucks = new HashMap<>();
        availableTrucks.put("TRAILER", 5);
        availableTrucks.put("CONTAINER", 3);

        transporter = Transporter.builder()
            .companyName("Company A")
            .rating(4.5)
            .availableTrucks(availableTrucks)
            .build();
    }

    @Test
    @DisplayName("canBid: Should allow bid when sufficient trucks available")
    void testCanBid_SufficientCapacity() {
        assertTrue(transporter.canBid("TRAILER", 5));  // Exactly available
        assertTrue(transporter.canBid("TRAILER", 3));  // Less than available
        assertTrue(transporter.canBid("CONTAINER", 2));
    }

    @Test
    @DisplayName("canBid: should reject bid when insufficient trucks")
    void testCanBid_InsufficientCapacity() {
        assertFalse(transporter.canBid("TRAILER", 6));  // More than available
        assertFalse(transporter.canBid("CONTAINER", 4));
    }

    @Test
    @DisplayName("canBid: Should handle non-existent truck type")
    void testCanBid_NonExistentTruckType() {
        // EDGE CASE: Truck type not in map
        // Act & Assert
        assertFalse(transporter.canBid("FLATBED", 1));  // Type doesn't exist → 0 available
    }

    @Test
    @DisplayName("canBid: Should handle zero available trucks")
    void testCanBid_ZeroAvailable() {
        // Setup: Use all trucks
        transporter.getAvailableTrucks().put("TRAILER", 0);
        
        // Act & Assert
        assertFalse(transporter.canBid("TRAILER", 1));  // Can't bid with 0 available
        assertFalse(transporter.canBid("TRAILER", 0));   // EDGE CASE: Bid 0 trucks?
    }

    @Test
    @DisplayName("canBid: Should allow multiple bids (optimistic)")
    void testCanBid_OptimisticBidding() {
        // IMPORTANT: canBid doesn't modify state
        // Should allow multiple bids on same trucks
        
        // Act
        boolean firstBid = transporter.canBid("TRAILER", 5);
        boolean secondBid = transporter.canBid("TRAILER", 5);  // Same trucks!
        boolean thirdBid = transporter.canBid("TRAILER", 5);
        
        // Assert
        assertTrue(firstBid);
        assertTrue(secondBid);
        assertTrue(thirdBid);
        
        // Trucks should still be 5 (not deducted)
        assertEquals(5, transporter.getAvailableTrucks().get("TRAILER"));
    }

    @Test
    @DisplayName("canAcceptBooking: Should allow when capacity available")
    void testCanAcceptBooking_SufficientCapacity() {
        assertTrue(transporter.canAcceptBooking("TRAILER", 5));
        assertTrue(transporter.canAcceptBooking("TRAILER", 3));
    }
    
    @Test
    @DisplayName("canAcceptBooking: Should reject when insufficient capacity")
    void testCanAcceptBooking_InsufficientCapacity() {
        assertFalse(transporter.canAcceptBooking("TRAILER", 6));
    }

    @Test
    @DisplayName("canAcceptBooking: After deduction, should fail")
    void testCanAcceptBooking_AfterDeduction() {
        // Act: Deduct 3 trucks
        transporter.deductTrucks("TRAILER", 3);
        
        // Assert: Now only 2 available
        assertTrue(transporter.canAcceptBooking("TRAILER", 2));
        assertFalse(transporter.canAcceptBooking("TRAILER", 3));  // FAIL!
    }

    // ========================================
    // deductTrucks() Tests
    // ========================================
    
    @Test
    @DisplayName("deductTrucks: Should reduce available trucks")
    void testDeductTrucks_Success() {
        // Act
        transporter.deductTrucks("TRAILER", 3);
        
        // Assert
        assertEquals(2, transporter.getAvailableTrucks().get("TRAILER"));
    }

    @Test
    @DisplayName("deductTrucks: Should throw exception when insufficient")
    void testDeductTrucks_InsufficientCapacity() {
        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> transporter.deductTrucks("TRAILER", 6)
        );
        
        assertTrue(exception.getMessage().contains("Insufficient trucks"));
        
        // State should NOT change after exception
        assertEquals(5, transporter.getAvailableTrucks().get("TRAILER"));
    }

    @Test
    @DisplayName("deductTrucks: EDGE CASE - Deduct all trucks")
    void testDeductTrucks_DeductAll() {
        // Act
        transporter.deductTrucks("TRAILER", 5);
        
        // Assert
        assertEquals(0, transporter.getAvailableTrucks().get("TRAILER"));
    }

    @Test
    @DisplayName("deductTrucks: EDGE CASE - Non-existent truck type")
    void testDeductTrucks_NonExistentType() {
        // DISCOVERED BUG: What happens when deducting from non-existent type?
        
        // Act & Assert
        assertThrows(
            IllegalStateException.class,
            () -> transporter.deductTrucks("FLATBED", 1)
        );
    }

    @Test
    @DisplayName("deductTrucks: Should handle multiple deductions")
    void testDeductTrucks_MultipleDeductions() {
        // Act
        transporter.deductTrucks("TRAILER", 2);
        transporter.deductTrucks("TRAILER", 1);
        transporter.deductTrucks("TRAILER", 1);
        
        // Assert
        assertEquals(1, transporter.getAvailableTrucks().get("TRAILER"));
    }

    // ========================================
    // restoreTrucks() Tests
    // ========================================
    
    @Test
    @DisplayName("restoreTrucks: Should restore trucks")
    void testRestoreTrucks_Success() {
        // Setup: Deduct first
        transporter.deductTrucks("TRAILER", 3);
        assertEquals(2, transporter.getAvailableTrucks().get("TRAILER"));
        
        // Act: Restore
        transporter.restoreTrucks("TRAILER", 3);
        
        // Assert: Back to original
        assertEquals(5, transporter.getAvailableTrucks().get("TRAILER"));
    }

    @Test
    @DisplayName("restoreTrucks: EDGE CASE - Restore when count is 0")
    void testRestoreTrucks_FromZero() {
        // Setup: Use all trucks
        transporter.deductTrucks("TRAILER", 5);
        assertEquals(0, transporter.getAvailableTrucks().get("TRAILER"));
        
        // Act: Restore
        transporter.restoreTrucks("TRAILER", 3);
        
        // Assert: Should work! (From your implementation)
        assertEquals(3, transporter.getAvailableTrucks().get("TRAILER"));
    }

    @Test
    @DisplayName("restoreTrucks: Should throw exception for non-existent type")
    void testRestoreTrucks_NonExistentType() {
        // EDGE CASE: Try to restore trucks for type that never existed
        
        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> transporter.restoreTrucks("FLATBED", 5)
        );
        
        assertTrue(exception.getMessage().contains("type doesn't exist"));
    }

    @Test
    @DisplayName("restoreTrucks: BUG? Can restore more than originally had")
    void testRestoreTrucks_RestoreMoreThanOriginal() {
        // EDGE CASE: What if we restore MORE trucks than originally existed?
        // Original: 5 trucks
        
        // Act
        transporter.restoreTrucks("TRAILER", 10);  // Restore 10!
        
        // Assert: Now has 15 trucks! Is this a bug?
        assertEquals(15, transporter.getAvailableTrucks().get("TRAILER"));
        
        // QUESTION: Should we prevent this? Or is it valid (transporter bought more trucks)?
    }

    // ========================================
    // Integration Tests - Multiple Operations
    // ========================================
    
    @Test
    @DisplayName("Integration: Simulate overbidding scenario")
    void testOverbiddingScenario() {
        // Scenario: Transporter bids on 2 loads with 5 trucks available
        
        // 1. Bid on Load 1 (3 trucks) - Optimistic
        assertTrue(transporter.canBid("TRAILER", 3));
        
        // 2. Bid on Load 2 (3 trucks) - Still optimistic (same 5 trucks!)
        assertTrue(transporter.canBid("TRAILER", 3));
        
        // 3. Load 1 accepts bid → deduct
        transporter.deductTrucks("TRAILER", 3);
        assertEquals(2, transporter.getAvailableTrucks().get("TRAILER"));
        
        // 4. Load 2 tries to accept bid → should fail!
        assertFalse(transporter.canAcceptBooking("TRAILER", 3));
        
        // 5. Attempting to deduct should throw exception
        assertThrows(
            IllegalStateException.class,
            () -> transporter.deductTrucks("TRAILER", 3)
        );
    }

    @Test
    @DisplayName("Integration: Booking cancellation restores capacity")
    void testBookingCancellationRestoresCapacity() {
        // 1. Accept booking (deduct 3)
        transporter.deductTrucks("TRAILER", 3);
        assertEquals(2, transporter.getAvailableTrucks().get("TRAILER"));
        
        // 2. Cannot accept another bid for 3 trucks
        assertFalse(transporter.canAcceptBooking("TRAILER", 3));
        
        // 3. Cancel booking (restore 3)
        transporter.restoreTrucks("TRAILER", 3);
        assertEquals(5, transporter.getAvailableTrucks().get("TRAILER"));
        
        // 4. Now can accept bid for 3 trucks again!
        assertTrue(transporter.canAcceptBooking("TRAILER", 3));
    }

}
