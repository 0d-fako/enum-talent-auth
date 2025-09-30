# Enum Talent Authentication API

Backend API for Authentication and Talent Profile setup on Enum platform.

## Table of Contents
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Setup Instructions](#setup-instructions)
- [Running the Application](#running-the-application)
- [Running Tests](#running-tests)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)


## Features

### Authentication
- User signup with email verification
- Email verification via token
- Login with JWT session management
- Logout with session revocation
- Rate limiting on login attempts (5 attempts per 15 minutes)

### Talent Profile
- Create/update talent profile (transcript & statement of purpose)
- Upsert behavior (no duplicate profiles)
- Completeness calculation (0%, 50%, 100%)
- Missing fields indicator

## Tech Stack

- **Java 17**
- **Spring Boot 3.5.6**
- **Spring Security** with JWT authentication
- **Spring Data JPA** with Hibernate
- **H2 Database** (in-memory)
- **Springdoc OpenAPI** (Swagger UI)
- **Hibernate Validator**
- **Lombok**
- **JUnit 5 + Mockito** for testing
- **JaCoCo** for code coverage
- **Maven** for build management

## Prerequisites

Ensure you have the following installed:

1. **Java 17+** (JDK)
```bash
   java -version
```

Maven 3.6+

```bash   
  mvn -v
````

Git

```bash   
  git --version
```


## Setup Instructions

Clone the repository

```bash   
  git clone <repository-url> 
  cd <folder-name>
```

Build the project

```bash   
  mvn clean install
```
Run tests

bash   mvn test

Check test coverage (≥70% required)

```bash   
  mvn clean test jacoco:report
```


Open target/site/jacoco/index.html in a browser to view coverage report.

## Running the Application
Using Maven
```bash
  mvn spring-boot:run
```
Using Java
```bash
  mvn clean package
  java -jar target/enum-0.0.1-SNAPSHOT.jar
```

The application will start on http://localhost:8080

## Running Tests
Run all tests
```bash
``mvn test

```

## View coverage report

Open target/site/jacoco/index.html in your browser after running the coverage command.


## API Documentation
Swagger UI

Once the application is running, access Swagger UI at:
```
http://localhost:8080/swagger-ui/index.html
```
OpenAPI JSON
```
http://localhost:8080/v3/api-docs
```

Sample cURL Commands
1. Health Check
```bash
  curl -X GET http://localhost:8080/well-known/health

```
2. Signup
```bash
  curl -X POST http://localhost:8080/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
  ```

3. Verify Email

``` bash
    curl -X POST http://localhost:8080/v1/auth/verify-email \
  -H "Content-Type: application/json" \
  -d '{
    "token": "<verification-token-from-console>"
     }'
  ```
4. Login
```bash
  curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
  ```

5. Create/Update Profile (Authenticated)
```bash
` curl -X POST http://localhost:8080/v1/profile/talent \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt-token>" \
  -d '{
    "transcript": "My academic transcript",
    "statementOfPurpose": "My statement of purpose"
  }'
  ```

6. Get Profile (Authenticated)
```bash
  curl -X GET http://localhost:8080/v1/profile/me \
  -H "Authorization: Bearer <jwt-token>"
  ```

7. Logout (Authenticated)
```bash
``curl -X POST http://localhost:8080/v1/auth/logout \
  -H "Authorization: Bearer <jwt-token>"
  ```

Project Structure


```
├── src/
│   ├── main/
│   │   ├── java/com/enumm/
│   │   │   ├── EnumApplication.java
│   │   │   ├── config/
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── HealthController.java
│   │   │   │   └── TalentProfileController.java
│   │   │   ├── dtos/
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   ├── enums/
│   │   │   │   ├── ErrorCode.java
│   │   │   │   └── UserStatus.java
│   │   │   ├── exception/
│   │   │   │   ├── BusinessException.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── model/
│   │   │   │   ├── Session.java
│   │   │   │   ├── TalentProfile.java
│   │   │   │   ├── User.java
│   │   │   │   └── VerificationToken.java
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── JwtTokenProvider.java
│   │   │   ├── service/
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── EmailService.java
│   │   │   │   ├── RateLimitService.java
│   │   │   │   └── TalentProfileService.java
│   │   │   └── util/
│   │   │       └── TokenGenerator.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/enumm/
├── docs/
│   └── openapi.yaml
├── postman/
│   └── EnumDayTask.postman_collection.json
├── pom.xml
└── README.md
```



## Email Service:
Implemented as a mock that logs to console.

Password Requirements: Minimum 8 characters. No complexity requirements

### Token Expiry:
##### Verification tokens: 24 hours
#### JWT sessions: 7 days
#### Rate Limiting: In-memory implementation (5 attempts per 15 minutes).





