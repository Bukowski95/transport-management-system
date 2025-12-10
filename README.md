# Transport Management System (TMS) – Backend

## Overview
This project implements a **Spring Boot–based backend** for a Transport Management System (TMS).  
The system supports load posting, bidding, booking, transporter management, capacity validation, and concurrency-safe operations.

## Tech Stack
- Java 17  
- Spring Boot 3.2+  
- Spring Data JPA  
- PostgreSQL  
- Maven  
- JUnit 5  
- JaCoCo (coverage reports)

## Key Features
- Load creation, bidding, and booking workflows  
- Transporter capacity validation and truck allocation  
- Multi-truck support for large loads  
- Optimistic locking to prevent over-booking  
- Permission-based bidding  
  - Transporters can bid on multiple loads  
  - Bookings are restricted when trucks are exhausted  

## Database Schema
![Database Schema](https://github.com/user-attachments/assets/764d67ff-fe2b-405f-9822-d8d2faf4ba66)

## API Documentation
A **Postman collection** is included in the repository.  
Import the JSON file into Postman to test all available endpoints.

## Testing
The system includes tests using **JUnit 5** and coverage reporting via **JaCoCo**.

- Unit tests for service logic  
- Integration tests for repositories and controllers  
- Coverage reports generated through Maven  

![Coverage](https://github.com/user-attachments/assets/6edbab1d-7157-4dc9-bcab-fa201be2062a)
