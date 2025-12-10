package com.koustav.tms.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koustav.tms.dto.request.BidRequest;
import com.koustav.tms.dto.response.BidResponse;
import com.koustav.tms.entity.BidStatus;
import com.koustav.tms.service.BidService;

@WebMvcTest(BidController.class)
@DisplayName("BidController Tests")
class BidControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BidService bidService;

    private UUID bidId;
    private UUID loadId;
    private UUID transporterId;
    private BidRequest bidRequest;
    private BidResponse bidResponse;

    @BeforeEach
    void setUp() {
        bidId = UUID.randomUUID();
        loadId = UUID.randomUUID();
        transporterId = UUID.randomUUID();

        bidRequest = new BidRequest();
        bidRequest.setLoadId(loadId);
        bidRequest.setTransporterId(transporterId);
        bidRequest.setProposedRate(5000.0);
        bidRequest.setTrucksOffered(3);

        bidResponse = BidResponse.builder()
            .bidId(bidId)
            .loadId(loadId)
            .transporterId(transporterId)
            .proposedRate(5000.0)
            .trucksOffered(3)
            .status(BidStatus.PENDING)
            .build();
    }

    @Test
    @DisplayName("Should submit bid and return 201 CREATED")
    void submitBid_Success() throws Exception {
        // Arrange
        when(bidService.submitBid(any(BidRequest.class))).thenReturn(bidResponse);

        // Act & Assert
        mockMvc.perform(post("/bid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bidRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.bidId").value(bidId.toString()))
            .andExpect(jsonPath("$.status").value("PENDING"));

        verify(bidService).submitBid(any(BidRequest.class));
    }

    @Test
    @DisplayName("Should get bids with filters and return 200 OK")
    void getBids_Success() throws Exception {
        // Arrange
        Page<BidResponse> bidPage = new PageImpl<>(java.util.List.of(bidResponse));
        when(bidService.getBids(eq(loadId), eq(transporterId), eq(BidStatus.PENDING), any()))
            .thenReturn(bidPage);

        // Act & Assert
        mockMvc.perform(get("/bid")
                .param("loadId", loadId.toString())
                .param("transporterId", transporterId.toString())
                .param("status", "PENDING")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].bidId").value(bidId.toString()));

        verify(bidService).getBids(eq(loadId), eq(transporterId), eq(BidStatus.PENDING), any());
    }

    @Test
    @DisplayName("Should get single bid by ID and return 200 OK")
    void getBid_Success() throws Exception {
        // Arrange
        when(bidService.getBid(bidId)).thenReturn(bidResponse);

        // Act & Assert
        mockMvc.perform(get("/bid/{bidId}", bidId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bidId").value(bidId.toString()))
            .andExpect(jsonPath("$.proposedRate").value(5000.0));

        verify(bidService).getBid(bidId);
    }

    @Test
    @DisplayName("Should reject bid and return 204 NO CONTENT")
    void rejectBid_Success() throws Exception {
        // Arrange
        doNothing().when(bidService).rejectBid(bidId);

        // Act & Assert
        mockMvc.perform(patch("/bid/{bidId}/reject", bidId))
            .andExpect(status().isNoContent());

        verify(bidService).rejectBid(bidId);
    }
}
