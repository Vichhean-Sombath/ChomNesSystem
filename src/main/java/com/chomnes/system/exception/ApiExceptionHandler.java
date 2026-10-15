package com.chomnes.system.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(exception.getMessage(), List.of()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(exception.getMessage(), List.of()));
    }

    @ExceptionHandler({
            UserNotFoundException.class,
            StudentNotFoundException.class,
            TeacherNotFoundException.class,
            AcademicYearNotFoundException.class,
            SemesterNotFoundException.class,
            SchoolClassNotFoundException.class,
            SubjectNotFoundException.class,
            ClassEnrollmentNotFoundException.class,
            TimeSlotNotFoundException.class,
            ScheduleNotFoundException.class,
            AttendanceRecordNotFoundException.class,
            GradingCriterionNotFoundException.class,
            AssessmentNotFoundException.class,
            GradeNotFoundException.class,
            FinalGradeNotFoundException.class,
            FeeStructureNotFoundException.class,
            InvoiceNotFoundException.class
    })
    public ResponseEntity<ApiError> handleNotFound(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(exception.getMessage(), List.of()));
    }

    @ExceptionHandler({
            AdminBootstrapCompleteException.class,
            DuplicateUserException.class,
            StudentConflictException.class,
            TeacherConflictException.class,
            AcademicYearConflictException.class,
            SemesterConflictException.class,
            SchoolClassConflictException.class,
            SubjectConflictException.class,
            ClassEnrollmentConflictException.class,
            TimeSlotConflictException.class,
            ScheduleConflictException.class,
            AttendanceRecordConflictException.class,
            GradingCriterionConflictException.class,
            AssessmentConflictException.class,
            GradeConflictException.class,
            FinalGradeConflictException.class,
            FeeStructureConflictException.class,
            InvoiceConflictException.class,
            DataIntegrityViolationException.class
    })
    public ResponseEntity<ApiError> handleConflict(RuntimeException exception) {
        String message = exception instanceof DataIntegrityViolationException
                ? "A conflicting record already exists."
                : exception.getMessage();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(message, List.of()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ApiError(exception.getMessage(), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest().body(new ApiError("Validation failed.", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("Request body contains an invalid value.", List.of()));
    }

    public record ApiError(String message, List<String> errors) {
    }
}
