Transport Management System (TMS) Backend

📌 Overview

This is a Spring Boot-based backend system for a Transport Management System (TMS) designed to handle load posting, bidding, booking, and transporter management with complex business rules and concurrency control.

🛠 Tech Stack
Java 17

Spring Boot 3.2+

Spring Data JPA

PostgreSQL

Maven

JUnit 5 (for unit and integration testing)

JaCoCo (for code coverage reporting)

✅ Key Features
Load creation, bidding, and booking workflows

Transporter capacity validation and truck allocation

Multi-truck load allocation support

Optimistic locking to prevent overbooking

Permission-based bidding: A transporter can bid for multiple loads, but cannot be booked if their trucks are exhausted

Example Scenario:
If T1 is booked for L1 and exhausts all trucks, then even if T1 has placed a bid for L2, the bid cannot be converted into a booking due to insufficient available trucks.

🗄 Database Schema

<img width="4063" height="7251" alt="Untitled diagram-2025-12-10-104650" src="https://github.com/user-attachments/assets/764d67ff-fe2b-405f-9822-d8d2faf4ba66" />




📡 API Documentation
A Postman collection is included in the repository. Import it into Postman to test all endpoints.





🧪 Testing
The project uses JUnit 5 for comprehensive testing:

Unit tests for service layer logic

Integration tests for repository and controller layers

Test coverage enforced via JaCoCo Maven plugin


<img width="736" height="150" alt="Screenshot 2025-12-10 at 4 18 35 PM" src="https://github.com/user-attachments/assets/6edbab1d-7157-4dc9-bcab-fa201be2062a" />

