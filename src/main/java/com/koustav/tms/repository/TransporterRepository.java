package com.koustav.tms.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.koustav.tms.entity.Transporter;

@Repository
public interface TransporterRepository extends JpaRepository<Transporter, UUID>{
    //find and save will do
}
