package com.koustav.tms.entity;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ConverterTest {

    public static void main(String[] args) throws Exception{
        ObjectMapper mapper = new ObjectMapper();

        //Test1: what happens with null?
        String result = mapper.writeValueAsString(null);
        System.out.println("Result of null: " + result);
        System.out.println("Is is the String 'null'?" + result.equals("null"));

        //Test2: what about an empty set?
        //Set<AvailableTrucks> emptySet = new HashSet<> ();
        //System.out.println("empty set: " + mapper.writeValueAsString(emptySet));
    }
    
}
