## Transport Management System (TMS) - Logistics Marketplace Backend

This project implements a **competitive bidding marketplace** that connects shippers with transporters, enabling efficient logistics operations through automated price discovery and optimal carrier selection.

### What It Does
The platform creates a **competitive market environment** where:
- **Shippers** post their transportation needs and receive competitive bids from multiple transporters
- **Transporters** compete by submitting bids based on their rates and service quality
- The system automatically selects the **best bidder** using a scoring algorithm (70% rate, 30% rating)
- **Over-bidding is allowed** - transporters can update their bids to stay competitive
- **Over-booking is prevented** - ensures trucks aren't double-booked through concurrency-safe operations

### Key Features
- **Automated Price Discovery**: Market-driven pricing through competitive bidding
- **Smart Carrier Selection**: Algorithm-based selection considering both cost and quality
- **Real-time Load Tracking**: Track shipment status from posting to delivery
- **Capacity Validation**: Prevents overbooking and ensures reliable capacity management
- **Concurrent Transaction Safety**: Handles multiple simultaneous bookings without conflicts

### Technical Implementation
Built with Spring Boot backend featuring RESTful APIs, PostgreSQL database with optimistic locking for concurrency control, comprehensive test coverage (85%), and proper error handling with input validation.

## Tech Stack
- Java 17  
- Spring Boot 3.2+  
- Spring Data JPA  
- PostgreSQL  
- Maven  
- JUnit 5  
- JaCoCo (coverage reports)
  

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

## Future Addition(s):
- Queue Based Notification services to shippers and Transporters.
