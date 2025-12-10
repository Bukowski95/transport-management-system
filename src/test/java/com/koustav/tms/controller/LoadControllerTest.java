package com.koustav.tms.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koustav.tms.dto.request.LoadRequest;
import com.koustav.tms.dto.response.BidResponse;
import com.koustav.tms.dto.response.LoadDetailResponse;
import com.koustav.tms.dto.response.LoadResponse;
import com.koustav.tms.entity.BidStatus;
import com.koustav.tms.entity.LoadStatus;
import com.koustav.tms.entity.WeightUnit;
import com.koustav.tms.service.LoadService;

@WebMvcTest(LoadController.class)
@DisplayName("LoadController Tests")
class LoadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LoadService loadService;

    private UUID loadId;
    private LoadRequest loadRequest;
    private LoadResponse loadResponse;
    private LoadDetailResponse loadDetailResponse;

    @BeforeEach
    void setUp() {
        loadId = UUID.randomUUID();

        loadRequest = LoadRequest.builder()
            .shipperId("SHIP123")
            .loadingCity("New York")
            .unLoadingCity("Los Angeles")
            .productType("Steel")
            .truckType("Flatbed")
            .noOfTrucks(5)
            .weight(10000.0)
            .weightUnit(WeightUnit.KG)
            .loadingDate(new Timestamp(System.currentTimeMillis()))
            .build();

        loadResponse = LoadResponse.builder()
            .loadId(loadId)
            .shipperId("SHIP123")
            .loadingCity("New York")
            .unloadingCity("Los Angeles")
            .truckType("Flatbed")
            .noOfTrucks(5)
            .status(LoadStatus.POSTED)
            .build();

        loadDetailResponse = LoadDetailResponse.builder()
            .loadId(loadId)
            .shipperId("SHIP123")
            .loadingCity("New York")
            .unloadingCity("Los Angeles")
            .build();
    }

    @Test
    @DisplayName("Should create load and return 201 CREATED")
    void createLoad_Success() throws Exception {
        // Arrange
        when(loadService.createLoad(any(LoadRequest.class))).thenReturn(loadResponse);

        // Act & Assert
        mockMvc.perform(post("/load")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loadRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.loadId").value(loadId.toString()))
            .andExpect(jsonPath("$.status").value("POSTED"));

        verify(loadService).createLoad(any(LoadRequest.class));
    }

    @Test
    @DisplayName("Should list loads with filters and return 200 OK")
    void listLoads_Success() throws Exception {
        // Arrange
        Page<LoadResponse> loadPage = new PageImpl<>(List.of(loadResponse));
        when(loadService.listLoads(eq("SHIP123"), eq(LoadStatus.POSTED), any()))
            .thenReturn(loadPage);

        // Act & Assert
        mockMvc.perform(get("/load")
                .param("shipperId", "SHIP123")
                .param("status", "POSTED")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].loadId").value(loadId.toString()));

        verify(loadService).listLoads(eq("SHIP123"), eq(LoadStatus.POSTED), any());
    }

    @Test
    @DisplayName("Should get load with active bids and return 200 OK")
    void getLoad_Success() throws Exception {
        // Arrange
        when(loadService.getLoad(loadId)).thenReturn(loadDetailResponse);

        // Act & Assert
        mockMvc.perform(get("/load/{loadId}", loadId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.loadId").value(loadId.toString()));

        verify(loadService).getLoad(loadId);
    }

    @Test
    @DisplayName("Should cancel load and return 204 NO CONTENT")
    void cancelLoad_Success() throws Exception {
        // Arrange
        doNothing().when(loadService).cancelLoad(loadId);

        // Act & Assert
        mockMvc.perform(patch("/load/{loadId}/cancel", loadId))
            .andExpect(status().isNoContent());

        verify(loadService).cancelLoad(loadId);
    }

    @Test
    @DisplayName("Should get best bids sorted by score and return 200 OK")
    void getBestBids_Success() throws Exception {
        // Arrange
        BidResponse bid1 = BidResponse.builder()
            .bidId(UUID.randomUUID())
            .loadId(loadId)
            .proposedRate(5000.0)
            .trucksOffered(3)
            .status(BidStatus.PENDING)
            .build();

        BidResponse bid2 = BidResponse.builder()
            .bidId(UUID.randomUUID())
            .loadId(loadId)
            .proposedRate(4500.0)
            .trucksOffered(3)
            .status(BidStatus.PENDING)
            .build();

        when(loadService.getBestBids(loadId)).thenReturn(List.of(bid2, bid1));

        // Act & Assert
        mockMvc.perform(get("/load/{loadId}/best-bids", loadId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].proposedRate").value(4500.0))
            .andExpect(jsonPath("$[1].proposedRate").value(5000.0));

        verify(loadService).getBestBids(loadId);
    }
}
