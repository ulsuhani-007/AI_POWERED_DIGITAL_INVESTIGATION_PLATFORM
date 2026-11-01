# CyberTrace Backend

## 1. Overview

The CyberTrace backend is developed using Java Spring Boot. It provides REST APIs to manage investigation cases, evidence and events. PostgreSQL is used as the database for persistent storage.

## 2. Technologies Used

- Java 21
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Maven

## 3. Project Structure

```text
backend/
├── src/main/java/backend/
│   ├── controller/
│   │   ├── CaseController.java
│   │   ├── EvidenceController.java
│   │   └── EventController.java
│   ├── model/
│   │   ├── Case.java
│   │   ├── Evidence.java
│   │   └── Event.java
│   ├── repository/
│   │   ├── CaseRepository.java
│   │   ├── EvidenceRepository.java
│   │   └── EventRepository.java
│   └── BackendApplication.java
├── src/main/resources/
│   └── application.properties
├── pom.xml
└── README.md
```

## 4. Backend Modules

### Case Management

- Create investigation cases.
- Retrieve all cases or a case by ID.
- Update case details.
- Delete cases.
- Validate case titles.

### Evidence Management

- Add evidence to an existing investigation case.
- Retrieve evidence records.
- Update and delete evidence.
- Retrieve evidence associated with a particular case.

### Event Management

- Create investigation events linked to cases.
- Retrieve and update events.
- Delete events.
- Retrieve events associated with a case in chronological order.

## 5. REST API Endpoints

Base URL: `http://localhost:8080`

| Method | Endpoint                      | Description               |
| ------ | ----------------------------- | ------------------------- |
| POST   | `/api/cases`                  | Create a case             |
| GET    | `/api/cases`                  | Retrieve all cases        |
| GET    | `/api/cases/{id}`             | Retrieve a case           |
| PUT    | `/api/cases/{id}`             | Update a case             |
| DELETE | `/api/cases/{id}`             | Delete a case             |
| POST   | `/api/evidence`               | Add evidence              |
| GET    | `/api/evidence`               | Retrieve all evidence     |
| GET    | `/api/evidence/{id}`          | Retrieve evidence         |
| PUT    | `/api/evidence/{id}`          | Update evidence           |
| DELETE | `/api/evidence/{id}`          | Delete evidence           |
| GET    | `/api/evidence/case/{caseId}` | Retrieve evidence by case |
| POST   | `/api/events`                 | Create an event           |
| GET    | `/api/events`                 | Retrieve all events       |
| GET    | `/api/events/{id}`            | Retrieve an event         |
| PUT    | `/api/events/{id}`            | Update an event           |
| DELETE | `/api/events/{id}`            | Delete an event           |
| GET    | `/api/events/case/{caseId}`   | Retrieve events by case   |

## 6. Database Configuration

The application uses PostgreSQL with Spring Data JPA.

Database name: `cybertrace`

Configure the database connection in `src/main/resources/application.properties`.

The database password is supplied through the `DB_PASSWORD` environment variable rather than being hardcoded.

## 7. Running the Backend

### Prerequisites

- Java 21
- PostgreSQL
- Maven (or the included Maven wrapper)

### Steps

1. Create a PostgreSQL database named `cybertrace`.
2. Configure the database credentials.
3. Navigate to the backend directory.
4. Set the database password environment variable.
5. Start the application.

```bash
cd backend
export DB_PASSWORD='your_database_password'
./mvnw spring-boot:run
```

The application starts on:

`http://localhost:8080`

## 8. Validation and Error Handling

The backend includes:

- Validation to prevent blank case, evidence and event titles.
- HTTP 400 responses for invalid input.
- HTTP 404 responses when requested records are not found.
- Validation of case references when creating evidence and events.

## 9. Build Verification

The backend was successfully compiled using:

```bash
./mvnw compile
```

**Build Status:** Successful
