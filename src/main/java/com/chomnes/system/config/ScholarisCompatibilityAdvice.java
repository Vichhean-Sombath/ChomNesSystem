package com.chomnes.system.config;

import com.chomnes.system.exception.ApiExceptionHandler;
import com.chomnes.system.dto.ScholarisAuthResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonInputMessage;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Adapts JSON names and envelopes only on the legacy Scholaris-compatible routes. */
@ControllerAdvice
public class ScholarisCompatibilityAdvice extends RequestBodyAdviceAdapter
        implements ResponseBodyAdvice<Object> {
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    public ScholarisCompatibilityAdvice(ObjectMapper objectMapper, HttpServletRequest request) {
        this.objectMapper = objectMapper;
        this.request = request;
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return isCompatibilityRequest() && methodParameter.hasParameterAnnotation(
                org.springframework.web.bind.annotation.RequestBody.class
        );
    }

    @Override
    public HttpInputMessage beforeBodyRead(
            HttpInputMessage inputMessage,
            MethodParameter parameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) throws IOException {
        byte[] bytes = inputMessage.getBody().readAllBytes();
        JsonNode root = objectMapper.readTree(bytes);
        JsonNode converted = toCamelCase(root);
        byte[] rewritten = objectMapper.writeValueAsBytes(converted);
        return new MappingJacksonInputMessage(new ByteArrayInputStream(rewritten), inputMessage.getHeaders());
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return isCompatibilityRequest();
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            org.springframework.http.server.ServerHttpRequest serverRequest,
            ServerHttpResponse serverResponse
    ) {
        if (body instanceof ScholarisAuthResponse || request.getRequestURI().equals("/user/logout")
                || request.getRequestURI().equals("/user/change-password")) {
            return toSnakeCase(body);
        }
        if (body instanceof ApiExceptionHandler.ApiError apiError) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("message", apiError.message());
            error.put("errors", apiError.errors());
            return error;
        }

        String method = request.getMethod();
        String path = request.getRequestURI();
        if (path.equals("/dashboard/summary")) {
            return Map.of("message", "Dashboard summary retrieved successfully!", "data", body == null ? Map.of() : body);
        }
        if (path.equals("/reports/summary")) {
            return Map.of("message", "Reports summary loaded successfully", "data", body == null ? Map.of() : body);
        }
        if (path.startsWith("/attendance-record/qr/")) {
            return Map.of("message", "Attendance QR generated successfully!", "data", toSnakeCase(body));
        }
        if (path.startsWith("/student/me/")) {
            return Map.of("message", studentPortalMessage(path), "data", body == null ? Map.of() : body);
        }
        if (path.startsWith("/user/enable/")) {
            return Map.of("message", "User activated successfully!", "data", toSnakeCase(body));
        }
        if (path.startsWith("/user/disable/")) {
            return Map.of("message", "User deactivated successfully!", "data", toSnakeCase(body));
        }
        if (path.equals("/user/change-password")) {
            return Map.of("message", "Password changed successfully!");
        }
        String resource = resourceName(request.getRequestURI());
        if ("DELETE".equals(method)) {
            return Map.of("message", resource + " deleted successfully!");
        }

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("message", resource + " " + operation(method) + " successfully!");
        if (body != null) {
            envelope.put("data", toSnakeCase(body));
        }
        return envelope;
    }

    private boolean isCompatibilityRequest() {
        String path = request.getRequestURI();
        return path != null && !path.equals("/api") && !path.startsWith("/api/");
    }

    private JsonNode toCamelCase(JsonNode node) {
        if (node == null || node.isValueNode()) {
            return node;
        }
        if (node.isArray()) {
            var result = objectMapper.createArrayNode();
            node.forEach(value -> result.add(toCamelCase(value)));
            return result;
        }
        ObjectNode result = objectMapper.createObjectNode();
        node.fields().forEachRemaining(entry -> result.set(camel(entry.getKey()), toCamelCase(entry.getValue())));
        return result;
    }

    private Object toSnakeCase(Object value) {
        JsonNode tree = objectMapper.valueToTree(value);
        return snakeTree(tree);
    }

    private Object snakeTree(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isValueNode()) {
            return objectMapper.convertValue(node, Object.class);
        }
        if (node.isArray()) {
            List<Object> result = new ArrayList<>();
            node.forEach(value -> result.add(snakeTree(value)));
            return result;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> result.put(snake(entry.getKey()), snakeTree(entry.getValue())));
        return result;
    }

    private String camel(String value) {
        StringBuilder result = new StringBuilder(value.length());
        boolean uppercaseNext = false;
        for (char c : value.toCharArray()) {
            if (c == '_') {
                uppercaseNext = true;
            } else {
                result.append(uppercaseNext ? Character.toUpperCase(c) : c);
                uppercaseNext = false;
            }
        }
        return result.toString();
    }

    private String snake(String value) {
        StringBuilder result = new StringBuilder(value.length() + 8);
        for (char c : value.toCharArray()) {
            if (Character.isUpperCase(c)) {
                result.append('_').append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private String resourceName(String path) {
        String[] parts = path.split("/");
        String resource = parts.length > 1 ? parts[1] : "Record";
        if (resource.equals("student-emergency-contact")) resource = "Emergency contact";
        if (resource.endsWith("s") && resource.length() > 1) {
            resource = resource.substring(0, resource.length() - 1);
        }
        resource = resource.replace('-', ' ').replace('_', ' ');
        return Character.toUpperCase(resource.charAt(0)) + resource.substring(1);
    }

    private String operation(String method) {
        return switch (method) {
            case "POST" -> "created";
            case "PUT", "PATCH" -> "updated";
            case "DELETE" -> "deleted";
            default -> "retrieved";
        };
    }

    private String studentPortalMessage(String path) {
        if (path.endsWith("/attendance/checkin")) return "Attendance checked in successfully!";
        if (path.endsWith("/summary")) return "Student summary retrieved successfully!";
        if (path.endsWith("/classes")) return "Student classes retrieved successfully!";
        if (path.endsWith("/schedule")) return "Student schedule retrieved successfully!";
        if (path.endsWith("/grades")) return "Student grades retrieved successfully!";
        if (path.endsWith("/attendance")) return "Student attendance retrieved successfully!";
        if (path.endsWith("/invoices")) return "Student invoices retrieved successfully!";
        if (path.endsWith("/certificates")) return "Student certificates retrieved successfully!";
        if (path.endsWith("/resources")) return "Student resources retrieved successfully!";
        if (path.endsWith("/profile")) return "Student profile retrieved successfully!";
        return "Student portal data retrieved successfully!";
    }
}
