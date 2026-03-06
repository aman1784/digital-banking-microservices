# 🏦 Digital Banking Management System (Microservices Architecture)

## 📌 Project Overview

This project is a **Digital Banking Management System** built using **Spring Boot Microservices Architecture**.

It simulates a real-world banking system where:

* Users authenticate using JWT
* Accounts can be created, frozen, and unfrozen
* Transactions are processed via Kafka
* Services communicate through Eureka & API Gateway
* Centralized configuration is managed using Config Server
* Distributed tracing is enabled using Zipkin
* Role-Based Access Control (RBAC) is enforced

The system is designed to follow **enterprise-grade backend architecture patterns**.

---

# 🏗 Architecture Overview

This project consists of the following services:

| Service                  | Responsibility                                        |
| ------------------------ | ----------------------------------------------------- |
| **Auth Service**         | User authentication & JWT generation                  |
| **API Gateway**          | Central request routing & JWT validation              |
| **Eureka Server**        | Service discovery                                     |
| **Config Server**        | Centralized configuration management                  |
| **Account Service**      | Account creation, freeze/unfreeze, balance management |
| **Transaction Service**  | Deposit & withdrawal operations                       |
| **Notification Service** | Kafka consumer for transaction events                 |
| **Kafka (Docker)**       | Event-driven communication                            |
| **Zipkin**               | Distributed tracing                                   |

---

# 🔐 Security Architecture (RBAC with JWT)

## Authentication Flow

1. User logs in via **Auth Service**
2. Auth Service:

   * Validates credentials
   * Fetches roles from database
   * Generates JWT containing:

     ```json
     {
       "sub": "username",
       "roles": ["ROLE_ADMIN"]
     }
     ```
3. Client sends JWT in `Authorization: Bearer <token>`

---

## Authorization Flow

### API Gateway

* Validates JWT signature
* Rejects invalid/expired tokens (401)
* Forwards valid requests to downstream services

### Account & Transaction Services

* Parse JWT
* Extract roles
* Set Authentication in SecurityContext
* Enforce role-based access using:

```java
@PreAuthorize("hasRole('ADMIN')")
```

---

## Role-Based Access

| Role       | Permissions              |
| ---------- | ------------------------ |
| ROLE_ADMIN | Freeze/Unfreeze accounts |
| ROLE_USER  | Perform transactions     |
| Anonymous  | No access                |

---

# 🧾 Account Status Logic

Accounts have status:

```sql
ACTIVE
FROZEN
```

If account is **FROZEN**:

* Transactions are blocked
* Business exception is thrown

---

# 📡 Event-Driven Communication (Kafka)

When a transaction occurs:

1. Transaction Service publishes event to Kafka topic:

   ```
   transaction-events
   ```
2. Notification Service consumes the event
3. Logs transaction notification

This simulates real-world async banking notifications.

---

# 🗄 Database Design

### Users Table (Auth Service)

| id | username | password | enabled |
| -- | -------- | -------- | ------- |

### Roles Table

| id | name       |
| -- | ---------- |
| 1  | ROLE_USER  |
| 2  | ROLE_ADMIN |

### User_Roles Table

| user_id | role_id |

---

# ⚙️ Tech Stack

* Java 21
* Spring Boot
* Spring Security
* Spring Cloud Gateway
* Spring Cloud Config
* Eureka Discovery Server
* Kafka (Docker)
* Zipkin (Docker)
* MySQL
* Maven

---

# 🚀 How To Run The Project

## 1️⃣ Prerequisites

* Java 21
* Docker Desktop
* MySQL running locally
* Maven

---

## 2️⃣ Start Infrastructure (Kafka + Zipkin)

From project root:

```bash
docker-compose up -d
```

This starts:

* Kafka on port 9092
* Zipkin on port 9411

---

## 3️⃣ Start Services (Order Matters)

Start in this order:

1. **Config Server**
2. **Eureka Server**
3. **Auth Service**
4. **Account Service**
5. **Transaction Service**
6. **Notification Service**
7. **API Gateway**

---

## 4️⃣ Access Gateway

All APIs go through:

```
http://localhost:8080
```

---

# 🔑 Example API Flow

### 1. Login

```
POST /api/v1/auth/login
```

Response:

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

### 2. Freeze Account (Admin Only)

```
PUT /api/v1/accounts/admin/accounts/{id}/freeze
```

Header:

```
Authorization: Bearer <ADMIN_TOKEN>
```

---

### 3. Deposit Transaction

```
POST /api/v1/transactions/deposit
```

If account is frozen → Transaction blocked.

---

# 🔎 Distributed Tracing

Zipkin UI:

```
http://localhost:9411
```

Used to trace request flow across services.

---

# 🎯 Key Architectural Decisions

* JWT-based stateless authentication
* RBAC enforced at service layer
* Event-driven architecture using Kafka
* Centralized config via Config Server
* Service discovery via Eureka
* Gateway-level token validation
* Microservice separation of concerns

---

# 📚 What This Project Demonstrates

* Real-world microservices architecture
* Secure inter-service communication
* Role-based authorization
* Kafka event publishing & consumption
* Distributed tracing
* Centralized configuration management
* Dockerized infrastructure setup

---

# 🏁 Final Notes

This project is designed to mimic a production-ready backend banking system and demonstrates:

* Clean separation of concerns
* Scalable microservice architecture
* Secure authentication & authorization patterns
* Event-driven communication model
