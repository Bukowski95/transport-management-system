package com.koustav.tms.entity;

import java.util.HashMap;
import java.util.Map;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TruckMapConverter implements AttributeConverter< Map<String, Integer>, String >{

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Integer> availableTrucks) {
        
        if (availableTrucks == null || availableTrucks.isEmpty()) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(availableTrucks);
        } catch (JsonProcessingException jpe) {
            throw new IllegalStateException("Failed to convert truck map to JSON", jpe);
        }
    }

    @Override
    public Map<String, Integer> convertToEntityAttribute(String value) {

        if (value == null || value.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            TypeReference<Map<String, Integer>> typeRef = new TypeReference<> () {};
            return objectMapper.readValue(value, typeRef);
        } catch (JsonProcessingException jpe) {
            throw new IllegalStateException("Failed to convert JSON to truck map", jpe);
        } 
    }

}
