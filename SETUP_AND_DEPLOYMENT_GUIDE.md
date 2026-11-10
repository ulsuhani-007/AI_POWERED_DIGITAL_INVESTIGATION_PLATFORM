# AI-Powered Digital Investigation Platform - Setup & Deployment Guide

This guide describes how any team member can clone, configure, and launch the **AI-Powered Digital Investigation Platform** on a fresh Windows machine with zero manual code modifications.

---

## 1. Quick Start for Fresh Machines (8 Simple Steps)

A new team member can get the entire platform up and running in just a few minutes:

1. **Install Java 21 or Later**:
   - Download and install JDK 21+ (e.g., [Eclipse Adoptium Temurin 21+](https://adoptium.net/) or Oracle JDK).
   - Verify in PowerShell:
     ```powershell
     java -version
     ```
2. **Database Configuration (Zero-Config or PostgreSQL 16+)**:
   - *Zero-Config (Out of the Box)*: If PostgreSQL is not installed, the platform automatically launches with an embedded persistent database engine (`./data/cybertrace_db`) with full persistence across restarts and zero manual configuration required.
   - *Production / Native PostgreSQL 16+ (Optional)*:
     - Install PostgreSQL 16+ and ensure the Windows Service is running (`Start-Service postgresql*`).
     - Create database: `createdb -U postgres cybertrace_db` (defaults: `postgres`/`postgres` on `5432`).
     - Custom credentials can be set via `$env:DB_USERNAME` and `$env:DB_PASSWORD`.
4. **Clone the Repository**:
   ```powershell
   git clone <repository-url>
   ```
5. **Open Project Root**:
   ```powershell
   cd AI_POWERED_DIGITAL_INVESTIGATION_PLATFORM
   ```
6. **Run the Portable Startup Script**:
   - Via Command Prompt or File Explorer:
     ```cmd
     start-platform.bat
     ```
   - Or via PowerShell:
     ```powershell
     .\start-platform.ps1
     ```
7. **Log In Using the Universal Demo / Admin Credentials**:
   - **Username**: `admin`
   - **Password**: `AdminPass123!`
8. **Use the JavaFX Investigation Workstation**:
   - The desktop workstation automatically launches and connects to the backend services.

---

## 2. Platform Architecture & Service Allocation

The platform is 100% Java & JavaFX based. It requires **no** Node.js, npm, Vite, Python, Electron, or external web runtimes:

```
JavaFX Desktop Forensic Workstation (Java 21 + JavaFX 21)
       ↓
M1 Backend Service (Spring Boot 3.3.4 :8080)
       ↓                     ↓
M2 Timeline Engine (:8081)   M3 AI Analysis Engine (:8082)
       ↓
PostgreSQL Database (:5432 / cybertrace_db)
```

| Component | Port | Technology | Purpose |
|---|---|---|---|
| **PostgreSQL** | `5432` | PostgreSQL 16+ | Forensic case persistence, evidence custody, audit logs & Flyway migrations |
| **M1 Backend** | `8080` | Spring Boot 3.3.4 | Core REST API, JWT auth, RBAC, evidence storage, historical cases |
| **M2 Timeline Engine** | `8081` | Spring Boot 3.3.4 | CSV evidence parser, timestamp normalizer, multi-source timeline builder |
| **M3 Analysis Engine** | `8082` | Spring Boot 3.3.4 | Statistical anomaly detection, brute force detection, AI forensic summary |
| **JavaFX Frontend** | Desktop | JavaFX 21 | High-performance forensic analyst workstation UI |

---

## 3. Universal Demo Accounts

On startup, Flyway and the database seeders initialize the standard demonstration credentials:

| Role | Username | Password | Access Capabilities |
|---|---|---|---|
| **Administrator** | `admin` | `AdminPass123!` | Full access to Cases, Evidence, Timeline, AI Analysis, and Administration (User Management, Audit Logs) |
| **Investigator** | `investigator` | `InvestigatorPass123!` | Full access to Cases, Evidence Upload, Normalized Timeline, Anomaly Inspection, and Case Reports |

---

## 4. Startup & Teardown Utilities

### Portable Startup (`start-platform.bat` / `start-platform.ps1`)
The startup script detects all dependencies dynamically without any hard-coded paths:
1. `[1/5] Checking Java...`: Dynamically locates `java.exe` via `JAVA_HOME` or `PATH` and verifies version >= 21.
2. `[2/5] Checking PostgreSQL...`: Checks localhost:5432; auto-detects Windows Service or standard installation directory if not listening.
3. `[3/5] Starting M1...`: Reuses existing healthy M1 instance on port 8080 if already active; terminates stale processes safely; starts M1.
4. `[4/5] Starting M2 and M3...`: Reuses existing instances on ports 8081 and 8082 if active; starts missing services.
5. `[5/5] Starting JavaFX...`: Launches the JavaFX Investigation Workstation via the portable Maven Wrapper.

### Portable Teardown (`stop-platform.bat` / `stop-platform.ps1`)
To cleanly terminate background microservices and free ports 8080, 8081, and 8082:
```cmd
stop-platform.bat
```
or in PowerShell:
```powershell
.\stop-platform.ps1
```

---

## 5. Environment Variables & Overrides (Optional)

All settings work out-of-the-box with default values. If needed, the following environment variables can be set:

| Variable | Default Value | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/cybertrace_db` | PostgreSQL JDBC connection URL |
| `DB_USERNAME` | `postgres` | PostgreSQL username |
| `DB_PASSWORD` | `postgres` | PostgreSQL password |
| `CYBERTRACE_BACKEND_URL` | `http://localhost:8080` | URL used by JavaFX frontend to communicate with M1 |
| `CYBERTRACE_M2_URL` | `http://localhost:8081` | URL used by M1 to communicate with M2 Timeline Engine |
| `CYBERTRACE_M3_URL` | `http://localhost:8082` | URL used by M1 to communicate with M3 Analysis Engine |

---

## 6. End-to-End Investigation Workflow

1. **Login**: Authenticate with `admin` / `AdminPass123!`.
2. **Cases**: View historical landmark demonstration cases or click **+ New Case**.
3. **Upload Evidence**: Upload forensic CSV files (e.g. login activity, network traffic, file access logs).
4. **Processing & Timeline**: Evidence is parsed by M2 and merged into a chronological multi-source timeline.
5. **AI Anomaly Detection**: M3 evaluates events using statistical z-scores, brute force detection, and off-hours heuristics.
6. **Forensic Summary**: Review evidence-grounded AI analysis findings and investigation statistics.
7. **Administration**: Provision new analysts, manage roles, and review tamper-evident audit logs.
