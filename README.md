# 🎟️ EventSphere – Centralized Event Discovery & Ticketing Platform

> **Coursework Module:** ITS 1114 – Advanced API Development  
> **Institution:** Institute of Java and Software Engineering (IJSE)  
> **Technology Stack:** Java 21 (LTS) • Spring Boot 3.2.4 • Spring Security 6 (JWT) • Spring Data JPA • Hibernate • MySQL (Local & Aiven Cloud) • PayHere IPG • Google Gemini API • Google ZXing • Brevo SMTP

---

## 📖 System Overview

**EventSphere** is an enterprise-grade RESTful ticketing and event discovery platform designed to replace fragmented, manual event coordination (spreadsheets, messaging apps, and social media posts) with a robust, scalable backend engine.

The platform provides a centralized, secure ecosystem where:
- **Attendees** can discover upcoming events, reserve multi-tier tickets with real-time cart holds, complete secure online payments, receive cryptographically signed QR tickets via email, and interact with an AI event concierge.
- **Organizers** can apply for verified status, publish events, configure ticket tiers, monitor real-time attendee lists, and scan QR tickets at venue entry gates with fraud protection.
- **Administrators** govern organizer verifications, oversee platform users and roles, and monitor gross revenue, ticket sales volume, and top-performing events.

---

## 🛠️ Technology Stack

| Layer / Subsystem | Technology | Details & Architecture Role |
| :--- | :--- | :--- |
| **Language & Runtime** | **Java 21 (LTS)** | Records, modern switch expressions, pattern matching, strong memory model. |
| **Framework** | **Spring Boot 3.2.4** | REST controllers, dependency injection, transaction management, asynchronous scheduling. |
| **Security & Auth** | **Spring Security 6 + JJWT 0.11.5** | Stateless authentication, RBAC, BCrypt password hashing (cost factor 12), and JWT filters. |
| **Persistence & ORM** | **Spring Data JPA & Hibernate** | ORM mapping, pessimistic database locks, custom JPQL queries, connection pooling via **HikariCP**. |
| **Database** | **MySQL 8+ / 9.x** | Relational data persistence with support for local instances and **Aiven Cloud Managed MySQL**. |
| **Payment Gateway** | **PayHere IPG** | Online checkout processing with SHA-256 checkout hashes and MD5 webhook signature verification. |
| **Artificial Intelligence** | **Google Gemini API** (`gemini-3.5-flash-lite`) | Natural language event assistant using server-side tool/function calling with strict safety guardrails. |
| **QR Code Engine** | **Google ZXing 3.5.3** | High-resolution PNG QR generation with **HMAC-SHA256 cryptographic signatures**. |
| **Email Service** | **JavaMailSender + Brevo SMTP** | Asynchronous (`@Async`) HTML emails: registration OTP, password reset, receipts, and attendee passes. |
| **Build & Tooling** | **Maven 3.8+**, **Lombok**, **spring-dotenv** | Automated dependency management, boilerplate reduction, and `.env` configuration loading. |

---

## 🏛️ System Architecture

EventSphere implements a strict **three-tier layered architecture**:

```
                             Client Applications
                (Web Frontend / Mobile / Gate QR Scanner / Postman)
                                     │
                                     │ HTTPS / JSON + Bearer JWT
                                     ▼
      ┌─────────────────────────────────────────────────────────────┐
      │                   Spring Security Filter                    │
      │         CORS Configuration • JwtAuthenticationFilter        │
      └──────────────────────────────┬──────────────────────────────┘
                                     │
                                     ▼
      ┌─────────────────────────────────────────────────────────────┐
      │                      Controller Layer                       │
      │    DTO Request Validation (@Valid) • Response Serialization │
      └──────────────────────────────┬──────────────────────────────┘
                                     │
                                     ▼
      ┌─────────────────────────────────────────────────────────────┐
      │                        Service Layer                        │
      │   Business Workflows • Pessimistic Concurrency Controls     │
      │   Schedulers (@Scheduled) • Async Email Dispatch (@Async)   │
      └──┬──────────────────┬─────────────────┬──────────────────┬──┘
         │                  │                 │                  │
         ▼                  ▼                 ▼                  ▼
  ┌──────────────┐   ┌──────────────┐  ┌──────────────┐   ┌──────────────┐
  │ Google Gemini│   │ PayHere IPG  │  │  Brevo SMTP  │   │ Google ZXing │
  │ AI Assistant │   │ Webhook IPG  │  │ Transactional│   │ QR Generator │
  └──────────────┘   └──────────────┘  └──────────────┘   └──────────────┘
         │
         ▼
      ┌─────────────────────────────────────────────────────────────┐
      │                      Repository Layer                       │
      │       Spring Data JPA • Hibernate ORM • HikariCP Pool       │
      └──────────────────────────────┬──────────────────────────────┘
                                     │
                                     ▼
      ┌─────────────────────────────────────────────────────────────┐
      │                  MySQL Relational Database                  │
      │              (Local MySQL / Aiven Cloud MySQL)              │
      └─────────────────────────────────────────────────────────────┘
```

---

## 🗄️ Database Entities & Relational Schema

The domain model consists of **15 core entities**:

```
User (1) ───────────< UserRoles >─────────── (N) Role
User (1) ─────────── (1) Organizer
Organizer (1) ────── (N) Event
Category (1) ─────── (N) Event
Venue (1) ────────── (N) Event
Event (1) ────────── (N) TicketType
Event (1) ────────── (N) EventSchedule
User (1) ─────────── (N) Booking
Event (1) ────────── (N) Booking
Booking (1) ──────── (N) BookingItem
TicketType (1) ───── (N) BookingItem
BookingItem (1) ──── (N) Ticket
Ticket (1) ───────── (N) CheckIn
Booking (1) ──────── (1) Payment
Payment (1) ──────── (N) PaymentLog
User (1) ─────────── (N) PasswordResetToken
```

### Entity Descriptions:
1. **`User`**: Core identity entity storing credentials (BCrypt hash), contact details, status (`ACTIVE`, `INACTIVE`), email verification status, and OTP verification codes.
2. **`Role`**: Role-based access control table (`ROLE_USER`, `ROLE_ORGANIZER`, `ROLE_ADMIN`).
3. **`Organizer`**: Business identity linked 1-to-1 with `User`, containing business registration number (BRN), NIC/Passport, verification status (`PENDING`, `APPROVED`, `REJECTED`), and bio.
4. **`Category`**: Event categories (e.g., *Technology*, *Music*, *Business*, *Workshops*).
5. **`Venue`**: Event physical locations or virtual venues (name, address, city, max capacity).
6. **`Event`**: Core event record storing title, description, banner URL, schedule dates, organizer reference, venue reference, and lifecycle status (`DRAFT`, `PUBLISHED`, `CANCELLED`).
7. **`EventSchedule`**: Extended multi-session or recurring date schedules for complex events.
8. **`TicketType`**: Tiers per event (e.g., *VIP*, *General Admission*, *Early Bird*) with unit price, total capacity, and available inventory.
9. **`Booking`**: Order entity capturing status (`PENDING`, `CONFIRMED`, `CANCELLED`, `EXPIRED`), total amount, unique UUID booking reference, and cart hold expiry timestamp (`expiresAt`).
10. **`BookingItem`**: Line item linking a booking to a ticket type, recording quantity, fixed unit price snapshot, and subtotal.
11. **`Ticket`**: Individual issued ticket instance with UUID ticket code, seat/zone number, attendee name, attendee email, issued timestamp, and entry status (`VALID`, `USED`, `CANCELLED`).
12. **`Payment`**: Payment record capturing transaction status (`PENDING`, `SUCCESS`, `FAILED`), merchant order ID, currency, amount, and payment provider (`PAYHERE`).
13. **`PaymentLog`**: Immutable audit log of every incoming webhook payload received from the payment gateway.
14. **`CheckIn`**: Append-only gate check-in log capturing every scan attempt, timestamp, gate staff user reference, and location notes.
15. **`PasswordResetToken`**: Time-limited 6-digit OTP tokens for secure password recovery.

---

## 💡 Key Engineering Highlights

### 1. Anti-Overselling & Pessimistic Concurrency Locking
Under heavy concurrent traffic, standard database reads allow two users to simultaneously purchase the final remaining ticket. EventSphere prevents race conditions using **Pessimistic Write Locking** (`LockModeType.PESSIMISTIC_WRITE`) at the database row level:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select t from TicketType t where t.id = :id")
Optional<TicketType> lockById(@Param("id") Long id);
```
During checkout, the database row is locked for the duration of the transaction (`SELECT ... FOR UPDATE`), validated, decremented, and safely committed.

### 2. 10-Minute Cart Hold & Automated Expiry Scheduler
When an attendee initiates checkout:
- Tickets are held provisionally with a status of `PENDING` and a 10-minute time-to-live (`expiresAt = now + 10 mins`).
- A background worker (`BookingExpiryScheduler`) fires every 60 seconds (`@Scheduled(fixedRate = 60000)`).
- Stale pending bookings past their TTL are automatically set to `EXPIRED`, and their held quantities are returned to the ticket pool.

### 3. Anti-Counterfeit HMAC-SHA256 QR Passes
Tickets are protected against gate forgery attacks:
- The QR payload contains a Base64 URL-safe token encoding the ticket code and an **HMAC-SHA256 cryptographic signature** computed using a server secret:
  $$\text{Payload} = \text{Base64Url}(\text{ticketCode} + ":" + \text{HMAC-SHA256}(\text{ticketCode}, \text{secret}))$$
- Gate staff scan tickets at `/api/v1/organizer/check-in/scan`. The signature is verified in memory in $O(1)$ time. Forged tokens are rejected before hitting the database.
- A ticket can only be used once; duplicate scan attempts are rejected with a `409 Conflict` (`TicketAlreadyUsedException`) while recording an audit entry.

### 4. Automated PayHere IPG & Idempotent Webhooks
- **Initiation**: Generates PayHere parameters and an MD5/SHA-256 checkout signature.
- **Asynchronous Webhook (`/api/v1/payments/notify`)**: PayHere sends server-to-server payment notifications. EventSphere validates the callback signature (`md5sig`), transitions payment to `SUCCESS`, booking to `CONFIRMED`, marks tickets as issued, and triggers email delivery.
- **Idempotency**: Late or duplicate webhook deliveries are safely acknowledged without double-crediting orders or re-issuing tickets.

### 5. Multi-Recipient Transactional Emails
Using Spring's `@Async` JavaMailSender with Brevo SMTP:
- **Master Order Receipt**: Dispatched to the primary purchaser with an itemized payment summary and all ticket QR passes inline.
- **Individual Guest Passes**: Sent directly to distinct guest attendees with their personal QR code pass.
- **OTP Verification**: Dispatches 6-digit registration and password reset codes.

### 6. Google Gemini AI Assistant with Function Calling
An interactive AI assistant (`gemini-3.5-flash-lite`) integrated server-side:
- **Tools**:
  - `search_events(keyword, category)`: Discovers published events matching criteria.
  - `get_event_details(eventId)`: Retrieves real-time venue, schedule, and ticket tier availability.
  - `get_my_bookings()`: Fetches the authenticated user's active booking history.
- **Safety Guardrail**: The assistant is strictly read-only. It has no tools to modify bookings or perform payments, preventing unauthorized model-driven financial mutations.

---

## 📡 Complete REST API Catalog

All endpoints return unified JSON responses wrapped in `CommonResponse<T>` (`code`, `message`, `data`).

### 1. Authentication & Account (`/api/v1/auth`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Public | Register new user; dispatches 6-digit verification OTP |
| `POST` | `/api/v1/auth/login` | Public | Authenticate user; returns JWT token and role profile |
| `POST` | `/api/v1/auth/verify-otp` | Public | Verify email OTP and activate account |
| `POST` | `/api/v1/auth/resend-otp` | Public | Resend account verification OTP email |
| `POST` | `/api/v1/auth/forgot-password` | Public | Request 6-digit password reset OTP email |
| `POST` | `/api/v1/auth/reset-password` | Public | Reset password using valid OTP |

### 2. User Profile (`/api/v1/users`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/users/me` | Authenticated | Retrieve current user's profile |
| `PUT` | `/api/v1/users/profile` | Authenticated | Update user's name and phone number |
| `PUT` | `/api/v1/users/password` | Authenticated | Change current password |

### 3. Public Discovery (`/api/v1/events`, `/api/v1/categories`, `/api/v1/venues`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/events` | Public | Paginated list of published events with search and filters |
| `GET` | `/api/v1/events/{id}` | Public | Event details with live ticket tiers and remaining stock |
| `GET` | `/api/v1/categories` | Public | List all event categories |
| `GET` | `/api/v1/venues` | Public | List all venues |

### 4. Booking System (`/api/v1/bookings`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/bookings` | Authenticated | Reserve tickets with 10-min hold (pessimistic lock) |
| `GET` | `/api/v1/bookings/{id}` | Authenticated | Retrieve booking by ID (ownership verified) |
| `GET` | `/api/v1/bookings` | Authenticated | Paginated list of user's bookings (`upcoming`, `past`, `cancelled`) |
| `PATCH`| `/api/v1/bookings/{id}/cancel` | Authenticated | Cancel pending booking and release held inventory |

### 5. Payments & Webhooks (`/api/v1/payments`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/payments/test-checkout` | Public | Check PayHere parameters in sandbox environment |
| `POST` | `/api/v1/payments/initiate/{bookingId}` | Authenticated | Generate checkout hash and payment parameters |
| `POST` | `/api/v1/payments/notify` | Public (IPG) | PayHere webhook callback; confirms payment and issues tickets |

### 6. Organizer Operations (`/api/v1/organizer`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/organizer/apply` | Authenticated | Submit application for organizer verification |
| `GET` | `/api/v1/organizer/profile` | Authenticated | View organizer profile and approval status |
| `PUT` | `/api/v1/organizer/profile` | `ORGANIZER` | Update organizer business details |
| `POST` | `/api/v1/organizer/events` | `ORGANIZER`, `ADMIN` | Create new event draft |
| `POST` | `/api/v1/organizer/events/{id}/ticket-types` | `ORGANIZER`, `ADMIN` | Add ticket tiers to an event |
| `PUT` | `/api/v1/organizer/events/{id}` | `ORGANIZER`, `ADMIN` | Update event information |
| `PATCH`| `/api/v1/organizer/events/{id}/publish` | `ORGANIZER`, `ADMIN` | Publish event for public discovery |
| `PATCH`| `/api/v1/organizer/events/{id}/cancel` | `ORGANIZER`, `ADMIN` | Cancel event and halt sales |
| `GET` | `/api/v1/organizer/events/my-events` | `ORGANIZER`, `ADMIN` | Paginated list of organizer's events |
| `GET` | `/api/v1/organizer/events/{id}/bookings` | `ORGANIZER`, `ADMIN` | View attendee booking list for an event |
| `POST` | `/api/v1/organizer/check-in/scan` | `ORGANIZER`, `ADMIN` | Scan attendee QR pass and verify entry |

### 7. AI Assistant (`/api/v1/assistant`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/assistant/chat` | Public / Auth | Chat with Gemini AI concierge for event discovery |

### 8. Administration (`/api/v1/admin`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/admin/analytics/overview` | `ADMIN` | Platform metrics (revenue, tickets sold, top events/organizers) |
| `GET` | `/api/v1/admin/users` | `ADMIN` | View all platform users and assigned roles |
| `PATCH`| `/api/v1/admin/users/{userId}/promote-to-admin` | `ADMIN` | Promote a user to Administrator |
| `GET` | `/api/v1/admin/organizers/pending` | `ADMIN` | View pending organizer applications |
| `PATCH`| `/api/v1/admin/organizers/{id}/verify` | `ADMIN` | Approve organizer application (grants `ROLE_ORGANIZER`) |
| `DELETE`| `/api/v1/admin/organizers/{id}/reject` | `ADMIN` | Reject organizer application |
| `POST` | `/api/v1/admin/categories` | `ADMIN` | Create new event category |
| `PUT`  | `/api/v1/admin/categories/{id}` | `ADMIN` | Update event category |
| `DELETE`| `/api/v1/admin/categories/{id}` | `ADMIN` | Delete event category |
| `POST` | `/api/v1/admin/venues` | `ADMIN` | Create new venue |
| `PUT`  | `/api/v1/admin/venues/{id}` | `ADMIN` | Update venue details |
| `DELETE`| `/api/v1/admin/venues/{id}` | `ADMIN` | Delete venue |

---

## 🚀 Getting Started

### Prerequisites
- **Java Development Kit (JDK):** Version 21 (LTS)
- **Build Tool:** Apache Maven 3.8+ (or included `mvnw`)
- **Database:** MySQL Server 8.0+ or MySQL 9.x (Local or Aiven Cloud)
- **External Accounts / Keys:**
  - Google Gemini API Key
  - PayHere Merchant Credentials (Sandbox / Live)
  - Brevo SMTP Key (or Gmail App Password)

---

### Installation & Configuration

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/YOUR_USERNAME/eventsphere.git
   cd eventsphere/eventsphere_backend
   ```

2. **Configure Environment Variables:**
   Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```

3. **Populate `.env` with your actual credentials:**
   ```properties
   # Server Port
   PORT=8080

   # Database (Local or Aiven Managed MySQL)
   DB_URL=jdbc:mysql://localhost:3306/eventsphere_db?createDatabaseIfNotExist=true&useSSL=false
   DB_USERNAME=root
   DB_PASSWORD=your_mysql_password

   # Security & Cryptography
   JWT_SECRET=your_jwt_secret_key_at_least_32_bytes_long
   JWT_EXPIRATION_MS=86400000
   TICKET_QR_SECRET=your_ticket_qr_signing_secret_key

   # Google Gemini AI API
   GEMINI_API_KEY=your_gemini_api_key

   # SMTP Email (Brevo / SendGrid / Gmail)
   MAIL_HOST=smtp-relay.brevo.com
   MAIL_PORT=587
   MAIL_USERNAME=your_brevo_smtp_login
   MAIL_PASSWORD=your_brevo_smtp_password
   MAIL_SENDER_EMAIL=eventsphere.tickets@gmail.com

   # PayHere IPG
   PAYHERE_MODE=sandbox
   PAYHERE_MERCHANT_ID=your_merchant_id
   PAYHERE_MERCHANT_SECRET=your_merchant_secret
   PAYHERE_RETURN_URL=http://localhost:5500/pages/my-bookings.html
   PAYHERE_CANCEL_URL=http://localhost:5500/pages/my-bookings.html
   PAYHERE_NOTIFY_URL=http://localhost:8080/api/v1/payments/notify

   # Seeded Default Admin Account
   ADMIN_EMAIL=eventsphere.tickets@gmail.com
   ADMIN_PASSWORD=ChangeMe123!

   # CORS Allowed Origins
   APP_CORS_ALLOWED_ORIGINS=http://localhost:5500,http://localhost:3000,http://127.0.0.1:5500
   ```

4. **Build the Application:**
   ```bash
   mvn clean install
   ```

5. **Run the Application:**
   ```bash
   mvn spring-boot:run
   ```
   *The backend starts at `http://localhost:8080`.*

---

## 🧪 Testing with HTTP Client

Pre-configured HTTP request files are located in `src/main/resources/http/`:
- `01-auth.http`: Registration, OTP verification, login, password reset
- `02-categories.http`: Category CRUD operations
- `04-venues.http`: Venue CRUD operations
- `03-events.http`: Event creation, ticket tier additions, publishing
- `06-userTOorganizer.http`: Organizer applications, admin approvals
- `05-booking.http`: Ticket booking, inventory locking, cancellations
- `07-payment.http`: PayHere checkout initiation, webhook notification simulations

---

## 🔮 Future Enhancements

- [ ] Real-time push notifications via WebSockets.
- [ ] Attendee seat-selection map for assigned seating venues.
- [ ] Native iOS & Android companion mobile applications for gate scanners.
- [ ] Offline gate scanner mode with local cryptographic signature verification.
- [ ] Discount codes and multi-ticket promotional bundles.

---

## 👨‍💻 Project Information

- **Module:** ITS 1114 – Advanced API Development  
- **Institution:** Institute of Java and Software Engineering (IJSE)  
- **License:** Educational Coursework License (IJSE)
