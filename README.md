# Loan Management API

A comprehensive Spring Boot-based REST API for managing bank loans with JWT authentication and role-based access control.

## Table of Contents
- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Running the Application](#running-the-application)
- [Access URLs](#access-urls)
- [API Documentation](#api-documentation)
- [Authentication](#authentication)
- [API Endpoints](#api-endpoints)
- [Business Rules](#business-rules)
- [Database Schema](#database-schema)
- [Default Users & Test Data](#default-users--test-data)
- [Testing the Application](#testing-the-application)
- [Example Workflows](#example-workflows)
- [Error Handling](#error-handling)
- [Project Structure](#project-structure)
- [Troubleshooting](#troubleshooting)

## Overview

This Loan Management System is a backend API service designed for bank employees to manage customer loans. It provides functionality for creating loans, listing loans and installments, and processing loan payments with advanced features like early payment discounts and late payment penalties.

## Features

### Core Features
- **Loan Creation**: Create loans with validation for amount, interest rate, and installment numbers
- **Credit Limit Management**: Automatic validation and tracking of customer credit limits
- **Flexible Installment Plans**: Support for 6, 9, 12, and 24-month payment plans
- **Smart Payment Processing**: Handles multiple installments with automatic calculation
- **Early Payment Discounts**: 0.1% daily discount for early payments
- **Late Payment Penalties**: 0.1% daily penalty for late payments
- **Role-Based Access Control**: ADMIN and CUSTOMER roles with different permissions

### Technical Features
- JWT-based authentication with 24-hour token expiry
- RESTful API design
- Swagger/OpenAPI documentation
- H2 in-memory database for easy development
- Comprehensive error handling
- Bean validation on all inputs
- Transaction management
- Detailed logging

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: H2 (in-memory)
- **Security**: Spring Security with JWT (jjwt 0.12.3)
- **ORM**: Spring Data JPA / Hibernate
- **API Documentation**: Swagger/OpenAPI 3.0 (springdoc 2.3.0)
- **Build Tool**: Maven 3.6+
- **Utility**: Lombok for reducing boilerplate code

## Prerequisites

Before running the application, ensure you have:
- Java 17 or higher installed
- Maven 3.6 or higher installed
- Git (optional, for cloning)

## Installation & Setup

### 1. Create Project Structure

Create the following directory structure:
```
loan-management-system/
├── src/
│   ├── main/
│   │   ├── java/com/bank/loanapi/
│   │   └── resources/
│   └── test/java/com/bank/loanapi/
├── pom.xml
└── README.md
```

### 2. Copy All Files

Copy all the provided code files to their respective locations as shown in the project structure.

### 3. Build the Project

```bash
cd lms
mvn clean install
```

## Running the Application

### Using Maven
```bash
mvn spring-boot:run
```

### Using Java JAR
```bash
java -jar target/loan-api-1.0.0.jar
```

The application will start on port 8080.

## Access URLs

Once the application is running, you can access:

| Service | URL | Description |
|---------|-----|-------------|
| **API Base URL** | http://localhost:8080/api | Base URL for all API endpoints |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | Interactive API documentation |
| **OpenAPI Spec** | http://localhost:8080/api-docs | OpenAPI specification in JSON format |
| **H2 Console** | http://localhost:8080/h2-console | Database management console |

### H2 Console Credentials
- **JDBC URL**: `jdbc:h2:mem:loandb`
- **Username**: `sa`
- **Password**: `password`

## API Documentation

### Authentication

All API endpoints (except `/api/auth/login`) require JWT authentication.

#### 1. Login to Get JWT Token
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "username": "admin",
  "roles": ["ADMIN"],
  "customerId": null
}
```

#### 2. Use Token in Requests
Include the token in the Authorization header:
```bash
curl -X GET http://localhost:8080/api/loans?customerId=1 \
  -H "Authorization: Bearer <your-jwt-token>"
```

## API Endpoints

### Authentication Endpoints

#### POST /api/auth/login
Authenticate user and receive JWT token.

**Request Body:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

### Loan Management Endpoints

#### POST /api/loans
Create a new loan for a customer.

**Headers:**
- `Authorization: Bearer <token>`
- `Content-Type: application/json`

**Request Body:**
```json
{
  "customerId": 1,
  "amount": 1000.00,
  "interestRate": 0.2,
  "numberOfInstallments": 6
}
```

**Validation Rules:**
- `numberOfInstallments`: Must be 6, 9, 12, or 24
- `interestRate`: Must be between 0.1 (10%) and 0.5 (50%)
- `amount`: Must be positive
- Customer must have sufficient available credit limit

#### GET /api/loans
List loans for a specific customer with optional filters.

**Query Parameters:**
- `customerId` (required): Customer ID
- `numberOfInstallments` (optional): Filter by installment count
- `isPaid` (optional): Filter by payment status (true/false)

**Example:**
```bash
GET /api/loans?customerId=1&isPaid=false&numberOfInstallments=12
```

#### GET /api/loans/{loanId}/installments
List all installments for a specific loan.

**Path Parameters:**
- `loanId`: The loan ID

**Example Response:**
```json
[
  {
    "id": 1,
    "loanId": 1,
    "amount": 200.00,
    "paidAmount": 0.00,
    "dueDate": "2024-02-01",
    "paymentDate": null,
    "isPaid": false,
    "installmentNumber": 1
  }
]
```

#### POST /api/loans/{loanId}/pay
Make a payment towards a loan.

**Path Parameters:**
- `loanId`: The loan ID

**Request Body:**
```json
{
  "amount": 500.00
}
```

**Response:**
```json
{
  "installmentsPaid": 2,
  "totalAmountSpent": 496.00,
  "isLoanFullyPaid": false,
  "remainingLoanAmount": 800.00,
  "paidInstallments": [
    {
      "installmentId": 1,
      "originalAmount": 200.00,
      "paidAmount": 198.00,
      "discountOrPenalty": -2.00,
      "paymentType": "EARLY"
    }
  ]
}
```

## Business Rules

### Loan Creation
1. **Installment Validation**: Only 6, 9, 12, or 24 installments allowed
2. **Interest Rate**: Between 0.1 (10%) and 0.5 (50%)
3. **Credit Check**: Total loan amount (principal + interest) must not exceed available credit
4. **Total Amount Calculation**: Total = Principal × (1 + Interest Rate)
5. **Installment Amount**: Each installment = Total ÷ Number of installments
6. **First Due Date**: 1st day of next month

### Payment Processing
1. **Whole Payments Only**: Installments must be paid in full (no partial payments)
2. **Payment Order**: Earliest unpaid installments are paid first
3. **Time Restriction**: Only installments due within 3 months can be paid
4. **Early Payment**: Discount = 0.1% × days before due date
5. **Late Payment**: Penalty = 0.1% × days after due date
6. **Loan Completion**: Credit limit is released when loan is fully paid

### Authorization
- **ADMIN Role**: Can access all customers' data
- **CUSTOMER Role**: Can only access their own loan data

## Database Schema

### Tables

#### users
- `id` (BIGINT, PK, AUTO_INCREMENT)
- `username` (VARCHAR, UNIQUE, NOT NULL)
- `password` (VARCHAR, NOT NULL)

#### user_roles
- `user_id` (BIGINT, FK to users.id)
- `role` (VARCHAR)

#### customers
- `id` (BIGINT, PK, AUTO_INCREMENT)
- `name` (VARCHAR, NOT NULL)
- `surname` (VARCHAR, NOT NULL)
- `credit_limit` (DECIMAL(15,2), NOT NULL)
- `used_credit_limit` (DECIMAL(15,2), NOT NULL)
- `user_id` (BIGINT, FK to users.id)

#### loans
- `id` (BIGINT, PK, AUTO_INCREMENT)
- `customer_id` (BIGINT, FK to customers.id)
- `loan_amount` (DECIMAL(15,2), NOT NULL)
- `number_of_installment` (INTEGER, NOT NULL)
- `interest_rate` (DECIMAL(5,2), NOT NULL)
- `create_date` (TIMESTAMP, NOT NULL)
- `is_paid` (BOOLEAN, NOT NULL)

#### loan_installments
- `id` (BIGINT, PK, AUTO_INCREMENT)
- `loan_id` (BIGINT, FK to loans.id)
- `amount` (DECIMAL(15,2), NOT NULL)
- `paid_amount` (DECIMAL(15,2), NOT NULL)
- `due_date` (DATE, NOT NULL)
- `payment_date` (DATE, NULLABLE)
- `is_paid` (BOOLEAN, NOT NULL)

## Default Users & Test Data

The application comes with pre-configured users and sample data:

### Users

| Username | Password | Role | Credit Limit | Description |
|----------|----------|------|--------------|-------------|
| admin | admin123 | ADMIN | N/A | System administrator |
| john.doe | password123 | CUSTOMER | $10,000 | Has active loan with 2 paid installments |
| jane.smith | password123 | CUSTOMER | $15,000 | Has completed loan |
| bob.wilson | password123 | CUSTOMER | $20,000 | Has new 24-month loan |
| alice.brown | password123 | CUSTOMER | $12,000 | Has loan with late payments |
| charlie.davis | password123 | CUSTOMER | $8,000 | No loans |
| david.miller | password123 | CUSTOMER | $25,000 | No loans |
| emma.garcia | password123 | CUSTOMER | $18,000 | No loans |

### Sample Loans
1. **John Doe**: 12-month loan of $5,000 at 15% interest (2 installments paid)
2. **Jane Smith**: Completed 6-month loan
3. **Bob Wilson**: New 24-month loan of $8,000 at 25% interest
4. **Alice Brown**: 6-month loan with some late payments

## Testing the Application

### Quick Test Flow

1. **Login as Admin**
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
```

2. **List All Customers' Loans**
```bash
curl -X GET "http://localhost:8080/api/loans?customerId=1" \
  -H "Authorization: Bearer $TOKEN"
```

3. **Create a New Loan**
```bash
curl -X POST http://localhost:8080/api/loans \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 5,
    "amount": 1000,
    "interestRate": 0.15,
    "numberOfInstallments": 6
  }'
```

4. **Make a Payment**
```bash
curl -X POST http://localhost:8080/api/loans/1/pay \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount": 500}'
```

### Running Unit Tests
```bash
mvn test
```

## Example Workflows

### Admin Workflow
```bash
# 1. Login
# 2. View all customers' loans
# 3. Create loans for any customer
# 4. Process payments for any loan
# 5. Monitor loan statuses
```

### Customer Workflow
```bash
# 1. Login with customer credentials
# 2. View only their own loans
# 3. View their loan installments
# 4. Make payments on their loans
# 5. Check remaining balances
```

## Error Handling

The API provides structured error responses:

### Error Response Format
```json
{
  "message": "Detailed error message",
  "error": "Error Type",
  "status": 400,
  "timestamp": "2024-01-20T10:30:00",
  "validationErrors": {
    "fieldName": "Error message for field"
  }
}
```

### Common Error Codes
- `400`: Bad Request (validation errors, insufficient credit)
- `401`: Unauthorized (invalid/missing JWT token)
- `403`: Forbidden (accessing another customer's data)
- `404`: Not Found (loan or customer not found)
- `500`: Internal Server Error

## Project Structure

```
loan-management-system/
├── src/main/java/com/bank/loanapi/
│   ├── LoanApiApplication.java          # Main application class
│   ├── config/                          # Configuration classes
│   │   ├── SecurityConfig.java          # Security configuration
│   │   ├── SwaggerConfig.java           # Swagger configuration
│   │   └── DataInitializer.java         # Initial data setup
│   ├── controller/                      # REST controllers
│   │   ├── AuthController.java          # Authentication endpoints
│   │   └── LoanController.java          # Loan management endpoints
│   ├── dto/                             # Data Transfer Objects
│   │   ├── request/                     # Request DTOs
│   │   └── response/                    # Response DTOs
│   ├── entity/                          # JPA entities
│   ├── exception/                       # Custom exceptions
│   ├── repository/                      # JPA repositories
│   ├── security/                        # Security components
│   ├── service/                         # Business logic
│   └── util/                            # Utility classes
├── src/main/resources/
│   ├── application.yml                  # Application configuration
│   └── import.sql                         # Initial data
└── pom.xml                              # Maven configuration
```

## Troubleshooting

### Common Issues

1. **Port Already in Use**
    - Change port in `application.yml`: `server.port: 8081`

2. **H2 Console Not Accessible**
    - Ensure URL matches exactly: `jdbc:h2:mem:loandb`
    - Username: `sa`, Password: `password`

3. **JWT Token Invalid**
    - Ensure "Bearer " prefix is included
    - Check token hasn't expired (24-hour validity)

4. **Cannot Create Loan**
    - Verify customer has sufficient credit limit
    - Check numberOfInstallments is 6, 9, 12, or 24
    - Ensure interest rate is between 0.1 and 0.5

5. **Access Denied**
    - CUSTOMER role users can only access their own data
    - Use ADMIN account to access all customers

