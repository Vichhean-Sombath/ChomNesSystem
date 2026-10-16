# Scholaris API Feature Parity Contract

**Stage:** 1 — Define the parity contract  
**Status:** Stage 1 baseline recorded; Stage 2 management routes and CRUD are implemented, with related-object response parity still outstanding.  
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
| Payments | Admin CRUD at `/payment/data[/:id]`, `/payment/create`, `/payment/update/:id`, `/payment/delete/:id`; provider routes below | Admin read/create exist at `/api/payments`; update/delete and payment-provider flows are absent. |
| Stripe | `GET /payment/stripe/config` (public); `POST /payment/stripe/checkout` and `/verify` (Admin/Student); `GET /payment/stripe/success` and `/cancel` (provider redirect); `POST /payment/stripe/webhook` (signature-verified provider callback); `POST /payment/stripe/record` (Admin/Student) | Absent. |
| Bakong | `POST /payment/bakong/qr` and `/verify` (Admin/Student); `GET /payment/bakong/account` (Admin); `POST /payment/bakong/webhook` (provider callback) | Partial storage API at `/api/bakong-qr-requests`; actual KHQR generation, provider verification, account check, and callback handling are absent. |
| Certificates | `GET /certificate/data[/:id]`, `POST /certificate/create`, `DELETE /certificate/delete/:id` (Admin) | Metadata read/create at `/api/certificates`; delete and student self-service/file behavior are absent. |
| Lesson resources | `GET /lesson_resource/data[/:id]`, create/update/delete (Admin/Teacher) | CRUD exists at `/api/lesson-resources`; reference paths are absent. Verify parity for file delivery and authorization. |
| Emergency contacts | `GET /student-emergency-contact/data[/:id]`, create/update/delete (Admin) | CRUD exists at `/api/student-emergency-contacts`; student self-service permissions and reference paths differ. |
| Admin dashboard | `GET /dashboard/summary` (Admin) | Absent. |
| Admin reports | `GET /reports/summary` (Admin) | Absent. |
| Student portal | `GET /student/me/{summary,classes,schedule,grades,attendance,invoices,certificates,resources,profile}` and `POST /student/me/attendance/checkin` (Student) | Absent. |
| Notifications | Optional email and Telegram notifications, especially payment events | Absent in Spring baseline. |

The reference CRUD route pattern is explicit per table above: where it says “create/update/delete,” those are respectively `POST /<resource>/create`, `PUT /<resource>/update/:id`, and `DELETE /<resource>/delete/:id` for the resource's displayed route stem.

## 4. Stage 2 implementation update — management API

The Spring project now exposes the Scholaris management route stems and CRUD suffixes for users, students, teachers, academic years, semesters, classes, subjects, enrollments, time slots, schedules, attendance, grading criteria, assessments, grades, final grades, fee structures, invoices, payments, certificates, lesson resources, and emergency contacts. The original `/api/...` routes remain available. Compatibility requests translate snake_case JSON keys to the Spring DTO names; compatibility responses use snake_case fields and the reference `{message, data}` envelope, with validation errors returned as `{message, errors}`. Route security rules now cover the reference role matrix for these management endpoints.

User registration creates its matching student or teacher profile; Scholaris login includes linked profile IDs; logout, password change, and administrator enable/disable routes are present. Repeated enable/disable requests return a client error, matching the reference lifecycle behavior.

This stage closes management route coverage and adds invoice/payment/final-grade update and delete operations. It does **not** claim full management response parity yet: several Spring DTOs return related record IDs where Scholaris populates related objects, and SQL foreign-key restrictions can prevent deletes that MongoDB may allow. Provider payment flows, dashboards, reports, and student portal workflows remain later stages.

## 5. Business behavior that must be preserved

1. **Authentication and account lifecycle:** login verifies credentials and returns user role plus linked `student_id`/`teacher_id`; registration only creates `Teacher` or `Student` accounts and creates the matching profile; logout and password change behave as in the reference; disabled accounts cannot continue authenticating; admins can re-enable them.
2. **Authorization and ownership:** student portal and student payment operations use the authenticated student's own profile. A student cannot read or mutate another student's information by changing an ID in a request. Teacher/admin permissions remain route-specific as in the matrix.
3. **Attendance check-in:** accept only a valid server-issued QR payload for a schedule; verify the student has an active enrollment in that class; only allow check-in for the current date; create `Present` attendance once per student/schedule/date; reject duplicate check-in with a conflict. Preserve attendance list/manual record management for Admin/Teacher.
4. **Grades:** preserve assessment/criteria relationships and final-grade calculation behavior; portal results are scoped to the signed-in student. Any grading-scale output must match the reference's stored/returned letter grade and score semantics.
5. **Invoices and payments:** provider confirmations, not client claims, determine successful payment. Validate invoice ownership and outstanding balance, prevent duplicate callback processing, record payment once, and keep invoice paid/partial/unpaid status and balances consistent.
6. **Dashboard/reports:** return the same metrics and filter semantics as the reference. Reports accept `range`, `academicYearId`, and `semesterId` filters; dashboard supports its documented revenue range. Zero-data results remain valid and use stable response keys.
7. **Files and notifications:** certificates and lesson resources must be retrievable artifacts with stable URLs or equivalent delivery; configured email/Telegram events follow the reference's optional behavior.

## 6. Data model and intentional implementation difference

The Spring Flyway migration and JPA entities map the same 22 core Scholaris tables (`users` through `student_emergency_contacts`). Scholaris's current server uses MongoDB/Mongoose while Spring uses SQL Server/JPA. This contract requires equivalent records and observable relationships, not identical storage engines or query implementation.

## 7. Stage 1 completion criteria

- This document is the baseline checklist for subsequent implementation stages.
- Each feature has a reference route/access entry and a Spring baseline status.
- Later stages must update the status and link implementation evidence here; a feature is complete only when its behavior, access control, and response contract meet the relevant sections above.
- Tests and client-level verification belong to implementation/release stages; this stage records the contract and does not claim runtime parity.

## 8. Source basis

- `../ScholarisAPI/README.md`
- `../ScholarisAPI/server.js`
- `../ScholarisAPI/src/modules/*/*.route.js`
- `../ScholarisAPI/src/modules/*/*.controller.js` and `*.service.js` for behavior
- `../ScholarisAPI/src/middleware/authenticate.js` and `authorize.js`
- `src/main/resources/db/migration/V1__create_scholaris_schema.sql`
- `src/main/java/com/chomnes/system/controller/`, `config/SecurityConfiguration.java`, and `model/`
