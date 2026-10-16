# Scholaris API Feature Parity Contract

**Stage:** 1 — Define the parity contract  
**Status:** Stages 1–7 implemented for the tracked management, portal, attendance QR, dashboard, reports, payment-provider, private file delivery, and optional payment-notification scope. Stage 8 source compilation now passes, but the project is **not release-verified**: related-record response differences remain and Maven could not run the test phase because its Surefire dependencies are unavailable offline.
**Reference:** `../ScholarisAPI` (local Scholaris API source and README).  
**Target:** `ChomNesSystem` Spring Boot API.

## 1. Parity goal

The Spring API is feature-equivalent when it supports the workflows, access rules, and externally observable results of the reference Scholaris API for administrators, teachers, and students. Existing Spring endpoints may remain available, but a Scholaris client should not lose a supported workflow when pointed at Spring.

The target is **API compatibility as well as feature parity**: expose the reference route paths and methods as compatibility routes where the Spring `/api/...` routes differ, and preserve the reference request/response contract for those routes. Do not remove or silently change existing Spring routes. The SQL Server/Flyway implementation is an accepted infrastructure difference; moving back to MongoDB is out of scope.

## 2. Shared API behavior

- Roles are `Admin`, `Teacher`, and `Student`. Protected routes require a valid Bearer access token and enforce the route's role rule below.
- The authenticated identity, including linked student/teacher identity, comes from the validated token and database records. A client-supplied identity must not be trusted for a self-service operation.
- Compatibility routes follow the reference's operation-specific response shape. Standard data operations use `{ "message": ..., "data": ... }`; validation failures use `{ "message": ..., "errors": [...] }`. Login/registration return the reference token and user fields. Do not return password hashes.
- Preserve reference-visible field names and types on compatibility routes, including populated related records when the reference returns them. Existing Spring DTO contracts can remain on `/api/...` routes.
- Match the reference's meaningful HTTP outcomes: validation `400`, unauthenticated `401`, forbidden `403`, missing record `404`, and conflict `409`; successful create/check-in normally returns `201`.
- Deactivation is a reversible account state (disable/enable), not hard deletion. Related domain records must retain their current reference behavior when records are deactivated or deleted.

## 3. Feature and route inventory

“Spring now” records the inspected implementation, not the desired endpoint naming. Reference route names below are the compatibility contract.

| Feature | Scholaris reference routes and access | Spring status at baseline |
|---|---|---|
| Authentication | `POST /user/login` (public); `POST /user/register` (public); `POST /user/logout` (authenticated) | Partial: `/api/auth/login`, one-time `/api/auth/bootstrap-admin`, and `/api/auth/password`; no compatible register/logout routes. |
| User administration | `POST /user/create` (Admin); `PUT /user/update/:id` (Admin/Teacher/Student); `PUT /user/change-password` (authenticated); `DELETE /user/disable/:id` and `POST /user/enable/:id` (Admin) | Partial: `/api/users` list/detail/create/update/deactivate exists; password is under auth route; enable and reference-compatible paths are absent. |
| Students | `GET /student/data[/:id]` (Admin/Teacher); `PUT /student/update/:id` (Admin) | Core records exist at `/api/students`; Spring additionally creates/deletes records. Reference path compatibility and permission parity need mapping. |
| Teachers | `GET /teacher/data[/:id]`, `PUT /teacher/update/:id` (Admin/Teacher) | Core records exist at `/api/teachers`; Spring additionally creates/deletes records. Reference path compatibility and permission parity need mapping. |
| Academic years | `GET /academic-year/data[/:id]` (Admin/Teacher); `POST /academic-year/create`, `PUT /academic-year/update/:id`, `DELETE /academic-year/delete/:id` (Admin) | Read/create/update exist at `/api/academic-years`; delete is absent. |
| Semesters | `GET /semester/data[/:id]` (Admin/Teacher); create/update/delete (Admin) | Read/create/update exist at `/api/semesters`; delete is absent. |
| Classes | `GET /class/data[/:id]` (Admin/Teacher); create/update/delete (Admin) | Read/create/update exist at `/api/classes`; delete is absent. |
| Subjects | `GET /subject/data[/:id]` (Admin/Teacher); create/update/delete (Admin) | Read/create/update exist at `/api/subjects`; delete is absent. |
| Class enrollments | `GET /class-enrollment/data[/:id]` (Admin/Teacher); create/update/delete (Admin) | Read/create/update exist at `/api/class-enrollments`; delete is absent. |
| Time slots | `GET /time-slot/data[/:id]` (Admin/Teacher); create/update/delete (Admin) | Read/create/update exist at `/api/time-slots`; delete is absent. |
| Schedules | `GET /schedule/data[/:id]` (Admin/Teacher); create/update/delete (Admin/Teacher) | Read/create/update exist at `/api/schedules`; delete is absent. |
| Attendance | `GET /attendance-record/data[/:id]`, create/update/delete (Admin/Teacher) | Read/create/update exist at `/api/attendance-records`; delete and student QR check-in are absent. |
| Grading criteria | `GET /grading-criteria/data[/:id]` (Admin/Teacher); create/update/delete (Admin) | Read/create/update exist at `/api/grading-criteria`; delete is absent. |
| Assessments | `GET /assessment/data[/:id]`, create/update/delete (Admin/Teacher) | Read/create/update exist at `/api/assessments`; delete is absent. |
| Grades | `GET /grade/data[/:id]`, create/update/delete (Admin/Teacher) | Read/create/update exist at `/api/grades`; delete is absent. |
| Final grades | `GET /final-grade/data[/:id]`, create/update/delete (Admin) | Read and compute exist at `/api/final-grades`; reference-compatible CRUD and update/delete are absent. |
| Fee structures | `GET /fee_structure/data[/:id]`, create/update/delete (Admin) | Read/create/update exist at `/api/fee-structures`; delete is absent. |
| Invoices | `GET /invoice/data[/:id]`, create/update/delete (Admin) | Read/create exist at `/api/invoices`; update/delete are absent. Student invoice self-service is also absent. |
| Payments | Admin CRUD at `/payment/data[/:id]`, `/payment/create`, `/payment/update/:id`, `/payment/delete/:id`; provider routes below | Implemented with provider-confirmed, idempotent payment recording; ordinary payment CRUD remains available. |
| Stripe | `GET /payment/stripe/config` (public); `POST /payment/stripe/checkout` and `/verify` (Admin/Student); `GET /payment/stripe/success` and `/cancel` (provider redirect); `POST /payment/stripe/webhook` (signature-verified provider callback); `POST /payment/stripe/record` (Admin/Student) | Implemented with Checkout Sessions, raw-body signature verification, session ownership/amount validation, webhook processing, and duplicate-event safety. The record route requires a Stripe `session_id` and verifies it before recording. |
| Bakong | `POST /payment/bakong/qr` and `/verify` (Admin/Student); `GET /payment/bakong/account` (Admin); `POST /payment/bakong/webhook` (provider callback) | Implemented with NBC KHQR generation, authenticated Open API transaction/account checks, amount and merchant validation, expiry, and idempotent recording. |
| Certificates | `GET /certificate/data[/:id]`, `POST /certificate/create`, `DELETE /certificate/delete/:id` (Admin) | Metadata routes and student self-service are implemented; Stage 7 adds private certificate upload and access-controlled download. |
| Lesson resources | `GET /lesson_resource/data[/:id]`, create/update/delete (Admin/Teacher) | CRUD and student portal listing are implemented; Stage 7 adds teacher/admin uploads and schedule-enrollment-scoped downloads. |
| Emergency contacts | `GET /student-emergency-contact/data[/:id]`, create/update/delete (Admin) | CRUD exists at `/api/student-emergency-contacts`; student self-service permissions and reference paths differ. |
| Admin dashboard | `GET /dashboard/summary` (Admin) | Implemented in Stage 3 with revenue ranges, monthly comparison, payment methods, invoice status, student enrollment trend, and recent payments. |
| Admin reports | `GET /reports/summary` (Admin) | Implemented in Stage 5 with financial, enrollment, academic, and teacher workload summaries plus range and academic filters. |
| Student portal | `GET /student/me/{summary,classes,schedule,grades,attendance,invoices,certificates,resources,profile}` and `POST /student/me/attendance/checkin` (Student) | Implemented in Stage 3; reads are scoped to the signed-in student's profile and active enrollments. Stage 4 upgrades check-in to schedule-bound signed QR tokens. |
| Notifications | Optional email and Telegram notifications, especially payment events | Stage 7 sends optional Telegram payment notices and email receipts to the first student emergency contact with an email address. |

The reference CRUD route pattern is explicit per table above: where it says “create/update/delete,” those are respectively `POST /<resource>/create`, `PUT /<resource>/update/:id`, and `DELETE /<resource>/delete/:id` for the resource's displayed route stem.

## 4. Stage 2 implementation update — management API

The Spring project now exposes the Scholaris management route stems and CRUD suffixes for users, students, teachers, academic years, semesters, classes, subjects, enrollments, time slots, schedules, attendance, grading criteria, assessments, grades, final grades, fee structures, invoices, payments, certificates, lesson resources, and emergency contacts. The original `/api/...` routes remain available. Compatibility requests translate snake_case JSON keys to the Spring DTO names; compatibility responses use snake_case fields and the reference `{message, data}` envelope, with validation errors returned as `{message, errors}`. Route security rules now cover the reference role matrix for these management endpoints.

User registration creates its matching student or teacher profile; Scholaris login includes linked profile IDs; logout, password change, and administrator enable/disable routes are present. Repeated enable/disable requests return a client error, matching the reference lifecycle behavior.

This stage closes management route coverage and adds invoice/payment/final-grade update and delete operations. It does **not** claim full management response parity yet: several Spring DTOs return related record IDs where Scholaris populates related objects, and SQL foreign-key restrictions can prevent deletes that MongoDB may allow. At this stage provider payment flows and reports remained future work; reports are covered by Stage 5 and payment-provider flows by Stage 6.

## 5. Stage 3 implementation update — role-specific portals

`GET /dashboard/summary` is now available to Admin accounts with the reference `range` options (`weekly`, `monthly`, `yearly`) and summary fields. The student portal routes now cover summary, active classes, schedule, published grades and final grades, attendance history, invoices with payments and balances, certificates, class resources, and profile with emergency contacts. Portal responses preserve the reference's camelCase payload fields inside the standard response envelope.

Student portal data is derived from the authenticated user-to-student association; no student ID is accepted from the caller. Attendance check-in verifies that the student and enrollment are active, the schedule exists, the date is today, and a duplicate has not already been recorded. Stage 4 replaces the shared check-in token with a short-lived signed token bound to a specific schedule and attendance date.

Stage 3 implementation evidence is in `StudentPortalController.java`, `StudentPortalService.java`, `DashboardController.java`, `DashboardService.java`, `SecurityConfiguration.java`, and `ScholarisCompatibilityAdvice.java`.

## 6. Stage 4 implementation update — attendance QR check-in

Admin users and the teacher assigned to a schedule can issue a signed QR token through `POST /attendance-record/qr/{scheduleId}` (also available at `POST /api/attendance-records/{scheduleId}/qr`). The response includes the compact token, schedule ID, attendance date, issue/expiry timestamps, and lifetime. The token is signed with the configured application JWT key and expires after `ATTENDANCE_QR_TTL_SECONDS` (default 120 seconds, maximum 900).

`POST /student/me/attendance/checkin` now validates the signature, issuer, purpose, expiration, and exact schedule/date binding from the token before checking the student's active account, enrollment, current date, and duplicate attendance. It derives student identity from the access token and records `Present` attendance with the schedule's teacher as marker. This makes a QR token unusable for another schedule/date and prevents reuse after expiry. The old `ATTENDANCE_CHECKIN_TOKEN` shared secret is no longer required.

Implementation evidence: `AttendanceQrController.java`, `AttendanceQrService.java`, `StudentPortalService.java`, `StudentAttendanceCheckInRequest.java`, `SecurityConfiguration.java`, and `application.properties`.

## 7. Stage 5 implementation update — dashboard and reports

`GET /dashboard/summary` remains available to Admin users with the existing revenue summary. `GET /reports/summary` now returns the Scholaris response envelope and four report sections: financial, enrollment, academic, and teacher workload. It accepts `range` (`weekly`, `monthly`, or `yearly`, defaulting to monthly for other values), `academicYearId`, and `semesterId`; the academic filters are applied to matching report sections. Financial results include revenue trends, payment methods, collection metrics, and up to 50 outstanding invoice rows. Enrollment results include student trends/distributions and class capacity. Academic results include attendance and final-grade summaries. Teacher workload results include schedule density and per-teacher averages.

The report is aggregated from the Spring/JPA datasets in memory. `ReportsController.java`, `ReportsService.java`, `SecurityConfiguration.java`, and `ScholarisCompatibilityAdvice.java` implement the route, Admin access, aggregation, and camelCase compatibility response.

## 8. Stage 6 implementation update — payment integrations

Stripe routes now create and retrieve Checkout Sessions through the Stripe API. Checkout prices use the invoice's current remaining USD balance. `verify` and the compatibility `record` route require a real Stripe session with paid status, USD currency, matching invoice/student metadata, and a valid remaining balance. The success route reports provider status but does not itself mark an invoice paid. The webhook endpoint receives the raw body, verifies `Stripe-Signature` with the configured webhook secret and timestamp tolerance, and processes completed/async-succeeded Checkout events.

Bakong QR payments now use the National Bank of Cambodia KHQR Java SDK to generate an amount-bound KHR dynamic QR with a 30-minute expiry. The generated MD5 and amount are stored with the invoice. Verification polls Bakong Open API using the QR MD5, checks provider success, merchant destination, currency, amount, and expiry, then records the payment. Bakong callbacks are treated as notification hints and are confirmed against Bakong Open API before any invoice changes. Repeated provider calls reuse the existing payment; invoice locking and a filtered unique transaction-reference index protect against concurrent duplicate recording.

Configure these environment variables where the corresponding provider is enabled: `STRIPE_SECRET_KEY`, `STRIPE_PUBLIC_KEY`, `STRIPE_WEBHOOK_SECRET`, `SERVER_URL`; and `BAKONG_ACCOUNT_ID`, `MERCHANT_NAME`, `MERCHANT_CITY`, `MERCHANT_PHONE`, optional `STORE_LABEL`, `BAKONG_API_URL`, `BAKONG_API_TOKEN`, `BAKONG_API_ENABLED`, and optional `BAKONG_KHR_PER_USD` (default 4000). `SYSTEM_USER_ID` may point to an existing system account; if omitted, the service creates a locked-down automation account with a random inaccessible password to associate provider payments.

Implementation evidence: `PaymentIntegrationController.java`, `PaymentIntegrationService.java`, `StripeProviderClient.java`, `BakongProviderClient.java`, `PaymentService.java`, `BakongQrRequestService.java`, `SecurityConfiguration.java`, `ScholarisCompatibilityAdvice.java`, and `V2__enforce_payment_transaction_idempotency.sql`.

## 9. Stage 7 implementation update — files, certificates, and notifications

Administrators can attach a file to an issued certificate with `POST /api/certificates/{id}/file` (or `/certificate/{id}/file`); administrators and the owning teacher can attach a file to a lesson resource with `POST /api/lesson-resources/{id}/file` (or `/lesson_resource/{id}/file`). Both accept a multipart `file` field, store a private copy under `FILE_STORAGE_DIR` (default `./uploads`), and return the stable `/files/{uuid}.{extension}` URL saved in the existing record. Uploads are limited to 25 MB and common PDF, image, Office document, text, and MP4 types.

`GET /files/{uuid}.{extension}` requires authentication. The API serves only files referenced by a certificate or lesson resource and checks the same access rules as that record: certificate owner or Admin, and lesson-resource Admin, owning Teacher, or Student with an active enrollment in the scheduled class. Files are returned as attachments with content-type sniffing disabled.

When a payment is recorded manually or confirmed by a payment provider, optional notifications run after the payment transaction commits. Telegram sends a payment summary to the configured chat; SMTP sends a receipt to the student's first emergency contact with an email address. Notification delivery failures do not undo successful payments. Configure `TELEGRAM_BOT_TOKEN`, `TELEGRAM_BOT_ID`, `EMAIL_SMTP_HOST`, `EMAIL_SMTP_PORT` (default 587), `EMAIL_SENDER_ADDRESS`, and `EMAIL_APP_PASSWORD` to enable delivery. Upload storage can be relocated with `FILE_STORAGE_DIR`; deployments should point it at durable storage shared by API instances.

Implementation evidence: `FileStorageService.java`, `StoredFileController.java`, `CertificateController.java`, `LessonResourceController.java`, `CertificateService.java`, `LessonResourceService.java`, `PaymentNotificationService.java`, `PaymentService.java`, `SecurityConfiguration.java`, and `application.properties`.

## 10. Stage 8 compatibility and release check

The release audit reviewed Scholaris populate behavior, Spring compatibility response mapping, endpoint role rules, file-delivery authorization, provider configuration, and available build verification. It confirmed that the Scholaris compatibility advice converts JSON naming and wraps responses, but some management responses still expose foreign-key IDs where Scholaris returns populated records. The gaps include relationships in academic years, semesters, classes, subjects, enrollments, time slots, schedules, attendance, grading criteria, assessments, grades, final grades, fee structures, invoices, payments, certificates, and emergency contacts. These differences must be reconciled before claiming complete API-response parity.

`mvn -o -B clean compile` succeeded using the cached dependencies copied into a temporary project-local repository; all 228 Java source files compiled. The `mvn test` attempt compiled production sources but could not resolve Surefire 3.5.6 dependencies in offline mode, and Maven Central access is denied here. The test directories contain no test sources. Database and provider runtime behavior therefore remains unverified. The test-phase failure is an environment limitation, not a source compilation failure.

Before release, run `mvn test` in an environment with Maven Central access and a writable local repository; address the populated-relation contract differences; configure production database/JWT secrets and provider credentials for enabled payment methods; configure `FILE_STORAGE_DIR` as durable storage shared by all API instances; and perform authenticated role-based smoke checks for the Scholaris compatibility routes, file access, Stripe callbacks, and Bakong verification. Email and Telegram remain optional and need credentials only when enabled.

Release audit evidence: successful clean compilation, the blocked Maven test phase, `ScholarisCompatibilityAdvice.java`, `SecurityConfiguration.java`, and `SCHOLARIS_PARITY_CONTRACT.md`.

## 11. Business behavior that must be preserved

1. **Authentication and account lifecycle:** login verifies credentials and returns user role plus linked `student_id`/`teacher_id`; registration only creates `Teacher` or `Student` accounts and creates the matching profile; logout and password change behave as in the reference; disabled accounts cannot continue authenticating; admins can re-enable them.
2. **Authorization and ownership:** student portal and student payment operations use the authenticated student's own profile. A student cannot read or mutate another student's information by changing an ID in a request. Teacher/admin permissions remain route-specific as in the matrix.
3. **Attendance check-in:** accept only a valid configured check-in token for a schedule; verify the student has an active enrollment in that class; only allow check-in for the current date; create `Present` attendance once per student/schedule/date; reject duplicate check-in with a conflict. Preserve attendance list/manual record management for Admin/Teacher.
4. **Grades:** preserve assessment/criteria relationships and final-grade calculation behavior; portal results are scoped to the signed-in student. Any grading-scale output must match the reference's stored/returned letter grade and score semantics.
5. **Invoices and payments:** provider confirmations, not client claims, determine successful payment. Validate invoice ownership and outstanding balance, prevent duplicate callback processing, record payment once, and keep invoice paid/partial/unpaid status and balances consistent.
6. **Dashboard/reports:** return the same metrics and filter semantics as the reference. Reports accept `range`, `academicYearId`, and `semesterId` filters; dashboard supports its documented revenue range. Zero-data results remain valid and use stable response keys.
7. **Files and notifications:** certificates and lesson resources must be retrievable artifacts with stable URLs or equivalent delivery; configured email/Telegram events follow the reference's optional behavior.

## 12. Data model and intentional implementation difference

The Spring Flyway migration and JPA entities map the same 22 core Scholaris tables (`users` through `student_emergency_contacts`). Scholaris's current server uses MongoDB/Mongoose while Spring uses SQL Server/JPA. This contract requires equivalent records and observable relationships, not identical storage engines or query implementation.

## 13. Stage 1 completion criteria

- This document is the baseline checklist for subsequent implementation stages.
- Each feature has a reference route/access entry and a Spring baseline status.
- Later stages must update the status and link implementation evidence here; a feature is complete only when its behavior, access control, and response contract meet the relevant sections above.
- Tests and client-level verification belong to implementation/release stages; this stage records the contract and does not claim runtime parity.

## 14. Source basis

- `../ScholarisAPI/README.md`
- `../ScholarisAPI/server.js`
- `../ScholarisAPI/src/modules/*/*.route.js`
- `../ScholarisAPI/src/modules/*/*.controller.js` and `*.service.js` for behavior
- `../ScholarisAPI/src/middleware/authenticate.js` and `authorize.js`
- `src/main/resources/db/migration/V1__create_scholaris_schema.sql`
- `src/main/java/com/chomnes/system/controller/`, `config/SecurityConfiguration.java`, and `model/`
