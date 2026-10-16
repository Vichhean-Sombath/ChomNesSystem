# ChomNesSystem

ChomNesSystem is a Spring Boot API for school administration. It provides management endpoints for academic records, role-specific student access, attendance, grades, payments, certificates, lesson resources, dashboards, and reports. The API is being aligned with the local [Scholaris API parity contract](SCHOLARIS_PARITY_CONTRACT.md).

## Features

- JWT authentication with `Admin`, `Teacher`, and `Student` roles.
- Management for users, students, teachers, academic years, semesters, classes, subjects, enrollments, schedules, attendance, assessments, grades, fees, invoices, and payments.
- Student self-service for profile, classes, schedule, grades, attendance, invoices, certificates, and learning resources.
- Signed, expiring QR tokens for schedule-based student attendance check-in.
- Dashboard and report summaries.
- Optional Stripe Checkout and Bakong KHQR payment flows.
- Private certificate and lesson-resource uploads, served only through authenticated, access-checked routes.
- Optional payment notifications over Telegram and SMTP email.
- SQL Server schema management through Flyway migrations.

## Requirements

- JDK 21
- Maven 3.9 or later
- Microsoft SQL Server

## Run locally

1. Create an empty SQL Server database, for example `ChomNesSystem`. The application runs the Flyway migrations in `src/main/resources/db/migration` at startup; the database user must be allowed to create and update schema objects.
2. Set the required environment variables. In PowerShell:

   ```powershell
   $env:DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=ChomNesSystem;encrypt=true;trustServerCertificate=true"
   $env:DB_USERNAME = "your_sql_username"
   $env:DB_PASSWORD = "your_sql_password"

   $jwtBytes = [byte[]]::new(32)
   [System.Security.Cryptography.RandomNumberGenerator]::Fill($jwtBytes)
   $env:JWT_SECRET_BASE64 = [Convert]::ToBase64String($jwtBytes)

   $env:ADMIN_BOOTSTRAP_KEY = "replace-with-a-random-value-at-least-32-characters-long"
   ```

   Keep the JWT secret and bootstrap key private. Use a stable JWT secret between restarts, and store production secrets in your deployment's secret manager. `trustServerCertificate=true` is for local development with a self-signed SQL Server certificate; configure a trusted certificate in production.

3. Start the application:

   ```powershell
   mvn spring-boot:run
   ```

   The API listens on `http://localhost:8080` by default. Flyway applies the versioned migrations before Hibernate validates the entity mappings.

4. Create the first administrator. Bootstrap is available only when `ADMIN_BOOTSTRAP_KEY` is configured, and succeeds only while no Admin account exists:

   ```powershell
   $body = @{
     username = "admin"
     email = "admin@example.com"
     password = "replace-with-a-strong-password"
     setupKey = $env:ADMIN_BOOTSTRAP_KEY
   } | ConvertTo-Json

   Invoke-RestMethod -Method Post `
     -Uri "http://localhost:8080/api/auth/bootstrap-admin" `
     -ContentType "application/json" `
     -Body $body
   ```

5. Log in to receive an access token:

   ```powershell
   $login = Invoke-RestMethod -Method Post `
     -Uri "http://localhost:8080/api/auth/login" `
     -ContentType "application/json" `
     -Body '{"email":"admin@example.com","password":"replace-with-a-strong-password"}'

   $token = $login.accessToken
   ```

   Send the token on protected routes with `Authorization: Bearer <token>`. Access tokens expire after 15 minutes by default.

## API overview

The API exposes REST-style routes under `/api` and compatibility routes matching the Scholaris API where implemented. For example, users are available through `/api/users` and `/user`, students through `/api/students` and `/student`, and academic years through `/api/academic-years` and `/academic-year`.

| Area | Main routes |
|---|---|
| Authentication | `/api/auth/*`, `/user/*` |
| School management | `/api/users`, `/api/students`, `/api/teachers`, `/api/academic-years`, `/api/semesters`, `/api/classes`, `/api/subjects`, `/api/class-enrollments`, `/api/time-slots`, `/api/schedules` |
| Attendance and academics | `/api/attendance-records`, `/api/grading-criteria`, `/api/assessments`, `/api/grades`, `/api/final-grades` |
| Fees and payments | `/api/fee-structures`, `/api/invoices`, `/api/payments`, `/payment/stripe/*`, `/payment/bakong/*` |
| Student portal | `/student/me/summary`, `/classes`, `/schedule`, `/grades`, `/attendance`, `/invoices`, `/certificates`, `/resources`, `/profile` |
| Dashboard and reports | `/dashboard/summary`, `/reports/summary` |
| Certificates and learning resources | `/api/certificates`, `/api/lesson-resources`, `/files/{fileName}` |

Protected API calls require a valid JWT. Admin, Teacher, and Student access is restricted by route and, for student self-service, scoped to the authenticated student's linked profile. Public routes include `POST /api/auth/login`, `POST /api/auth/bootstrap-admin`, `POST /user/login`, and `POST /user/register`; payment provider webhooks are public endpoints that validate provider callbacks. Registration is exposed on the Scholaris-compatible `/user/register` route.

For request and response details, see the DTOs under `src/main/java/com/chomnes/system/dto` and the controller classes under `src/main/java/com/chomnes/system/controller`. Route aliases do not necessarily share identical response shapes; consult the parity contract before relying on field-level compatibility.

## Configuration

The application reads configuration from environment variables. Required settings are `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET_BASE64`.

| Variable | Purpose | Default |
|---|---|---|
| `DB_URL` | JDBC URL for SQL Server | Local `ChomNesSystem` database URL in `application.properties` |
| `DB_USERNAME`, `DB_PASSWORD` | SQL Server credentials | Required |
| `JWT_SECRET_BASE64` | Base64-encoded HMAC key; must decode to at least 32 bytes | Required |
| `JWT_EXPIRATION_MS` | JWT lifetime in milliseconds | `900000` |
| `ADMIN_BOOTSTRAP_KEY` | One-time key used to create the first Admin; must be at least 32 characters | Empty (bootstrap disabled) |
| `ATTENDANCE_QR_TTL_SECONDS` | Signed attendance QR token lifetime, from 1 to 900 seconds | `120` |
| `FILE_STORAGE_DIR` | Private upload storage directory | `./uploads` |
| `STRIPE_SECRET_KEY`, `STRIPE_PUBLIC_KEY`, `STRIPE_WEBHOOK_SECRET`, `SERVER_URL` | Stripe Checkout and webhook configuration | Empty (Stripe unavailable until configured) |
| `BAKONG_ACCOUNT_ID`, `MERCHANT_NAME`, `MERCHANT_CITY`, `MERCHANT_PHONE` | Bakong merchant identity for KHQR generation | Empty |
| `BAKONG_API_URL`, `BAKONG_API_TOKEN`, `BAKONG_API_ENABLED` | Bakong Open API configuration | NBC API URL; empty token; disabled |
| `BAKONG_KHR_PER_USD` | USD-to-KHR conversion rate for QR payments | `4000` |
| `TELEGRAM_BOT_TOKEN`, `TELEGRAM_BOT_ID` | Optional Telegram payment notices | Empty (disabled) |
| `EMAIL_SMTP_HOST`, `EMAIL_SMTP_PORT`, `EMAIL_SENDER_ADDRESS`, `EMAIL_APP_PASSWORD` | Optional SMTP payment receipts | Port `587`; other values empty |
| `SYSTEM_USER_ID` | Optional existing user to associate with provider-recorded payments | Empty (an inaccessible automation account is created if needed) |

Payment and notification providers are optional. Configure all required credentials for a provider before enabling it. Uploaded files are private; deployments should use durable storage shared by all API instances and back it up separately from the database.

## Build and checks

```powershell
mvn clean package
mvn test
```

The test source directories are currently empty. A successful compile does not verify SQL Server connectivity, payment-provider callbacks, email delivery, or role-based runtime behavior.

## Project layout

```text
src/main/java/com/chomnes/system/
  config/       Security and application configuration
  controller/   HTTP API endpoints
  dto/          Request and response records
  exception/    API exceptions and error handling
  model/        JPA entities
  repository/   Spring Data repositories
  service/      Business logic and provider clients
src/main/resources/
  application.properties
  db/migration/ Flyway SQL Server migrations
```

## Compatibility and release status

The compatibility contract and release checklist are maintained in [SCHOLARIS_PARITY_CONTRACT.md](SCHOLARIS_PARITY_CONTRACT.md). That document records known response-level differences, including some related records that are returned as IDs rather than populated objects, and release checks that still need to run with Maven Central, SQL Server, and provider credentials available. The API should not be considered fully release-verified until those checks and parity gaps are addressed.
