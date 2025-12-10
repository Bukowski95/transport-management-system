package com.koustav.tms.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TruckMapConverter Tests")
class TruckMapConverterTest {

    private TruckMapConverter converter;

    @BeforeEach
    void setUp() {
        converter = new TruckMapConverter();
    }

    @Test
    @DisplayName("Should convert truck map to JSON string")
    void convertToDatabaseColumn_ValidMap_Success() {
        // Arrange
        Map<String, Integer> truckMap = new HashMap<>();
        truckMap.put("Flatbed", 10);
        truckMap.put("Container", 5);

        // Act
        String json = converter.convertToDatabaseColumn(truckMap);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("Flatbed"));
        assertTrue(json.contains("10"));
        assertTrue(json.contains("Container"));
        assertTrue(json.contains("5"));
    }

    @Test
    @DisplayName("Should convert empty map to empty JSON object")
    void convertToDatabaseColumn_EmptyMap_ReturnsEmptyJson() {
        // Arrange
        Map<String, Integer> emptyMap = new HashMap<>();

        // Act
        String json = converter.convertToDatabaseColumn(emptyMap);

        // Assert
        assertEquals("{}", json);
    }

    @Test
    @DisplayName("Should convert null map to empty JSON object")
    void convertToDatabaseColumn_NullMap_ReturnsEmptyJson() {
        // Act
        String json = converter.convertToDatabaseColumn(null);

        // Assert
        assertEquals("{}", json);
    }

    @Test
    @DisplayName("Should convert JSON string to truck map")
    void convertToEntityAttribute_ValidJson_Success() {
        // Arrange
        String json = "{\"Flatbed\":10,\"Container\":5}";

        // Act
        Map<String, Integer> truckMap = converter.convertToEntityAttribute(json);

        // Assert
        assertNotNull(truckMap);
        assertEquals(2, truckMap.size());
        assertEquals(10, truckMap.get("Flatbed"));
        assertEquals(5, truckMap.get("Container"));
    }

    @Test
    @DisplayName("Should convert empty JSON string to empty map")
    void convertToEntityAttribute_EmptyJson_ReturnsEmptyMap() {
        // Arrange
        String json = "{}";

        // Act
        Map<String, Integer> truckMap = converter.convertToEntityAttribute(json);

        // Assert
        assertNotNull(truckMap);
        assertTrue(truckMap.isEmpty());
    }

    @Test
    @DisplayName("Should convert null JSON string to empty map")
    void convertToEntityAttribute_NullJson_ReturnsEmptyMap() {
        // Act
        Map<String, Integer> truckMap = converter.convertToEntityAttribute(null);

        // Assert
        assertNotNull(truckMap);
        assertTrue(truckMap.isEmpty());
    }

    @Test
    @DisplayName("Should convert whitespace JSON string to empty map")
    void convertToEntityAttribute_WhitespaceJson_ReturnsEmptyMap() {
        // Act
        Map<String, Integer> truckMap = converter.convertToEntityAttribute("   ");

        // Assert
        assertNotNull(truckMap);
        assertTrue(truckMap.isEmpty());
    }

    @Test
    @DisplayName("Should throw exception for invalid JSON string")
    void convertToEntityAttribute_InvalidJson_ThrowsException() {
        // Arrange
        String invalidJson = "{invalid json}";

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> converter.convertToEntityAttribute(invalidJson)
        );
        assertTrue(exception.getMessage().contains("Failed to convert JSON to truck map"));
    }

    @Test
    @DisplayName("Should handle round-trip conversion correctly")
    void roundTripConversion_Success() {
        // Arrange
        Map<String, Integer> originalMap = new HashMap<>();
        originalMap.put("Flatbed", 10);
        originalMap.put("Container", 5);
        originalMap.put("Tanker", 8);

        // Act
        String json = converter.convertToDatabaseColumn(originalMap);
        Map<String, Integer> resultMap = converter.convertToEntityAttribute(json);

        // Assert
        assertEquals(originalMap.size(), resultMap.size());
        assertEquals(originalMap.get("Flatbed"), resultMap.get("Flatbed"));
        assertEquals(originalMap.get("Container"), resultMap.get("Container"));
        assertEquals(originalMap.get("Tanker"), resultMap.get("Tanker"));
    }

    @Test
    @DisplayName("Should handle single entry map correctly")
    void convertToDatabaseColumn_SingleEntry_Success() {
        // Arrange
        Map<String, Integer> singleEntryMap = Map.of("Flatbed", 10);

        // Act
        String json = converter.convertToDatabaseColumn(singleEntryMap);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("Flatbed"));
        assertTrue(json.contains("10"));
    }

    @Test
    @DisplayName("Should handle map with zero values correctly")
    void convertToDatabaseColumn_ZeroValues_Success() {
        // Arrange
        Map<String, Integer> zeroValueMap = new HashMap<>();
        zeroValueMap.put("Flatbed", 0);

        // Act
        String json = converter.convertToDatabaseColumn(zeroValueMap);
        Map<String, Integer> resultMap = converter.convertToEntityAttribute(json);

        // Assert
        assertEquals(0, resultMap.get("Flatbed"));
    }
}
