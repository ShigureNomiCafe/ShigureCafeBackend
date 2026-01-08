# Shigure Cafe Backend System

The backend service for ShigureCafe, a robust and secure user management system built with modern Java 25 and Spring Boot 4.

## Core Features

*   **Authentication & Security:**
    *   **Stateless JWT Authentication:** Secure, scalable token-based auth using `jjwt`.
    *   **Multi-Factor Authentication (MFA):** Supports both **Email 2FA** and **TOTP (Google Authenticator)**.
    *   **Token Blacklisting:** Instant logout capability by blacklisting active JWTs.
    *   **Secure Passwords:** Strong hashing using BCrypt.
*   **User Registration & Audit Workflow:**
    *   **Two-Stage Registration:** New users are created with a `PENDING` status.
    *   **Audit Code System:** Administrators generate unique audit codes to approve/activate users.
    *   **Email Verification:** Integration with **Microsoft Graph API** for reliable email delivery.
*   **User Management:**
    *   Role-Based Access Control (`USER`, `ADMIN`).
    *   Profile management: Nickname updates, email changes with verification.
    *   Account lifecycle management for administrators.
*   **Architecture & Reliability:**
    *   **Global Exception Handling:** Standardized error responses.
    *   **Scheduled Tasks:** Automatic cleanup of expired tokens and verification codes.
    *   **API Documentation:** Integrated **Springdoc OpenAPI (Swagger)**.

## Technical Stack

*   **Framework:** Spring Boot 4.0.1
*   **Language:** Java 25
*   **Security:** Spring Security & JJWT 0.12.6
*   **Database:** MySQL 8.x with Spring Data JPA & Hibernate
*   **Email Integration:** Microsoft Graph SDK 6.19.0 & Azure Identity
*   **MFA:** dev.samstevens.totp 1.7.1
*   **Documentation:** Springdoc OpenAPI 2.8.14

## Getting Started

### Prerequisites

*   **Java 25** or higher.
*   **Maven** (or use the provided `mvnw`).
*   **MySQL** instance.

### Configuration

Create a `.env` file in the root directory:

```env
DB_URL=jdbc:mysql://localhost:3306/shigure_cafe?serverTimezone=UTC
DB_USER=your_db_username
DB_PASSWORD=your_db_password
AZURE_CLIENT_ID=your_azure_client_id
AZURE_TENANT_ID=your_azure_tenant_id
AZURE_CLIENT_SECRET=your_azure_client_secret
```

### Running the Application

```bash
# Using Maven Wrapper
./mvnw spring-boot:run
```

The server will start on port `8080` by default. API documentation can be accessed at `/swagger-ui.html`.
