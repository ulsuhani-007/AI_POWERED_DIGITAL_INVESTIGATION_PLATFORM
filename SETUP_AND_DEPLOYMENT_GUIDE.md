# AI-Powered Digital Investigation Platform - Setup & Deployment Guide

This guide describes how any team member can clone, configure, and launch the **AI-Powered Digital Investigation Platform** on a fresh Windows machine.

---

## 1. System Prerequisites

The platform is 100% Java-based and requires only standard, vendor-neutral prerequisites:

1. **Java Development Kit (JDK) 21 or later**
   - Verified runtime: Java 21 / Java 25 (Eclipse Adoptium Temurin or Oracle JDK).
   - Verify in terminal:
     ```powershell
     java -version
     ```
2. **PostgreSQL 16 Database Server**
   - Running locally on port `5432`.
   - Default superuser: `postgres`.
   - Database name: `cybertrace_db`.
   - Setup command:
     ```powershell
     createdb -U postgres cybertrace_db
     ```
   - *Note*: If your PostgreSQL uses custom credentials, you can configure them via environment variables:
     - `DB_URL` (default: `jdbc:postgresql://localhost:5432/cybertrace_db`)
     - `DB_USERNAME` (default: `postgres`)
     - `DB_PASSWORD` (default: `postgres`)

No Node.js, npm, Python, Electron, or external web runtimes are required.

---

## 2. Platform Architecture & Port Allocation

The platform comprises three microservices, a relational database, and a JavaFX desktop workstation:

| Component | Role | Default Port | Technology |
|---|---|---|---|
| **PostgreSQL** | Forensic data persistence & Flyway schema | `5432` | PostgreSQL 16 |
| **M1 Backend** | Case management, custody tracking & security | `8080` | Spring Boot 3.3.4 (Java 21) |
| **M2 Timeline Engine** | Evidence parsing, event normalization & merging | `8081` | Spring Boot 3.3.4 (Java 21) |
| **M3 Analysis Engine** | AI anomaly detection, statistical baselines & summaries | `8082` | Spring Boot 3.3.4 (Java 21) |
| **JavaFX Frontend** | Desktop forensic workstation UI | N/A | JavaFX 21 + Jackson API |

---

## 3. Universal Demo Accounts

On any fresh or existing machine, the application automatically and idempotently initializes standard demo credentials with BCrypt password hashes in PostgreSQL:

### Universal Administrator
- **Username**: `admin`
- **Password**: `AdminPass123!`
- **Role**: `ROLE_ADMIN`
- **Capabilities**: Full access to Case Management, Evidence Processing, AI Analysis, and the Administration Panel (User Management, Audit Logs, Security Policies).

### Universal Investigator
- **Username**: `investigator`
- **Password**: `InvestigatorPass123!`
- **Role**: `ROLE_INVESTIGATOR`
- **Capabilities**: Forensic case intake, evidence upload, timeline inspection, anomaly review, and forensic summary generation.

---

## 4. One-Click Portable Startup

The repository provides two portable startup scripts that do **not** depend on any user-specific directories or usernames:

### Method A: Windows Command Prompt / File Explorer
Double-click or run from cmd:
```cmd
start-platform.bat
```

### Method B: PowerShell
From the project root:
```powershell
.\start-platform.ps1
```

The script automatically:
1. Verifies the Java runtime (supports Java 21, 22, 23, 24, 25, and later).
2. Checks PostgreSQL on port `5432` and automatically starts the database if installed locally (`%USERPROFILE%\pgsql` or standard Windows service).
3. Diagnoses and safely terminates stale project processes on ports `8080`, `8081`, and `8082`.
4. Starts M1 (port `8080`), M2 (port `8081`), and M3 (port `8082`) in background with logs in `target\logs\`.
5. Waits for health checks to verify all backend microservices are online.
6. Launches the JavaFX Investigation Workstation.

---

## 5. Manual Service Startup (Optional)

If you prefer to start each service in separate terminal windows:

### Terminal 1: M3 Analysis Engine (Port 8082)
```powershell
.\cybertrace-m3\mvnw.cmd -f cybertrace-m3\pom.xml spring-boot:run
```

### Terminal 2: M2 Timeline Engine (Port 8081)
```powershell
.\cybertrace-m2\mvnw.cmd -f cybertrace-m2\pom.xml spring-boot:run
```

### Terminal 3: M1 Backend Service (Port 8080)
```powershell
.\cybertrace-m1\mvnw.cmd -f cybertrace-m1\pom.xml spring-boot:run
```

### Terminal 4: JavaFX Desktop Workstation
```powershell
.\cybertrace-m1\mvnw.cmd -f frontend-javafx\pom.xml javafx:run
```

---

## 6. End-to-End Investigation Workflow

1. **Investigator Login**:
   - Open the application and log in using `admin / AdminPass123!`.
2. **Case Intake**:
   - Navigate to **Cases** and click **+ New Case**.
   - Enter case number, title, priority, and lead investigator.
3. **Evidence Ingestion**:
   - Select the case, click **Upload Evidence**, and choose a forensic CSV file (e.g. login, network, file access, or browser activity).
   - The platform calculates the SHA-256 integrity hash for chain-of-custody tracking.
4. **M2 Normalization & Timeline Construction**:
   - The M2 timeline engine normalizes timestamps and attributes into a unified schema (`normalized_events`).
5. **M3 AI Anomaly Detection & Forensic Summary**:
   - The M3 engine applies statistical anomaly detection, brute-force correlation, and off-hours heuristics.
   - An evidence-grounded forensic report is produced with key findings and investigation summaries.
6. **User Administration**:
   - Admins can navigate to **Administration**, view existing users, and click **+ New User** to provision new accounts with custom security roles.

