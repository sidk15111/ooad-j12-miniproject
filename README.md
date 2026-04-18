# Smart Queue System (OOAD Mini Project)

## 1. Project Overview
Smart Queue System is a Java 17 + Spring Boot 3 application that models and executes a real service workflow where:
- Customers create and manage appointments.
- Receptionists convert checked-in appointments (and walk-ins) into queue entries.
- Service staff process queue entries through service sessions.
- Admin can access all role modules.

The implementation follows Object-Oriented Analysis and Design principles and maps directly to the domain lifecycles defined in the project diagrams.

## 2. What Was Implemented
This project evolved from a plain Java OOAD foundation to a full Spring Boot web application with persistence and deployment support.

### 2.1 Core Framework and Stack
- Java 17
- Spring Boot 3.3.x
- Spring MVC + Thymeleaf (Java-only frontend)
- Spring Security (RBAC)
- Spring Data JPA + MySQL
- Docker + Docker Compose for one-command startup

### 2.2 Full Workflow Correlation Across Pages
The following correlation is now implemented end-to-end:
1. Customer creates appointment.
2. Customer confirms and checks in.
3. Reception sees checked-in appointments and adds them to the queue.
4. Staff activates a service session.
5. Staff session starts the next queue entry.
6. Staff completes assigned queue work.

This solves the earlier isolation issue between pages/services.

### 2.3 Persistence Migration Completed
The application was migrated from in-memory maps to persistent MySQL-backed entities and repositories.
Data now survives app/container restarts.

## 3. High-Level Architecture
The codebase follows layered MVC architecture with domain-driven service orchestration:

- Controller layer: Handles HTTP requests and view model binding.
- Service layer: Contains business rules and orchestration logic.
- Repository layer: Persistence abstraction via Spring Data interfaces.
- Domain model layer: Entities + state-driven behavior.
- Pattern layer: State and Strategy pattern contracts and concrete implementations.
- Security layer: Authentication + authorization policies.

## 4. OOAD Terms and Concepts Used

### 4.1 Domain Model
Key domain entities:
- Appointment
- QueueEntry
- ServiceSession

Each entity has lifecycle state, legal transitions, and role-driven operations.

### 4.2 State Pattern (Behavior by State)
Instead of large conditional blocks, state-specific classes govern legal transitions.

State interfaces used:
- src/main/java/com/pes/smartqueue/pattern/state/appointment/AppointmentState.java
- src/main/java/com/pes/smartqueue/pattern/state/queue/QueueState.java
- src/main/java/com/pes/smartqueue/pattern/state/session/ServiceSessionState.java

Concrete state classes implement transition rules and invalid-transition behavior.

Benefit:
- Explicit lifecycle control.
- Better extensibility.
- Cleaner testability.

### 4.3 Strategy Pattern (Pluggable Queue Ordering)
Queue ordering is abstracted behind strategy interface:
- src/main/java/com/pes/smartqueue/pattern/strategy/QueueOrderingStrategy.java

Concrete strategies can be switched at runtime (e.g., appointment-priority, strict FIFO).

Benefit:
- Open/Closed Principle: add new ordering strategies without changing queue service core.

### 4.4 SOLID Principles Reflected
- Single Responsibility: controllers handle web concerns, services handle business rules, repositories handle persistence.
- Open/Closed: state and strategy behavior extended via new classes.
- Liskov Substitution: concrete states/strategies substituted via interface types.
- Interface Segregation: small focused behavior contracts (state interfaces).
- Dependency Inversion: services depend on abstractions (repositories, strategy interface), injected by Spring.

### 4.5 RBAC (Role-Based Access Control)
Roles:
- RECEPTIONIST
- CUSTOMER
- SERVICE_STAFF
- ADMIN

Role guards are configured in security configuration and enforced per route.

## 5. Package-by-Package Breakdown

### 5.1 config
- src/main/java/com/pes/smartqueue/config/SecurityConfig.java

Defines:
- Route authorization rules
- Form login/logout
- Access denied page
- In-memory seed users for current phase

### 5.2 controller
- src/main/java/com/pes/smartqueue/controller/HomeController.java
- src/main/java/com/pes/smartqueue/controller/CustomerController.java
- src/main/java/com/pes/smartqueue/controller/ReceptionistController.java
- src/main/java/com/pes/smartqueue/controller/ServiceStaffController.java

Responsibilities:
- Receive requests
- Call service methods
- Handle success/error feedback via redirect attributes
- Bind model data for Thymeleaf views

### 5.3 model
- Appointment aggregate
- QueueEntry aggregate
- ServiceSession aggregate
- Enum types (status/category)

All three core models are JPA entities and hydrate their state handler objects after DB load.

### 5.4 pattern
Contains State and Strategy contracts + concrete implementations.

### 5.5 repository
- src/main/java/com/pes/smartqueue/repository/AppointmentRepository.java
- src/main/java/com/pes/smartqueue/repository/QueueEntryRepository.java
- src/main/java/com/pes/smartqueue/repository/ServiceSessionRepository.java

Responsibilities:
- Query methods for sorted lists and status filters
- Existence checks for correlation constraints

### 5.6 service
- src/main/java/com/pes/smartqueue/service/AppointmentService.java
- src/main/java/com/pes/smartqueue/service/QueueService.java
- src/main/java/com/pes/smartqueue/service/ServiceSessionService.java

Responsibilities:
- Transactional business logic
- Lifecycle transitions
- Cross-aggregate coordination

## 6. Interface Definitions and Their Purpose

### 6.1 AppointmentState
File: src/main/java/com/pes/smartqueue/pattern/state/appointment/AppointmentState.java

Methods:
- confirm(context)
- checkIn(context)
- cancel(context)
- expire(context)
- name()

Purpose:
Encapsulate legal appointment transitions by current appointment state.

### 6.2 QueueState
File: src/main/java/com/pes/smartqueue/pattern/state/queue/QueueState.java

Methods:
- startService(context)
- completeService(context)
- cancel(context)
- name()

Purpose:
Control queue entry progression (waiting, in-progress, terminal states).

### 6.3 ServiceSessionState
File: src/main/java/com/pes/smartqueue/pattern/state/session/ServiceSessionState.java

Methods:
- activate(context)
- pause(context)
- resume(context)
- complete(context)
- name()

Purpose:
Model service staff session lifecycle and enforce valid operations.

### 6.4 QueueOrderingStrategy
File: src/main/java/com/pes/smartqueue/pattern/strategy/QueueOrderingStrategy.java

Methods:
- orderQueue(queue)
- key()

Purpose:
Allow runtime-switchable queue ordering algorithms.

## 7. Lifecycle Modeling

### 7.1 Appointment Lifecycle
CREATED -> CONFIRMED -> CHECKED_IN
Alternative exits:
- CREATED/CONFIRMED -> CANCELLED
- CREATED/CONFIRMED -> EXPIRED

### 7.2 Queue Entry Lifecycle
WAITING -> IN_PROGRESS -> COMPLETED
Alternative exit:
- WAITING/IN_PROGRESS -> CANCELLED

### 7.3 Service Session Lifecycle
IDLE -> ACTIVE -> PAUSED -> ACTIVE (resume loop)
Completion path:
- ACTIVE/PAUSED -> COMPLETED

Constraint:
A staff member cannot own multiple ACTIVE sessions simultaneously.

## 8. Correlated Workflow (Detailed)

### 8.1 Customer Module
Route: /customer/appointments
- Create appointment.
- Confirm appointment.
- Check in appointment.
- Cancel appointment.
- Automatic expiry check is executed when listing.

### 8.2 Reception Module
Route: /reception/queue
- See all queue entries.
- See checked-in appointments not yet queued.
- Convert checked-in appointment to queue entry.
- Add walk-in entries.
- Start/complete/cancel queue entries.
- Switch ordering strategy.

### 8.3 Service Staff Module
Route: /staff/sessions
- Create and activate service session.
- Start next queue entry for a session.
- Complete currently assigned queue entry.
- Pause/resume/complete session.
- View queue snapshot and session assignment map.

## 9. Security Model
Security config file:
- src/main/java/com/pes/smartqueue/config/SecurityConfig.java

Route policy:
- /reception/**: RECEPTIONIST or ADMIN
- /customer/**: CUSTOMER or ADMIN
- /staff/**: SERVICE_STAFF or ADMIN

Current seeded credentials:
- reception / reception123
- customer / customer123
- staff / staff123
- admin / admin123

## 10. Persistence Model
JPA entities:
- appointments table
- queue_entries table
- service_sessions table

Schema generation is configured via Hibernate DDL auto mode (update).

Datasource configuration:
- src/main/resources/application.properties

## 11. Dockerized Deployment

### 11.1 One-Command Startup
From project root:

```bash
docker compose up --build
```

Compose file:
- docker-compose.yml

Build file:
- Dockerfile

What starts:
- MySQL 8.4 container
- Spring Boot app container

Default application URL:
- http://localhost:8080

## 12. Engineering Notes

### 12.1 Why Entity State Hydration Exists
State objects are runtime behavior objects and are not directly persisted as relational columns.
Persisted enum status is rehydrated into concrete state class on entity load. This keeps:
- Relational schema simple
- Behavior object model rich

### 12.2 Transaction Boundaries
Service classes are annotated transactional to guarantee consistency across multi-step operations, such as:
- Session starts next queue entry and stores assignment.
- Session completes assigned queue entry and clears assignment.

### 12.3 Current Trade-Offs
- Authentication users are still in-memory for speed of setup.
- Future enhancement: move users/roles to DB-backed auth model.

## 13. Suggested Next Engineering Improvements
1. Move security users/roles from in-memory to persistent user/role entities.
2. Add DTO layer + mapper layer to decouple view and domain models.
3. Add validation annotations on request payloads and entity fields.
4. Add Flyway migrations for controlled schema evolution.
5. Add unit/integration tests for state transitions and role-protected workflows.
6. Add audit logging for all lifecycle transitions.

## 14. Quick File Map
- Application entry: src/main/java/com/pes/smartqueue/SmartQueueApplication.java
- Security config: src/main/java/com/pes/smartqueue/config/SecurityConfig.java
- Controllers: src/main/java/com/pes/smartqueue/controller
- Services: src/main/java/com/pes/smartqueue/service
- Repositories: src/main/java/com/pes/smartqueue/repository
- Domain models: src/main/java/com/pes/smartqueue/model
- State pattern: src/main/java/com/pes/smartqueue/pattern/state
- Strategy pattern: src/main/java/com/pes/smartqueue/pattern/strategy
- Templates: src/main/resources/templates
- Runtime config: src/main/resources/application.properties
- Container files: Dockerfile, docker-compose.yml

---
This README is intentionally deep to support software engineers reviewing architecture decisions, design pattern mapping, and lifecycle/workflow implementation in an OOAD context.
