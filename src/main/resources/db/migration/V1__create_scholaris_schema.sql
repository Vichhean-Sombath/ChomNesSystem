-- SQL Server version of Scholaris' legacy schema.
-- MySQL ENUMs become NVARCHAR columns protected by CHECK constraints.
-- MySQL utf8mb4 strings map to NVARCHAR so Khmer and other Unicode text is preserved.

CREATE TABLE users
(
    user_id       INT IDENTITY(1,1) NOT NULL,
    username      NVARCHAR(50) NOT NULL,
    email         NVARCHAR(100) NOT NULL,
    password_hash NVARCHAR(255) NOT NULL, [
    role]
    NVARCHAR
(
    20
) NOT NULL, [status] NVARCHAR(20) NOT NULL CONSTRAINT df_users_status DEFAULT N'Active', last_login_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT df_users_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL CONSTRAINT df_users_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK ([role] IN (N'Admin', N'Teacher', N'Student')) ,
    CONSTRAINT ck_users_status CHECK ([status] IN (N'Active', N'Inactive'))
);

CREATE TABLE teachers
(
    teacher_id     INT IDENTITY(1,1) NOT NULL,
    user_id        INT NOT NULL,
    first_name     NVARCHAR(50) NOT NULL,
    last_name      NVARCHAR(50) NOT NULL,
    dob            DATE NULL,
    gender         NVARCHAR(6) NULL,
    photo_url      NVARCHAR(255) NULL,
    contact_number NVARCHAR(20) NULL,
    specialization NVARCHAR(100) NULL,
    bio            NVARCHAR(MAX) NULL,
    hire_date      DATE NULL, [
    status]
    NVARCHAR
(
    20
) NOT NULL CONSTRAINT df_teachers_status DEFAULT N'Active',
    CONSTRAINT pk_teachers PRIMARY KEY (teacher_id),
    CONSTRAINT uq_teachers_user_id UNIQUE (user_id),
    CONSTRAINT ck_teachers_gender CHECK (gender IS NULL OR gender IN (N'Male', N'Female', N'Other')),
    CONSTRAINT ck_teachers_status CHECK ([status] IN (N'Active', N'Inactive')) ,
    CONSTRAINT teachers_user_id_fk FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE students
(
    student_id      INT IDENTITY(1,1) NOT NULL,
    user_id         INT  NOT NULL,
    first_name      NVARCHAR(50) NOT NULL,
    last_name       NVARCHAR(50) NOT NULL,
    dob             DATE NULL,
    gender          NVARCHAR(6) NULL,
    photo_url       NVARCHAR(255) NULL,
    contact_number  NVARCHAR(20) NULL,
    address         NVARCHAR(255) NULL,
    enrollment_date DATE NOT NULL, [
    status]
    NVARCHAR
(
    20
) NOT NULL CONSTRAINT df_students_status DEFAULT N'Active',
    created_at DATETIME2 NOT NULL CONSTRAINT df_students_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_students PRIMARY KEY (student_id),
    CONSTRAINT uq_students_user_id UNIQUE (user_id),
    CONSTRAINT ck_students_gender CHECK (gender IS NULL OR gender IN (N'Male', N'Female', N'Other')),
    CONSTRAINT ck_students_status CHECK ([status] IN (N'Active', N'Inactive')) ,
    CONSTRAINT students_user_id_fk FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE academic_years
(
    academic_year_id INT IDENTITY(1,1) NOT NULL,
    year_name        NVARCHAR(20) NOT NULL,
    start_date       DATE NOT NULL,
    end_date         DATE NOT NULL, [
    status]
    NVARCHAR
(
    10
) NOT NULL CONSTRAINT df_academic_years_status DEFAULT N'Active',
    CONSTRAINT pk_academic_years PRIMARY KEY (academic_year_id),
    CONSTRAINT uq_academic_years_year_name UNIQUE (year_name),
    CONSTRAINT ck_academic_years_status CHECK ([status] IN (N'Active', N'Closed'))
    );

CREATE TABLE semesters
(
    semester_id      INT IDENTITY(1,1) NOT NULL,
    academic_year_id INT  NOT NULL,
    semester_name    NVARCHAR(30) NOT NULL,
    start_date       DATE NOT NULL,
    end_date         DATE NOT NULL,
    CONSTRAINT pk_semesters PRIMARY KEY (semester_id),
    CONSTRAINT semesters_academic_year_id_fk FOREIGN KEY (academic_year_id) REFERENCES academic_years (academic_year_id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE subjects
(
    subject_id              INT IDENTITY(1,1) NOT NULL,
    subject_code            NVARCHAR(20) NOT NULL,
    subject_name            NVARCHAR(100) NOT NULL,
    description             NVARCHAR(MAX) NULL,
    prerequisite_subject_id INT NULL,
    CONSTRAINT pk_subjects PRIMARY KEY (subject_id),
    CONSTRAINT uq_subjects_subject_code UNIQUE (subject_code),
    CONSTRAINT subjects_prerequisite_subject_id_fk FOREIGN KEY (prerequisite_subject_id) REFERENCES subjects (subject_id) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE TABLE classes
(
    class_id            INT IDENTITY(1,1) NOT NULL,
    class_name          NVARCHAR(50) NOT NULL,
    academic_year_id    INT NOT NULL,
    semester_id         INT NOT NULL,
    room_number         NVARCHAR(20) NULL,
    max_capacity        INT NULL,
    homeroom_teacher_id INT NULL,
    CONSTRAINT pk_classes PRIMARY KEY (class_id),
    CONSTRAINT classes_academic_year_id_fk FOREIGN KEY (academic_year_id) REFERENCES academic_years (academic_year_id) ON DELETE NO ACTION ON UPDATE CASCADE,
    CONSTRAINT classes_semester_id_fk FOREIGN KEY (semester_id) REFERENCES semesters (semester_id) ON DELETE NO ACTION ON UPDATE CASCADE,
    CONSTRAINT classes_homeroom_teacher_id_fk FOREIGN KEY (homeroom_teacher_id) REFERENCES teachers (teacher_id) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE TABLE class_enrollments
(
    enrollment_id   INT IDENTITY(1,1) NOT NULL,
    student_id      INT  NOT NULL,
    class_id        INT  NOT NULL,
    enrollment_date DATE NOT NULL, [
    status]
    NVARCHAR
(
    20
) NOT NULL CONSTRAINT df_class_enrollments_status DEFAULT N'Active',
    CONSTRAINT pk_class_enrollments PRIMARY KEY (enrollment_id),
    CONSTRAINT uq_class_enrollments_student_class_date UNIQUE (student_id, class_id, enrollment_date),
    CONSTRAINT ck_class_enrollments_status CHECK ([status] IN (N'Active', N'Transferred', N'Withdrawn')) ,
    CONSTRAINT class_enrollments_student_id_fk FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT class_enrollments_class_id_fk FOREIGN KEY (class_id) REFERENCES classes(class_id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE time_slots
(
    time_slot_id INT IDENTITY(1,1) NOT NULL,
    day_of_week  NVARCHAR(3) NOT NULL,
    start_time   TIME(0) NOT NULL,
    end_time     TIME(0) NOT NULL,
    CONSTRAINT pk_time_slots PRIMARY KEY (time_slot_id),
    CONSTRAINT uq_time_slots_day_start UNIQUE (day_of_week, start_time),
    CONSTRAINT ck_time_slots_day_of_week CHECK (day_of_week IN (N'Mon', N'Tue', N'Wed', N'Thu', N'Fri', N'Sat', N'Sun'))
);

CREATE TABLE schedules
(
    schedule_id  INT IDENTITY(1,1) NOT NULL,
    class_id     INT NOT NULL,
    subject_id   INT NOT NULL,
    teacher_id   INT NOT NULL,
    time_slot_id INT NOT NULL,
    room_number  NVARCHAR(20) NULL,
    CONSTRAINT pk_schedules PRIMARY KEY (schedule_id),
    CONSTRAINT uq_schedules_teacher_time_slot UNIQUE (teacher_id, time_slot_id),
    CONSTRAINT schedules_class_id_fk FOREIGN KEY (class_id) REFERENCES classes (class_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT schedules_subject_id_fk FOREIGN KEY (subject_id) REFERENCES subjects (subject_id) ON DELETE NO ACTION ON UPDATE CASCADE,
    CONSTRAINT schedules_teacher_id_fk FOREIGN KEY (teacher_id) REFERENCES teachers (teacher_id) ON DELETE NO ACTION ON UPDATE CASCADE,
    CONSTRAINT schedules_time_slot_id_fk FOREIGN KEY (time_slot_id) REFERENCES time_slots (time_slot_id) ON DELETE NO ACTION ON UPDATE CASCADE
);

CREATE TABLE attendance_records
(
    attendance_id   INT IDENTITY(1,1) NOT NULL,
    schedule_id     INT  NOT NULL,
    student_id      INT  NOT NULL,
    attendance_date DATE NOT NULL, [
    status]
    NVARCHAR
(
    10
) NOT NULL,
    marked_by INT NOT NULL,
    marked_at DATETIME2 NOT NULL CONSTRAINT df_attendance_records_marked_at DEFAULT SYSDATETIME(),
    last_edited_at DATETIME2 NULL,
    CONSTRAINT pk_attendance_records PRIMARY KEY (attendance_id),
    CONSTRAINT uq_attendance_schedule_student_date UNIQUE (schedule_id, student_id, attendance_date),
    CONSTRAINT ck_attendance_records_status CHECK ([status] IN (N'Present', N'Absent', N'Late', N'Excused')) ,
    CONSTRAINT attendance_records_schedule_id_fk FOREIGN KEY (schedule_id) REFERENCES schedules(schedule_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT attendance_records_student_id_fk FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT attendance_records_marked_by_fk FOREIGN KEY (marked_by) REFERENCES teachers(teacher_id) ON DELETE NO ACTION ON UPDATE CASCADE
);

CREATE TABLE grading_criteria
(
    criteria_id       INT IDENTITY(1,1) NOT NULL,
    subject_id        INT NULL,
    class_id          INT NULL,
    component_name    NVARCHAR(50) NOT NULL,
    weight_percentage DECIMAL(5, 2) NOT NULL,
    attempt_count     INT           NOT NULL CONSTRAINT df_grading_criteria_attempt_count DEFAULT 1,
    CONSTRAINT pk_grading_criteria PRIMARY KEY (criteria_id),
    CONSTRAINT grading_criteria_subject_id_fk FOREIGN KEY (subject_id) REFERENCES subjects (subject_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT grading_criteria_class_id_fk FOREIGN KEY (class_id) REFERENCES classes (class_id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE assessments
(
    assessment_id   INT IDENTITY(1,1) NOT NULL,
    schedule_id     INT           NOT NULL,
    criteria_id     INT           NOT NULL,
    assessment_name NVARCHAR(100) NOT NULL,
    max_score       DECIMAL(6, 2) NOT NULL,
    assessment_date DATE NULL,
    CONSTRAINT pk_assessments PRIMARY KEY (assessment_id),
    CONSTRAINT assessments_schedule_id_fk FOREIGN KEY (schedule_id) REFERENCES schedules (schedule_id) ON DELETE CASCADE ON UPDATE CASCADE,
    -- Avoid SQL Server's multiple-cascade-path restriction from classes through
    -- both schedules and grading_criteria to assessments.
    CONSTRAINT assessments_criteria_id_fk FOREIGN KEY (criteria_id) REFERENCES grading_criteria (criteria_id) ON DELETE NO ACTION ON UPDATE CASCADE
);

CREATE TABLE grades
(
    grade_id      INT IDENTITY(1,1) NOT NULL,
    assessment_id INT           NOT NULL,
    student_id    INT           NOT NULL,
    score         DECIMAL(6, 2) NOT NULL,
    entered_by    INT           NOT NULL,
    entered_at    DATETIME2     NOT NULL CONSTRAINT df_grades_entered_at DEFAULT SYSDATETIME(),
    is_published  BIT           NOT NULL CONSTRAINT df_grades_is_published DEFAULT 0,
    CONSTRAINT pk_grades PRIMARY KEY (grade_id),
    CONSTRAINT uq_grades_assessment_student UNIQUE (assessment_id, student_id),
    CONSTRAINT grades_assessment_id_fk FOREIGN KEY (assessment_id) REFERENCES assessments (assessment_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT grades_student_id_fk FOREIGN KEY (student_id) REFERENCES students (student_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT grades_entered_by_fk FOREIGN KEY (entered_by) REFERENCES teachers (teacher_id) ON DELETE NO ACTION ON UPDATE CASCADE
);

CREATE TABLE final_grades
(
    final_grade_id INT IDENTITY(1,1) NOT NULL,
    student_id     INT           NOT NULL,
    subject_id     INT           NOT NULL,
    class_id       INT           NOT NULL,
    semester_id    INT           NOT NULL,
    final_score    DECIMAL(6, 2) NOT NULL,
    letter_grade   NVARCHAR(2) NULL,
    gpa_points     DECIMAL(3, 2) NULL,
    computed_at    DATETIME2     NOT NULL CONSTRAINT df_final_grades_computed_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_final_grades PRIMARY KEY (final_grade_id),
    CONSTRAINT uq_final_grades_student_subject_semester UNIQUE (student_id, subject_id, semester_id),
    CONSTRAINT final_grades_student_id_fk FOREIGN KEY (student_id) REFERENCES students (student_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT final_grades_subject_id_fk FOREIGN KEY (subject_id) REFERENCES subjects (subject_id) ON DELETE NO ACTION ON UPDATE CASCADE,
    CONSTRAINT final_grades_class_id_fk FOREIGN KEY (class_id) REFERENCES classes (class_id) ON DELETE NO ACTION ON UPDATE CASCADE,
    CONSTRAINT final_grades_semester_id_fk FOREIGN KEY (semester_id) REFERENCES semesters (semester_id) ON DELETE NO ACTION ON UPDATE CASCADE
);

CREATE TABLE fee_structures
(
    fee_id      INT IDENTITY(1,1) NOT NULL,
    class_id    INT NULL,
    semester_id INT            NOT NULL,
    fee_name    NVARCHAR(100) NOT NULL,
    amount      DECIMAL(10, 2) NOT NULL,
    due_date    DATE NULL,
    CONSTRAINT pk_fee_structures PRIMARY KEY (fee_id),
    CONSTRAINT fee_structures_class_id_fk FOREIGN KEY (class_id) REFERENCES classes (class_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fee_structures_semester_id_fk FOREIGN KEY (semester_id) REFERENCES semesters (semester_id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE invoices
(
    invoice_id     INT IDENTITY(1,1) NOT NULL,
    invoice_number NVARCHAR(30) NOT NULL,
    student_id     INT            NOT NULL,
    fee_id         INT            NOT NULL,
    semester_id    INT            NOT NULL,
    issue_date     DATE           NOT NULL,
    due_date       DATE           NOT NULL,
    total_amount   DECIMAL(10, 2) NOT NULL,
    amount_paid    DECIMAL(10, 2) NOT NULL CONSTRAINT df_invoices_amount_paid DEFAULT 0, [
    status]
    NVARCHAR
(
    10
) NOT NULL CONSTRAINT df_invoices_status DEFAULT N'Unpaid',
    created_at DATETIME2 NOT NULL CONSTRAINT df_invoices_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_invoices PRIMARY KEY (invoice_id),
    CONSTRAINT uq_invoices_invoice_number UNIQUE (invoice_number),
    CONSTRAINT ck_invoices_status CHECK ([status] IN (N'Unpaid', N'Partial', N'Paid', N'Overdue')) ,
    CONSTRAINT invoices_student_id_fk FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT invoices_fee_id_fk FOREIGN KEY (fee_id) REFERENCES fee_structures(fee_id) ON DELETE NO ACTION ON UPDATE CASCADE,
    CONSTRAINT invoices_semester_id_fk FOREIGN KEY (semester_id) REFERENCES semesters(semester_id) ON DELETE NO ACTION ON UPDATE CASCADE
);

CREATE TABLE payments
(
    payment_id            INT IDENTITY(1,1) NOT NULL,
    invoice_id            INT            NOT NULL,
    payment_date          DATE           NOT NULL,
    amount                DECIMAL(10, 2) NOT NULL,
    payment_method        NVARCHAR(20) NOT NULL,
    receipt_url           NVARCHAR(255) NULL,
    transaction_reference NVARCHAR(255) NULL,
    recorded_by           INT            NOT NULL,
    notes                 NVARCHAR(255) NULL,
    CONSTRAINT pk_payments PRIMARY KEY (payment_id),
    CONSTRAINT ck_payments_payment_method CHECK (payment_method IN (N'Stripe', N'BakongKHQR')),
    CONSTRAINT payments_invoice_id_fk FOREIGN KEY (invoice_id) REFERENCES invoices (invoice_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT payments_recorded_by_fk FOREIGN KEY (recorded_by) REFERENCES users (user_id) ON DELETE NO ACTION ON UPDATE CASCADE
);

CREATE TABLE bakong_qr_requests
(
    qr_id      INT IDENTITY(1,1) NOT NULL,
    invoice_id INT            NOT NULL,
    md5        NVARCHAR(64) NOT NULL,
    amount_khr DECIMAL(15, 2) NOT NULL,
    amount_usd DECIMAL(10, 2) NOT NULL, [
    status]
    NVARCHAR
(
    10
) NOT NULL CONSTRAINT df_bakong_qr_requests_status DEFAULT N'Pending',
    expires_at DATETIME2 NOT NULL,
    paid_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT df_bakong_qr_requests_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_bakong_qr_requests PRIMARY KEY (qr_id),
    CONSTRAINT uq_bakong_qr_requests_md5 UNIQUE (md5),
    CONSTRAINT ck_bakong_qr_requests_status CHECK ([status] IN (N'Pending', N'Paid', N'Expired', N'Failed')) ,
    CONSTRAINT bakong_qr_requests_invoice_id_fk FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE certificates
(
    certificate_id     INT IDENTITY(1,1) NOT NULL,
    student_id         INT  NOT NULL,
    certificate_type   NVARCHAR(20) NOT NULL,
    template_used      NVARCHAR(100) NULL,
    issue_date         DATE NOT NULL,
    generated_file_url NVARCHAR(255) NULL,
    issued_by          INT  NOT NULL,
    CONSTRAINT pk_certificates PRIMARY KEY (certificate_id),
    CONSTRAINT ck_certificates_certificate_type CHECK (certificate_type IN (N'Completion', N'Transcript', N'Recommendation')),
    CONSTRAINT certificates_student_id_fk FOREIGN KEY (student_id) REFERENCES students (student_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT certificates_issued_by_fk FOREIGN KEY (issued_by) REFERENCES users (user_id) ON DELETE NO ACTION ON UPDATE CASCADE
);

CREATE TABLE lesson_resources
(
    resource_id   INT IDENTITY(1,1) NOT NULL,
    schedule_id   INT NULL,
    teacher_id    INT       NOT NULL,
    title         NVARCHAR(150) NOT NULL,
    description   NVARCHAR(MAX) NULL,
    resource_type NVARCHAR(20) NOT NULL,
    file_url      NVARCHAR(255) NULL,
    upload_date   DATETIME2 NOT NULL CONSTRAINT df_lesson_resources_upload_date DEFAULT SYSDATETIME(),
    CONSTRAINT pk_lesson_resources PRIMARY KEY (resource_id),
    CONSTRAINT ck_lesson_resources_resource_type CHECK (resource_type IN (N'LessonPlan', N'Homework', N'Syllabus', N'Other')),
    CONSTRAINT lesson_resources_schedule_id_fk FOREIGN KEY (schedule_id) REFERENCES schedules (schedule_id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT lesson_resources_teacher_id_fk FOREIGN KEY (teacher_id) REFERENCES teachers (teacher_id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE student_emergency_contacts
(
    contact_id   INT IDENTITY(1,1) NOT NULL,
    student_id   INT NOT NULL,
    contact_name NVARCHAR(100) NOT NULL,
    relationship NVARCHAR(50) NULL,
    phone_number NVARCHAR(20) NOT NULL,
    email        NVARCHAR(100) NULL,
    CONSTRAINT pk_student_emergency_contacts PRIMARY KEY (contact_id),
    CONSTRAINT student_emergency_contacts_student_id_fk FOREIGN KEY (student_id) REFERENCES students (student_id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX ix_teachers_contact_number ON teachers (contact_number);
CREATE INDEX ix_students_contact_number ON students (contact_number);
CREATE INDEX ix_semesters_academic_year ON semesters (academic_year_id);
CREATE INDEX ix_subjects_prerequisite ON subjects (prerequisite_subject_id);
CREATE INDEX ix_classes_academic_year ON classes (academic_year_id);
CREATE INDEX ix_classes_semester ON classes (semester_id);
CREATE INDEX ix_classes_homeroom_teacher ON classes (homeroom_teacher_id);
CREATE INDEX ix_class_enrollments_student ON class_enrollments (student_id);
CREATE INDEX ix_class_enrollments_class ON class_enrollments (class_id);
CREATE INDEX ix_schedules_class ON schedules (class_id);
CREATE INDEX ix_schedules_subject ON schedules (subject_id);
CREATE INDEX ix_schedules_teacher ON schedules (teacher_id);
CREATE INDEX ix_schedules_time_slot ON schedules (time_slot_id);
CREATE INDEX ix_attendance_records_student ON attendance_records (student_id);
CREATE INDEX ix_attendance_records_marked_by ON attendance_records (marked_by);
CREATE INDEX ix_attendance_records_date ON attendance_records (attendance_date);
CREATE INDEX ix_grading_criteria_subject ON grading_criteria (subject_id);
CREATE INDEX ix_grading_criteria_class ON grading_criteria (class_id);
CREATE INDEX ix_assessments_schedule ON assessments (schedule_id);
CREATE INDEX ix_assessments_criteria ON assessments (criteria_id);
CREATE INDEX ix_grades_student ON grades (student_id);
CREATE INDEX ix_grades_entered_by ON grades (entered_by);
CREATE INDEX ix_final_grades_subject ON final_grades (subject_id);
CREATE INDEX ix_final_grades_class ON final_grades (class_id);
CREATE INDEX ix_final_grades_semester ON final_grades (semester_id);
CREATE INDEX ix_fee_structures_class ON fee_structures (class_id);
CREATE INDEX ix_fee_structures_semester ON fee_structures (semester_id);
CREATE INDEX ix_invoices_student ON invoices (student_id);
CREATE INDEX ix_invoices_fee ON invoices (fee_id);
CREATE INDEX ix_invoices_semester ON invoices (semester_id);
CREATE INDEX ix_payments_invoice ON payments (invoice_id);
CREATE INDEX ix_payments_recorded_by ON payments (recorded_by);
CREATE INDEX ix_bakong_qr_requests_invoice ON bakong_qr_requests (invoice_id);
CREATE INDEX ix_bakong_qr_requests_status ON bakong_qr_requests ([status]);
CREATE INDEX ix_certificates_student ON certificates (student_id);
CREATE INDEX ix_certificates_issued_by ON certificates (issued_by);
CREATE INDEX ix_lesson_resources_schedule ON lesson_resources (schedule_id);
CREATE INDEX ix_lesson_resources_teacher ON lesson_resources (teacher_id);
CREATE INDEX ix_student_emergency_contacts_student ON student_emergency_contacts (student_id);

GO

CREATE TRIGGER trg_users_updated_at
    ON users
    AFTER
UPDATE
    AS
BEGIN
    SET
NOCOUNT ON;

    -- Avoid recursion when this trigger writes the timestamp back to the row.
    IF
TRIGGER_NESTLEVEL() > 1
        RETURN;

UPDATE u
SET updated_at = SYSDATETIME() FROM users AS u
    INNER JOIN inserted AS i
ON i.user_id = u.user_id;
END;
