# SmartQueue

A full-stack service queue management system built with Java 17 and Spring Boot 3. SmartQueue models a real-world service workflow — from appointment booking through receptionist check-in and queue management to staff-side service delivery — using formally defined domain lifecycles, persistent state machines, and role-based access control enforced at the framework level.

Built as a Mini Project for the Object-Oriented Analysis and Design course (Group J12, PES University).

---

## Table of Contents

- [Overview](#overview)
- [Technology Stack](#technology-stack)
- [Quick Start](#quick-start)
- [Default Credentials](#default-credentials)
- [Application Pages](#application-pages)
  - [Login and Registration](#login-and-registration)
  - [Customer — Appointments](#customer--appointments)
  - [Reception — Queue Desk](#reception--queue-desk)
  - [Service Staff — Session View](#service-staff--session-view)
  - [Admin — Dashboard](#admin--dashboard)
  - [Admin — User Management](#admin--user-management)
  - [Admin — System Configuration](#admin--system-configuration)
  - [Admin — Metrics Dashboard](#admin--metrics-dashboard)
  - [Admin — Report Generation](#admin--report-generation)
- [Domain Lifecycle Diagrams](#domain-lifecycle-diagrams)
- [Design Patterns](#design-patterns)
- [Architecture](#architecture)
- [Security Model](#security-model)
- [Project Structure](#project-structure)
- [Team](#team)

---
## Overview

SmartQueue coordinates three distinct roles across a shared service workflow:

1. A **customer** books an appointment and confirms it through the web interface.
2. A **receptionist** checks the customer in on arrival and places them into the waiting queue. Walk-in customers can be added directly. The receptionist assigns waiting customers to specific available staff members.
3. A **service staff member** marks themselves available, receives the assignment, serves the customer, and marks the visit complete.
4. An **administrator** manages users, configures system parameters, monitors live metrics, and exports activity reports.

Every domain object — appointment, queue entry, service session — enforces its own lifecycle through the State design pattern. Invalid transitions (such as checking in before confirming, or completing a session that still has an active patient) are rejected at the model layer with typed exceptions, not in controller conditionals.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3.x |
| Web | Spring MVC |
| Templating | Thymeleaf + Thymeleaf Security Extras |
| Security | Spring Security 6 |
| Persistence | Spring Data JPA + Hibernate |
| Database | MySQL 8.4 |
| Build | Maven 3.9 |
| Containerisation | Docker + Docker Compose |

---

## Quick Start

Requires Docker and Docker Compose. No local Java or MySQL installation needed.

```bash
git clone https://github.com/sidk15111/ooad-j12-miniproject.git
cd ooad-j12-miniproject
docker compose up --build
```

The application will be available at `http://localhost:8080`.

The MySQL container includes a health check. The Spring Boot container will not start until the database is accepting connections. Data is persisted in a named Docker volume and survives container restarts.

To stop and remove all containers while preserving data:

```bash
docker compose down
```

To also remove persisted data:

```bash
docker compose down -v
```

---

## Default Credentials

| Username | Password | Role |
|---|---|---|
| admin | admin123 | ADMIN |
| reception | reception123 | RECEPTIONIST |
| customer | customer123 | CUSTOMER |
| staff | staff123 | SERVICE_STAFF |

New customer accounts can also be self-registered from the login page without any admin approval step.

---

## Application Pages

### Login and Registration

**Route:** `/login`

The login page contains two panels side by side. The left panel handles standard username and password login. The right panel allows new customers to self-register with a chosen username and password — the account is created immediately with the CUSTOMER role.

Features:
- Form-based authentication backed by Spring Security
- CSRF protection on all forms
- Flash message display for login errors, logout confirmation, and registration success or failure
- Role-based post-login redirect: each role lands on its own home screen automatically (admin to dashboard, receptionist to queue desk, customer to appointments, staff to session view)
- Accounts deactivated by an administrator are rejected at login; sessions already in progress are terminated by a request filter

---

### Customer — Appointments

**Route:** `/customer/appointments`

The customer's primary view. Appointments are scoped to the logged-in user — each customer sees only their own bookings, not other customers'.

Features:
- Create an appointment by selecting a date and time using a datetime-local input. The customer name is drawn from the authenticated session, eliminating any possibility of booking under a false identity.
- Confirm a pending appointment to signal intent to attend. Only CREATED appointments can be confirmed.
- Cancel an appointment at any stage before check-in. Cancelled appointments cannot be reopened.
- Ownership guard on every action: the server verifies the authenticated user owns the appointment before processing a confirm or cancel request. Attempts to modify another user's appointment are silently blocked with a flash error.
- Automatic expiry on page load: any CREATED, CONFIRMED, or RESCHEDULED appointment whose slot time is more than 30 minutes in the past is automatically transitioned to EXPIRED before the list is rendered.
- The current state object name is displayed alongside the persisted status enum — useful for tracing the state pattern in action.
- Reception handles physical check-in; customers do not check themselves in.

---

### Reception — Queue Desk

**Route:** `/reception/queue`

The central operational view. The receptionist manages the full transition from appointment arrival through queue assignment to staff handoff.

The page is divided into five sections:

**Walk-In Entry**
Add a walk-in customer directly to the waiting queue by entering their name. Walk-in entries carry the `WALK_IN` entry type, which affects ordering under the appointment-priority strategy.

**Confirmed Appointments Awaiting Check-In**
All CONFIRMED appointments are listed here with their slot times. The receptionist can:
- Check in a confirmed appointment, transitioning it to CHECKED_IN so it becomes eligible for queue intake.
- Mark a confirmed appointment as no-show if the customer does not arrive, cancelling the appointment record.

**Checked-In Appointments Ready for Queue**
CHECKED_IN appointments that have not yet been converted to queue entries are listed here. The receptionist clicks "Add To Waiting" to create a queue entry linked to the source appointment ID. The system prevents the same appointment from being added twice.

**Ordered Active Queue**
All WAITING and IN_PROGRESS queue entries, sorted according to the active ordering strategy (configured by admin). For each WAITING entry, the receptionist selects an available staff member from a dropdown and clicks Assign. This:
1. Moves the queue entry to IN_PROGRESS.
2. Links the entry to the selected staff member's session.

The "Assigned Staff" column shows which staff member is currently serving each entry. The staff dropdown is populated in real time — only staff members with an ACTIVE session and no current assignment appear as options.

**All Entries**
A complete historical log of every queue entry, including terminal states (COMPLETED, CANCELLED, NO_SHOW), with appointment reference IDs and assigned staff shown.

---

### Service Staff — Session View

**Route:** `/staff/sessions`

A focused, single-user view. Each staff member sees only their own session — there is no list of all sessions. A session is auto-provisioned for the logged-in user if one does not already exist.

Features:
- Session status panel showing session ID, staff username, current status (IDLE, ACTIVE, PAUSED, COMPLETED), and the currently assigned patient's queue ID and name.
- Mark Available button: transitions the session to ACTIVE (activating from IDLE, or resuming from PAUSED). A staff member cannot have more than one ACTIVE session simultaneously; the constraint is enforced at the database query level.
- Mark Unavailable button: transitions the session to PAUSED. Cannot be triggered while a patient is currently assigned — the staff member must complete their current patient first.
- Complete Assigned Patient button: visible only when a patient is assigned. Marks the queue entry as COMPLETED, clears the assignment from the session, and leaves the session ACTIVE and ready for the next assignment.
- Queue Snapshot section showing all currently WAITING and IN_PROGRESS entries so the staff member can see overall queue activity.

Assignment is initiated by the receptionist, not by the staff member pulling from the queue. This reflects the real operational model where the front desk coordinates workflow.

---

### Admin — Dashboard

**Route:** `/admin/dashboard`

The admin landing page. Provides navigation tiles to the four admin sub-modules: System Configuration, User Management, Metrics Dashboard, and Report Generation.

---

### Admin — User Management

**Route:** `/admin/users`

Full runtime control over user accounts. Users are stored in-memory and survive only the application process lifetime; the architecture is designed for straightforward migration to a database-backed store.

Features:
- View all users with their username, role, and active status in a table.
- Add a new user with a chosen username, password, and role (CUSTOMER, RECEPTIONIST, SERVICE_STAFF, or ADMIN). Passwords are stored as BCrypt hashes. Duplicate usernames are rejected.
- Reactivate a deactivated account.
- Deactivate an account. The deactivation takes effect immediately — a custom request filter (`ActiveUserFilter`) checks account status on every request and terminates active sessions for deactivated users without waiting for their session cookie to expire.
- Delete a user account entirely. The default admin account is protected and cannot be deleted.

---

### Admin — System Configuration

**Route:** `/admin/config`

Runtime configuration for operational parameters.

Features:
- View and update the slot duration in minutes (default: 30). This value is used by the appointment expiry sweep to determine when a confirmed appointment is considered past due. Validated to reject zero or negative values.
- View and switch the active queue ordering strategy. Two strategies are available:
  - **APPOINTMENT_PRIORITY**: appointment-backed entries are sorted ahead of walk-ins. Within each group, entries are ordered by arrival time.
  - **STRICT_FIFO**: all entries regardless of type are sorted purely by arrival time.
- Strategy selection takes effect immediately on the next queue read. The strategy architecture is open for extension — new ordering algorithms can be added as `@Component` classes implementing the `QueueOrderingStrategy` interface, and they appear in this dropdown automatically.

In v1, strategy switching was available to receptionists. In v2 it was moved here, restricting it to administrators as a policy decision.

---

### Admin — Metrics Dashboard

**Route:** `/admin/metrics`

A live system snapshot presented as three side-by-side count tables.

Features:
- Appointment breakdown by status: count of appointments currently in CREATED, CONFIRMED, CHECKED_IN, COMPLETED, CANCELLED, and EXPIRED states. RESCHEDULED is excluded as it is a transitional state rather than an outcome.
- Queue entry breakdown by status: count of entries in WAITING, IN_PROGRESS, COMPLETED, CANCELLED, and NO_SHOW states.
- Service session breakdown by status: count of sessions in IDLE, ACTIVE, PAUSED, and COMPLETED states.

All counts are live database queries, not cached values.

---

### Admin — Report Generation

**Route:** `/admin/reports`

Structured activity reports for a selected date range.

Features:
- Select report type (Daily or Weekly) and a target date.
- Generate report: produces a two-section text report rendered in a monospace block on the page.
  - **Window activity section**: counts appointments (by slot time) and queue entries (by arrival time) that fall within the selected date range, broken down by status.
  - **Live snapshot section**: current total counts across all records in the system, regardless of date, for both appointments and queue entries. Includes total session count.
- Export as .txt: downloads the same report content as a file with a formatted filename (e.g. `smartqueue-daily-2025-01-15.txt`). Implemented as a `ResponseEntity<byte[]>` with `Content-Disposition: attachment` — a proper HTTP file download, not a page render.

---

## Domain Lifecycle Diagrams

### Appointment

```
CREATED ──confirm──► CONFIRMED ──reschedule──► RESCHEDULED
   │                     │                         │
cancel                 cancel                   cancel
expire               checkIn(reception)         confirm
   │                   expire                       │
   ▼                     │                         ▼
CANCELLED             CHECKED_IN ◄────────── CONFIRMED
EXPIRED                   │
                        complete
                           │
                           ▼
                        COMPLETED
```

`CANCELLED`, `EXPIRED`, and `COMPLETED` are terminal. No transition out of these states is permitted.

`RESCHEDULED` requires re-confirmation before check-in. It cannot be expired directly.

---

### Queue Entry

```
WAITING ──startService──► IN_PROGRESS ──completeService──► COMPLETED
   │                           │
markNoShow               markNoShow
cancel                   cancel
   │                     releaseToWaiting (back to WAITING)
   ▼                           │
NO_SHOW                   CANCELLED
CANCELLED
```

`COMPLETED`, `CANCELLED`, and `NO_SHOW` are terminal.

`releaseToWaiting` on an IN_PROGRESS entry sends it back to WAITING — used when a staff member releases an assignment before completing it.

---

### Service Session

```
IDLE ──activate──► ACTIVE ──pause──► PAUSED
                      │                  │
                   complete           resume
                      │                  │
                      ▼              ──► ACTIVE
                   COMPLETED
                      ▲
                      │
                   complete (also reachable from PAUSED)
```

A staff member cannot hold more than one ACTIVE session simultaneously. The constraint is enforced via a database existence check: `existsByStaffUsernameAndStatusAndIdNot`.

A session cannot be completed while a queue entry is assigned to it.

---

## Design Patterns

### State Pattern

Applied independently to all three domain entities: `Appointment`, `QueueEntry`, and `ServiceSession`.

Each entity holds a `@Transient` state object (not persisted) alongside a `@Enumerated(EnumType.STRING)` status column (persisted). The `@PostLoad` JPA lifecycle callback `hydrateState()` reconstructs the correct state class from the persisted enum every time the entity is loaded from the database. This keeps the relational schema flat while the object model retains rich, polymorphic behavior.

Every public action method on the entity calls `hydrateState()` before delegating to the current state object. State objects call `transitionTo(nextState, nextStatus)` on the entity to record a legal transition. Illegal transitions throw typed exceptions (`InvalidAppointmentTransitionException`, `InvalidQueueTransitionException`, `InvalidServiceSessionTransitionException`) which propagate through the service layer and are caught in a controller-level helper that surfaces them as flash error messages.

| Interface | Concrete States |
|---|---|
| `AppointmentState` | Created, Confirmed, Rescheduled, CheckedIn, Completed, Cancelled, Expired |
| `QueueState` | Waiting, InProgress, Completed, Cancelled, NoShow |
| `ServiceSessionState` | Idle, Active, Paused, Completed |

### Strategy Pattern

Applied to queue ordering in `QueueService`.

`QueueOrderingStrategy` defines two methods: `orderQueue(List<QueueEntry>)` and `key()`. Both concrete implementations are Spring `@Component` beans, allowing Spring to auto-discover and inject them as a `List<QueueOrderingStrategy>` into `QueueService`. The service builds a `ConcurrentHashMap` keyed by `strategy.key()` at construction time.

Adding a new ordering algorithm requires no changes to existing code — a new `@Component` class implementing the interface is sufficient. This is a direct application of the Open/Closed Principle.

The active strategy is switchable at runtime by administrators through the System Configuration page.

---

## Architecture

```
src/main/java/com/pes/smartqueue/
├── config/
│   ├── SecurityConfig.java          Spring Security filter chain, UserDetailsService, BCrypt bean
│   └── ActiveUserFilter.java        OncePerRequestFilter: terminates sessions for deactivated users
├── controller/
│   ├── HomeController.java          Root redirect, customer self-registration
│   ├── CustomerController.java      /customer/** — appointment CRUD with ownership guard
│   ├── ReceptionistController.java  /reception/** — check-in, queue intake, staff assignment
│   ├── ServiceStaffController.java  /staff/** — availability toggle, patient completion
│   └── AdminController.java         /admin/** — users, config, metrics, reports
├── service/
│   ├── AppointmentService.java      Appointment lifecycle, expiry sweep
│   ├── QueueService.java            Queue CRUD, strategy registry, synchronized startNext
│   ├── ServiceSessionService.java   Session lifecycle, availability facade, cross-domain assignment
│   ├── UserManagementService.java   Runtime user store, BCrypt, role validation
│   ├── SystemConfigService.java     Slot duration, strategy delegation
│   ├── MetricsService.java          Live count queries across all three domains
│   └── ReportService.java           Windowed and live report generation, file export
├── repository/
│   ├── AppointmentRepository.java
│   ├── QueueEntryRepository.java
│   └── ServiceSessionRepository.java
├── model/
│   ├── appointment/                 Appointment entity + AppointmentStatus enum
│   ├── queue/                       QueueEntry entity + QueueStatus + EntryType enums
│   └── session/                     ServiceSession entity + ServiceSessionStatus enum
├── pattern/
│   ├── state/appointment/           AppointmentState interface + 7 concrete classes
│   ├── state/queue/                 QueueState interface + 5 concrete classes
│   ├── state/session/               ServiceSessionState interface + 4 concrete classes
│   └── strategy/                    QueueOrderingStrategy interface + 2 concrete classes
└── exception/
    ├── InvalidAppointmentTransitionException.java
    ├── InvalidQueueTransitionException.java
    └── InvalidServiceSessionTransitionException.java
```

---

## Security Model

Security is configured in `SecurityConfig` using Spring Security 6's `SecurityFilterChain` bean.

**Route Authorization**

| Route Pattern | Permitted Roles |
|---|---|
| `/admin/**` | ADMIN |
| `/reception/**` | RECEPTIONIST, ADMIN |
| `/customer/**` | CUSTOMER, ADMIN |
| `/staff/**` | SERVICE_STAFF, ADMIN |
| `/login`, `/register/customer`, `/css/**` | Public |

**Runtime Deactivation**

`ActiveUserFilter` extends `OncePerRequestFilter` and is registered after `AnonymousAuthenticationFilter` in the security chain. On every authenticated request, it calls `UserManagementService.isUserActive(username)`. If the account has been deactivated since the session was established, the security context is cleared and the user is redirected to `/access-denied` immediately — without waiting for their session to expire naturally.

**Password Storage**

All passwords are encoded with BCrypt. The `PasswordEncoder` bean is defined separately and injected wherever encoding or verification is needed.

**CSRF Protection**

CSRF tokens are included on all state-mutating forms via Thymeleaf's `th:name="${_csrf.parameterName}"` pattern. Spring Security validates the token on every POST request.

---

## Project Structure

```
ooad-j12-miniproject/
├── src/
│   ├── main/
│   │   ├── java/com/pes/smartqueue/     Application source
│   │   └── resources/
│   │       ├── templates/               Thymeleaf HTML templates
│   │       │   ├── admin/               dashboard, config, users, metrics, reports
│   │       │   ├── customer/            appointments
│   │       │   ├── reception/           queue
│   │       │   ├── staff/               sessions
│   │       │   ├── login.html
│   │       │   ├── home.html
│   │       │   └── access-denied.html
│   │       └── application.properties   Environment-variable-overridable datasource config
│   └── test/
│       └── java/com/pes/smartqueue/
│           ├── ServiceRulesIntegrationTest.java      State machine rules, concurrency race test
│           └── WorkflowEndToEndIntegrationTest.java  Full customer-to-staff journey via MockMvc
├── Dockerfile                           Multi-stage build: Maven build layer + JRE runtime layer
├── docker-compose.yml                   MySQL 8.4 + app, health-check dependency
└── pom.xml
```

---

## Team

| Name | Roll | Domain |
|---|---|---|
| Sujay Haridas | 608 | Appointment Lifecycle and Customer Domain |
| Siddhartha Kumar | 577 | Queue Management and Reception Domain |
| Pranavjeet Naidu | 586 | Session Management and Service Staff Domain |
| Subham Dey | 604 | UI, RBAC, Admin Module, and System Infrastructure |
