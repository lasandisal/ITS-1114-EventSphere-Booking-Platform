# 🎟️ EventSphere – Centralized Event Discovery & Ticketing API

> **Module:** ITS 1114 – Advanced API Development  
> **Institution:** IJSE  
> **Technology Stack:** Java 21 • Spring Boot • Spring Security (JWT) • Spring Data JPA • Hibernate • MySQL • Claude API

---

# 📖 Overview

EventSphere is a RESTful backend API that centralizes event discovery and ticket booking. It provides a single platform where event organizers can publish and manage events while attendees can search, book, and manage their tickets securely.

Unlike traditional event management that relies on social media posts, messaging apps, spreadsheets, and manual ticket tracking, EventSphere offers real-time ticket inventory management, attendee tracking, and AI-assisted event discovery.

The application follows a layered architecture using Spring Boot and exposes REST APIs that can be consumed by any frontend application.

---

# ✨ Features

## Authentication & Authorization

- JWT-based stateless authentication
- Role-based access control (RBAC)
- Three user roles
  - USER (Attendee)
  - ORGANIZER
  - ADMIN
- Password encryption using BCrypt

---

## Event Management

Organizers can

- Create events
- Update event details
- Publish or unpublish events
- Cancel events
- View attendee lists
- Manage ticket inventory

---

## Ticket Management

Each event supports multiple ticket categories.

Example:

- General Admission
- VIP
- Early Bird

Each ticket type maintains its own

- Price
- Capacity
- Remaining inventory

---

## Booking System

Attendees can

- Browse published events
- Search by keyword
- Filter by category
- View ticket availability
- Purchase multiple tickets in one booking
- View booking history
- Cancel bookings

Each ticket stores

- Attendee name
- Attendee email
- Optional seat number
- Unique ticket code
- Check-in status

---

## Inventory Protection

To prevent overselling during concurrent bookings, EventSphere uses

- Pessimistic database locking
- Transaction management
- Atomic inventory updates

This guarantees ticket consistency even under high traffic.

---

## AI Assistant

The platform integrates the Claude API using tool/function calling.

The assistant can

- Search events
- Answer event-related questions
- Show user's booking history

The assistant **cannot**

- Purchase tickets
- Cancel bookings
- Modify bookings
- Perform financial transactions

All booking actions require explicit user confirmation through the application UI.

---

## Administration

Administrators can

- Manage users
- Enable/disable accounts
- Manage user roles
- Monitor platform activity

---

# 🏛️ System Architecture

```
                 Client
        (Web / Mobile / Postman)
                    │
                    │ REST API
                    ▼
+--------------------------------------+
|          Controller Layer            |
| Request Validation & Endpoints       |
+--------------------------------------+
                    │
                    ▼
+--------------------------------------+
|           Service Layer              |
| Business Logic & AI Integration      |
+--------------------------------------+
                    │
                    ▼
+--------------------------------------+
|         Repository Layer             |
|      Spring Data JPA / Hibernate     |
+--------------------------------------+
                    │
                    ▼
+--------------------------------------+
|           MySQL Database             |
+--------------------------------------+
```

---

# 📂 Project Structure

```
lk.ijse.eventsphere
│
├── constant
│
├── controller
│
├── dto
│
├── entity
│
├── enums
│
├── repository
│
├── security
│
└── service
```

---

# 🗄️ Database Entities

The system consists of five core entities.

```
User
Event
TicketType
Booking
Ticket
```

### Relationships

```
User (Organizer)
      │
      │ 1
      ▼
    Event
      │
      │ 1
      ▼
 TicketType
      │
      │ 1
      ▼
    Ticket
      ▲
      │ *
   Booking
      ▲
      │ *
     User
```

---

# 🔐 Security

- Spring Security
- JWT Authentication
- Stateless Sessions
- BCrypt Password Hashing
- Role-based Endpoint Authorization

---

# 🤖 AI Integration

Claude is integrated server-side using the Anthropic API.

Available AI tools include

- `search_events()`
- `get_event_details()`
- `get_my_bookings()`

The AI assistant only provides recommendations and information.

Booking creation, cancellation, and payment confirmation remain controlled by the application's REST endpoints.

---

# 🛠️ Technology Stack

| Category | Technology |
|----------|------------|
| Language | Java 21 |
| Framework | Spring Boot |
| Security | Spring Security |
| Authentication | JWT |
| ORM | Hibernate |
| Persistence | Spring Data JPA |
| Database | MySQL |
| Build Tool | Maven |
| AI | Claude API |
| API Format | REST / JSON |

---

# 🚀 Getting Started

## Prerequisites

- Java 21
- Maven 3.8+
- MySQL 8+ or MySQL 9.x
- Anthropic API Key

---

## Clone Repository

```bash
git clone https://github.com/YOUR_USERNAME/eventsphere.git

cd eventsphere
```

---

## Create Database

```sql
CREATE DATABASE eventsphere_db;
```

---

## Configure Application

Update `application.properties`

```properties
spring.application.name=eventsphere

spring.datasource.url=jdbc:mysql://localhost:3306/eventsphere_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

jwt.secret=YOUR_SECRET_KEY
jwt.expiration=86400000

anthropic.api.key=YOUR_CLAUDE_API_KEY
```

---

## Build

```bash
mvn clean install
```

---

## Run

```bash
mvn spring-boot:run
```

Application starts at

```
http://localhost:8080
```

---

# 📡 REST API

The backend exposes REST endpoints for

- Authentication
- User Management
- Event Management
- Ticket Types
- Bookings
- AI Assistant

All responses are returned in JSON.

---

# 🔮 Future Improvements

- Online payment gateway integration
- Email notifications
- SMS reminders
- QR code generation
- Analytics dashboard
- Organizer revenue reports
- Mobile applications
- Event recommendations using AI
- Payment hold and timeout mechanism
- Cloud deployment

---

# 👨‍💻 Developed For

**ITS 1114 – Advanced API Development**

IJSE

---

# 📄 License

This project was developed for educational purposes as part of the **ITS 1114 – Advanced API Development** module at IJSE.
