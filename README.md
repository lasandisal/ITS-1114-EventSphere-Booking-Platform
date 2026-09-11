# 🎟️ EventSphere Backend – Centralized Event Discovery & Ticketing Platform

<p align="center">
  <img src="https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Spring%20Security-6.x-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security" />
  <img src="https://img.shields.io/badge/Thymeleaf-3.x-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white" alt="Thymeleaf" />
  <img src="https://img.shields.io/badge/MySQL-8.0%2B-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL" />
  <img src="https://img.shields.io/badge/PayHere-IPG%20Gateway-0052CC?style=for-the-badge" alt="PayHere" />
  <img src="https://img.shields.io/badge/Brevo-Transactional%20SMTP-0B99FF?style=for-the-badge&logo=brevo&logoColor=white" alt="Brevo SMTP" />
  <img src="https://img.shields.io/badge/Google%20Gemini-3.5%20Flash%20Lite-8E75B2?style=for-the-badge&logo=google&logoColor=white" alt="Google Gemini" />
  <img src="https://img.shields.io/badge/Docker-Containerized-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
  <img src="https://img.shields.io/badge/Render-Cloud%20Hosted-46E3B7?style=for-the-badge&logo=render&logoColor=white" alt="Render" />
  <a href="https://docs.google.com/document/d/1X6XspO6Vx_Vn0Wct_69B55BjL2htt4JV/edit?usp=sharing&ouid=105743732836393895401&rtpof=true&sd=true"><img src="https://img.shields.io/badge/Google%20Docs-Project%20Report-4285F4?style=for-the-badge&logo=googledocs&logoColor=white" alt="Project Report" /></a>
</p>

> **Course:** Final Coursework  
> **Module:** ITS 1114 – Advanced API Development  
> **Institution:** Institute of Java and Software Engineering (IJSE)  
> **Author:** Lasandi Salwathura  
> **Project Documentation:** [EventSphere Project Report (Google Docs)](https://docs.google.com/document/d/1X6XspO6Vx_Vn0Wct_69B55BjL2htt4JV/edit?usp=sharing&ouid=105743732836393895401&rtpof=true&sd=true)  
> **Live API URL:** [https://its-1114-eventsphere-booking-platform.onrender.com](https://its-1114-eventsphere-booking-platform.onrender.com)  
> **Frontend Application:** [https://eventsphere-webapp.vercel.app](https://eventsphere-webapp.vercel.app)

---

## 📖 System Overview

**EventSphere** is an enterprise-grade RESTful event management and ticketing platform built to eliminate the fragmented, error-prone workflows common in traditional ticketing (manual spreadsheets, messaging apps, and insecure PDF tickets).

The backend engine provides a secure, high-concurrency API powering three primary user personas:
- **Attendees (`ROLE_USER`):** Real-time event discovery with category and location filters, dynamic ticket selection with a 10-minute temporary inventory hold, PayHere IPG payment checkout, and cryptographically signed QR admission passes delivered via Brevo transactional email.
- **Event Organizers (`ROLE_ORGANIZER`):** Business verification onboarding, custom venue creation, scheduling collision prevention, multi-tiered pricing management, real-time sales analytics, and high-speed gate admission scanning via QR camera verification.
- **Platform Administrators (`ROLE_ADMIN`):** Verification and governance of organizer applications, user lifecycle moderation, platform-wide revenue audits, and global category/venue oversight.

---

## 🛠️ Technology Stack Breakdown & Architecture Rationale

| Technology / Library | Version / Spec | Purpose & Architectural Justification |
| :--- | :--- | :--- |
| **Java Platform** | **Java 21 (LTS)** | Leverages modern LTS language features (Pattern Matching, Sealed Classes, Records, and enhanced switch expressions) alongside high-throughput memory management. |
| **Framework** | **Spring Boot 3.2.4** | Core inversion-of-control (IoC) container, REST controllers, declarative transaction management (`@Transactional`), and automated task scheduling (`@EnableScheduling`). |
| **Security** | **Spring Security 6 + JJWT 0.11.5** | Stateless JWT authentication filter (`OncePerRequestFilter`), role-based endpoint guards (`@PreAuthorize`), and BCrypt password hashing (cost factor 12). |
| **Persistence / ORM** | **Spring Data JPA & Hibernate** | Relational mapping for 15 domain entities, pessimistic database row locking, custom JPQL queries, and HikariCP connection pooling. |
| **Database** | **MySQL 8+ (Local & Aiven Cloud)** | ACID-compliant relational storage with strict foreign keys, transactional boundaries, and row-level locking support. |
| **Template Engine** | **Spring Boot Thymeleaf 3.x** | Decouples HTML email presentation from Java business logic. Provides auto-escaping to prevent XSS, designer previewability, and dynamic loop binding. |
| **Transactional Email** | **Brevo (formerly Sendinblue) SMTP** | Cloud-native transactional mail delivery on port 587 (STARTTLS). Bypasses personal SMTP restrictions, ensures high inbox deliverability, and avoids cloud IP blacklisting. |
| **Payment Gateway** | **PayHere IPG** | Central Bank of Sri Lanka (CBSL) approved internet payment gateway with HMAC/MD5 callback signature verification and idempotent webhook processing. |
| **Artificial Intelligence** | **Google Gemini API (`gemini-3.5-flash-lite`)** | Conversational event assistant operating with server-side Function/Tool Calling (`search_events`, `get_event_details`, `get_my_bookings`) restricted to read-only queries. |
| **QR Code Engine** | **Google ZXing 3.5.3** | Generates high-density PNG QR code byte arrays signed with **HMAC-SHA256** signatures to guarantee ticket authenticity at venue gates. |
| **Containerization** | **Docker (Eclipse Temurin 21 JRE)** | Multi-stage production container build resulting in an optimized, minimal Linux container deployed on Render. |

---

## 💌 Why Brevo SMTP & Why Thymeleaf? (Engineering Deep-Dive)

### 1. Why Brevo SMTP over Standard Gmail?
In local development, many developers configure `smtp.gmail.com` with a personal Google App Password. However, deploying a production SaaS backend on cloud providers (Render, AWS, DigitalOcean) using personal Gmail SMTP fails for several critical reasons:
- **Cloud IP Blacklisting:** Cloud hosting platforms share IP pools. Major mail servers (Google, Microsoft) frequently flag or throttle outbound SMTP traffic emerging directly from cloud hosting ranges.
- **Strict Personal Rate Limits:** Gmail caps personal accounts at 100–500 emails/day. In an event platform where a single order can dispatch 4+ emails (purchaser receipt + individual attendee passes + registration OTPs), personal limits are breached instantly.
- **Account Suspensions:** Automating transaction receipts through personal Gmail accounts violates Google's Acceptable Use Policy and risks sudden credential revocation.
- **The Brevo Advantage:**
  - **Dedicated Transactional Infrastructure:** Brevo provides authenticated relay infrastructure configured with proper SPF, DKIM, and DMARC alignment.
  - **Port 587 STARTTLS Support:** Fully compatible with Spring Boot's `JavaMailSender` and cloud hosting firewall policies.
  - **Generous Free Tier:** 300 free transactional emails per day with zero credit card required, providing professional delivery for coursework and production demos.
  - **Comprehensive Audit Logs:** Web dashboard displays real-time delivery status, opens, bounces, and latency metrics.

### 2. Why Thymeleaf Templates over In-Code HTML Strings?
Earlier prototypes assembled email markup using string concatenations (`+ "<div>" + ...`). This was replaced with **Thymeleaf HTML templates** under `src/main/resources/templates/mail/`:
- **Separation of Concerns (SoC):** Java service classes (`EmailServiceImpl.java`) focus purely on transaction boundaries, MIME headers, and email dispatch, leaving presentation markup in clean `.html` files.
- **Security & Auto-Escaping:** Thymeleaf automatically sanitizes and escapes dynamic parameters (`th:text="${var}"`), eliminating HTML injection and XSS vulnerabilities.
- **Designer & Tooling Friendly:** Email templates can be opened and styled in standard HTML/CSS editors with full syntax highlighting, CSS linting, and email-client previewability.
- **Maintainability:** Changing an email logo, adjusting typography, or updating color schemes requires zero Java code recompilation.

---

## 🏛️ System Architecture

EventSphere strictly adheres to a **three-tier layered architecture**:

```
                             Client Applications
               (Vercel Web App / Gate QR Scanner / Postman)
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
  │ AI Assistant │   │ Webhook IPG  │  │  (Thymeleaf) │   │ QR Generator │
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

## 💡 Core Engineering Highlights

### 1. Anti-Overselling via Database Pessimistic Locking
In popular events, hundreds of concurrent checkouts can trigger race conditions where two users purchase the final remaining seat simultaneously. EventSphere eliminates overselling using row-level pessimistic locking (`PESSIMISTIC_WRITE`):

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT t FROM TicketType t WHERE t.id = :id")
Optional<TicketType> lockById(@Param("id") Long id);
```
During booking creation, Hibernate issues a `SELECT ... FOR UPDATE` query, locking the specific ticket type row for the microsecond duration of the transaction. Stock is validated and decremented safely before releasing the lock.

### 2. 10-Minute Cart Hold & Automated Expiry Scheduler
When an attendee selects tickets and proceeds to checkout:
1. `createBooking()` sets status to `PENDING` and records `expiresAt = LocalDateTime.now().plusMinutes(10)`.
2. Tickets are provisionally decremented so other attendees see real-time availability.
3. **Background Scheduler:** Enabled via `@EnableScheduling` on `EventsphereApplication`, the `BookingExpiryScheduler` runs:
   - On server startup (`initialDelay = 5000ms`) to immediately clear holds abandoned while the server was deploying or sleeping.
   - Every 60 seconds (`fixedRate = 60000ms`) to automatically mark stale bookings as `EXPIRED` and return the held inventory to `ticket_types.available_quantity`.

### 3. Duplicate Event & Venue Scheduling Collision Prevention
EventSphere employs enterprise-grade collision detection in both `createEvent` and `updateEvent`:
- **Organizer Duplicate Rule:** Prevents an organizer from accidentally creating multiple active events with the same title on the same calendar day using index-friendly day boundaries:
  ```sql
  SELECT COUNT(e) > 0 FROM Event e
  WHERE e.organizer.id = :organizerId
    AND LOWER(TRIM(e.title)) = LOWER(TRIM(:title))
    AND e.startDatetime >= :startOfDay AND e.startDatetime <= :endOfDay
    AND e.status != 'CANCELLED'
    AND (:excludeEventId IS NULL OR e.id != :excludeEventId)
  ```
- **Venue Double-Booking Rule:** Enforces the mathematical interval collision condition across all active events sharing a physical venue:
  $$\text{NewStart} < \text{ExistingEnd} \land \text{NewEnd} > \text{ExistingStart}$$
  If another active event overlaps the selected venue window, the API immediately halts execution with an HTTP `409 Conflict`.

### 4. Cryptographically Signed Gate Admission Passes (HMAC-SHA256)
To prevent ticket counterfeiting and screenshot reuse:
- The QR code contains a base64-encoded payload consisting of the unique ticket UUID combined with an **HMAC-SHA256 signature** generated using a server-side secret key:
  $$\text{Payload} = \text{Base64Url}(\text{ticketCode} + ":" + \text{HMAC-SHA256}(\text{ticketCode}, \text{secret}))$$
- When scanned at gate check-in (`/api/v1/organizer/check-in/scan`), the server recalculates and verifies the cryptographic hash in $O(1)$ time. Forged tickets are rejected immediately without database overhead.
- Valid tickets transition to `USED` status. Duplicate scan attempts trigger an immediate `409 Conflict` (`TicketAlreadyUsedException`) and log the security violation.

### 5. Idempotent PayHere Webhook Processing
- PayHere dispatches payment status notifications to `/api/v1/payments/notify`.
- The webhook endpoint verifies the MD5 checksum (`merchant_id + order_id + payhere_amount + payhere_currency + status_code + strtoupper(md5(merchant_secret))`).
- **Idempotency Guarantee:** If PayHere retries the webhook due to network latency, the service detects that the booking is already `CONFIRMED`, returns HTTP 200 immediately, and avoids duplicate ticket generation or re-sending confirmation emails.

---

## 🗄️ Relational Entity-Relationship Diagram (15 Entities)

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

---

## 📁 Backend Repository Directory Structure

```
eventsphere_backend/
├── src/
│   ├── main/
│   │   ├── java/lk/ijse/eventsphere/
│   │   │   ├── EventsphereApplication.java        # Main entry point (@EnableScheduling, @EnableAsync)
│   │   │   ├── config/                           # SecurityConfig, CorsConfig, Schedulers, Properties
│   │   │   │   ├── BookingExpiryScheduler.java   # 60-second recurring hold expiry scheduler
│   │   │   │   ├── PayHereProperties.java        # Strongly-typed PayHere config
│   │   │   │   └── SecurityConfig.java           # Spring Security 6 filter chain & CORS rules
│   │   │   ├── controller/                       # REST endpoints (Auth, Events, Bookings, Payments, etc.)
│   │   │   │   ├── AdminController.java          # Admin governance, analytics, approvals
│   │   │   │   ├── AssistantController.java      # Gemini AI chat concierge
│   │   │   │   ├── AuthController.java           # Login, register, OTP verification
│   │   │   │   ├── BookingController.java        # Booking creation and user tickets
│   │   │   │   ├── EventController.java          # Public event discovery & catalog
│   │   │   │   ├── OrganizerController.java      # Organizer events, tiers, check-in scanning
│   │   │   │   ├── OrganizerVenueController.java # Organizer custom venue registration
│   │   │   │   └── PaymentController.java        # PayHere checkout initiate & webhook callback
│   │   │   ├── dto/                              # Request & Response Data Transfer Objects
│   │   │   ├── entity/                           # 15 JPA Domain Entities
│   │   │   ├── enums/                            # RoleName, BookingStatus, EventStatus, etc.
│   │   │   ├── exception/                        # GlobalExceptionHandler & domain exceptions
│   │   │   ├── repository/                       # Spring Data JPA interfaces with custom JPQL queries
│   │   │   ├── security/                         # JwtAuthFilter, JwtTokenProvider, CurrentUserProvider
│   │   │   ├── service/                          # Service interfaces and implementations
│   │   │   │   └── impl/
│   │   │   │       ├── BookingServiceImpl.java   # Pessimistic locking & hold management
│   │   │   │       ├── EmailServiceImpl.java     # Thymeleaf-based multi-recipient mail service
│   │   │   │       ├── EventServiceImpl.java     # Event lifecycle, duplicates, collisions
│   │   │   │       └── GeminiAssistantService.java # AI tool execution engine
│   │   │   └── util/                             # PayHereSignatureUtil, TicketSigningUtil, QrCodeGenerator
│   │   └── resources/
│   │       ├── application.properties            # Core Spring configuration
│   │       ├── application-local.properties      # Local development profile
│   │       ├── application-prod.properties       # Render cloud production profile
│   │       ├── http/                             # HTTP Client test scripts (01-auth to 07-payment)
│   │       └── templates/mail/                   # Thymeleaf HTML Email Templates
│   │           ├── otp-verification.html         # User sign-up verification OTP
│   │           ├── password-reset-otp.html       # Password recovery OTP
│   │           ├── order-receipt.html            # Master purchaser order receipt & passes
│   │           └── ticket-pass.html              # Individual attendee admission pass
│   └── test/                                     # MockMvc integration tests & unit suites
├── Dockerfile                                    # Multi-stage production container definition
├── pom.xml                                       # Maven build file with all dependencies
└── README.md                                     # Backend documentation
```

---

## 📡 REST API Catalog Overview

All endpoints return a standardized envelope response:
```json
{
  "code": 200,
  "status": 200,
  "message": "Operation completed successfully",
  "data": { ... }
}
```

### 1. Authentication & Security (`/api/v1/auth`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Public | Register account; triggers Brevo verification OTP email |
| `POST` | `/api/v1/auth/login` | Public | Authenticate user; returns JWT token and role profile |
| `POST` | `/api/v1/auth/verify-otp` | Public | Activate account using 6-digit email OTP |
| `POST` | `/api/v1/auth/resend-otp` | Public | Resend account verification OTP email |
| `POST` | `/api/v1/auth/forgot-password` | Public | Request password reset OTP |
| `POST` | `/api/v1/auth/reset-password` | Public | Set new password using valid OTP code |

### 2. Event Discovery & Catalog (`/api/v1/events`, `/api/v1/categories`, `/api/v1/venues`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/events` | Public | Paginated event list with search keyword, category, and date filtering |
| `GET` | `/api/v1/events/{id}` | Public | Full event details with live ticket tier availability |
| `GET` | `/api/v1/categories` | Public | Retrieve all platform event categories |
| `GET` | `/api/v1/venues` | Public | List all available venues |

### 3. Bookings & Inventory (`/api/v1/bookings`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/bookings` | `USER`, `ORGANIZER`, `ADMIN` | Reserve tickets with 10-minute hold (pessimistic lock) |
| `GET` | `/api/v1/bookings/{id}` | Authenticated Owner | Retrieve booking summary and attendee tickets |
| `GET` | `/api/v1/bookings` | Authenticated Owner | Paginated user bookings by tab (`upcoming`, `past`, `cancelled`) |
| `PATCH`| `/api/v1/bookings/{id}/cancel`| Authenticated Owner | Cancel pending booking and release held inventory |

### 4. Payments & Webhooks (`/api/v1/payments`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/payments/initiate/{bookingId}` | Authenticated Owner | Generate PayHere checkout parameters and cryptographic hash |
| `POST` | `/api/v1/payments/notify` | Public (PayHere IPG) | Webhook callback; validates MD5 signature, confirms booking, issues passes |

### 5. Organizer Operations (`/api/v1/organizer`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/organizer/apply` | `USER` | Submit business verification application (BRN, NIC/Passport) |
| `GET` | `/api/v1/organizer/profile` | `ORGANIZER` | View organizer verification profile and status |
| `POST` | `/api/v1/organizer/venues` | `ORGANIZER`, `ADMIN` | Register a new custom venue for immediate event selection |
| `POST` | `/api/v1/organizer/events` | `ORGANIZER`, `ADMIN` | Create event with duplicate title & venue collision checks |
| `POST` | `/api/v1/organizer/events/{id}/ticket-types` | `ORGANIZER`, `ADMIN` | Configure ticket tiers (name, price, capacity) |
| `PUT` | `/api/v1/organizer/events/{id}` | `ORGANIZER`, `ADMIN` | Update event information |
| `PATCH`| `/api/v1/organizer/events/{id}/publish` | `ORGANIZER`, `ADMIN` | Publish draft event for public booking |
| `GET` | `/api/v1/organizer/events/my-events` | `ORGANIZER`, `ADMIN` | List organizer's managed events |
| `GET` | `/api/v1/organizer/events/{id}/bookings` | `ORGANIZER`, `ADMIN` | Retrieve attendee manifests and ticket sales data |
| `POST` | `/api/v1/organizer/check-in/scan` | `ORGANIZER`, `ADMIN` | Verify cryptographic QR pass and admit attendee |

### 6. AI Event Concierge (`/api/v1/assistant`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/assistant/chat` | Public / Authenticated | Chat with Google Gemini AI assistant for real-time recommendations |

### 7. Platform Administration (`/api/v1/admin`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/admin/analytics/overview` | `ADMIN` | Global platform metrics (gross revenue, tickets sold, top events) |
| `GET` | `/api/v1/admin/users` | `ADMIN` | Manage user accounts, status, and role assignments |
| `PATCH`| `/api/v1/admin/users/{userId}/promote-to-admin` | `ADMIN` | Promote a user account to Administrator role |
| `GET` | `/api/v1/admin/organizers/pending` | `ADMIN` | View pending organizer business verification submissions |
| `PATCH`| `/api/v1/admin/organizers/{id}/verify` | `ADMIN` | Approve organizer application (grants `ROLE_ORGANIZER`) |
| `DELETE`| `/api/v1/admin/organizers/{id}/reject` | `ADMIN` | Reject organizer application with feedback |
| `POST` | `/api/v1/admin/categories` | `ADMIN` | Create new event category |
| `POST` | `/api/v1/admin/venues` | `ADMIN` | Create global venue record |

---

## 🚀 Getting Started & Local Setup

### Prerequisites
- **Java 21 (LTS):** OpenJDK or Eclipse Temurin
- **Maven 3.8+:** Or use the included `mvnw.cmd` / `mvnw` wrapper
- **MySQL 8.0+:** Local server or cloud managed database (Aiven)
- **Brevo SMTP Account:** For transactional email delivery (Free tier available)
- **PayHere Merchant Account:** Sandbox credentials for testing payments
- **Google Gemini API Key:** For the AI assistant integration

---

### Step-by-Step Installation

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/lasandisal/ITS-1114-EventSphere-Booking-Platform.git
   cd ITS-1114-EventSphere-Booking-Platform/eventsphere_backend
   ```

2. **Configure Environment Variables (`.env`):**
   Copy `.env.example` to `.env` in the root of `eventsphere_backend`:
   ```properties
   # Server Port
   PORT=7080

   # Active Profile
   SPRING_PROFILES_ACTIVE=local

   # Database Connection (Local MySQL or Aiven Cloud)
   DB_URL=jdbc:mysql://localhost:3306/eventsphere_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
   DB_USERNAME=root
   DB_PASSWORD=your_mysql_password

   # JWT Security
   JWT_SECRET=your_super_secret_jwt_key_at_least_32_characters_long_min
   JWT_EXPIRATION_MS=86400000

   # Ticket Cryptographic QR Signing
   TICKET_QR_SECRET=your_ticket_qr_signing_secret_key_random_string

   # Brevo Transactional SMTP
   MAIL_HOST=smtp-relay.brevo.com
   MAIL_PORT=587
   MAIL_USERNAME=your_brevo_login_email
   MAIL_PASSWORD=your_brevo_smtp_master_key
   MAIL_SENDER_EMAIL=eventsphere.tickets@gmail.com

   # PayHere IPG Sandbox
   PAYHERE_MODE=sandbox
   PAYHERE_MERCHANT_ID=your_merchant_id
   PAYHERE_MERCHANT_SECRET=your_merchant_secret
   PAYHERE_RETURN_URL=http://localhost:5500/pages/my-bookings.html
   PAYHERE_CANCEL_URL=http://localhost:5500/pages/booking.html
   PAYHERE_NOTIFY_URL=http://localhost:7080/api/v1/payments/notify

   # Google Gemini API
   GEMINI_API_KEY=your_gemini_api_key

   # CORS Allowed Origins
   APP_CORS_ALLOWED_ORIGINS=http://localhost:5500,http://127.0.0.1:5500,http://localhost:3000,https://eventsphere-webapp.vercel.app
   ```

3. **Build the Backend:**
   ```bash
   ./mvnw clean install
   ```

4. **Run the Application:**
   ```bash
   ./mvnw spring-boot:run
   ```
   *The server initializes on port `7080` (or the configured `PORT`).*

---

## 🐳 Docker Deployment

The application includes an optimized multi-stage `Dockerfile`:

```bash
# 1. Build the Docker image
docker build -t eventsphere-backend:latest .

# 2. Run the Docker container
docker run -p 7080:7080 --env-file .env eventsphere-backend:latest
```

---

## 🧪 Automated Testing

The test suite validates context configuration, collision rejection, custom venue onboarding, scheduled booking expiration, and Thymeleaf template rendering:

```bash
# Run all integration and unit tests
./mvnw test
```

All 5 core test suites run against an in-memory/isolated MySQL configuration and execute with 100% success:
- `contextLoads`: Verifies Spring context, scheduling, and async thread initialization.
- `testOrganizerCreateCustomVenue`: Validates custom venue registration endpoint (`201 Created`).
- `testDuplicateEventAndVenueCollisionRejection`: Tests duplicate event title and venue interval overlap rejection (`409 Conflict`).
- `testStaleBookingExpiryAndInventoryRestoration`: Validates that expired `PENDING` bookings release held tickets back to available stock.
- `testEmailThymeleafTemplatesRenderProperly`: Validates that all 4 Thymeleaf email templates render without error and interpolate context variables properly.

---

## 🔒 Security & Privacy Standards

- **Zero Secret Exposure:** Backend credentials (JWT secret, DB password, Gemini API key, Brevo SMTP keys, PayHere secrets) are strictly managed via environment variables and never committed to version control.
- **Salted Password Hashing:** User passwords are encrypted using BCrypt with a cost factor of 12.
- **PCI-DSS Compliance via PayHere:** The platform never collects, transmits, or stores cardholder credit/debit card numbers. All payments occur within PayHere's secure hosted payment gateway.
- **Stateless RBAC:** APIs are secured with stateless JWT tokens verified on every request using custom security filters with role checks.
- **Replay-Proof Gate Scanning:** Tickets transition immediately to `USED` upon gate check-in with immutable audit logs recording the scan time, staff user, and location notes.

---

## 🎓 Academic Module Information

- **Coursework:** Final Coursework
- **Module:** ITS 1114 – Advanced API Development
- **Institution:** Institute of Java and Software Engineering (IJSE)
- **Author:** Lasandi Salwathura
- **Project Report / Documentation:** [EventSphere Project Report (Google Docs)](https://docs.google.com/document/d/1X6XspO6Vx_Vn0Wct_69B55BjL2htt4JV/edit?usp=sharing&ouid=105743732836393895401&rtpof=true&sd=true)
