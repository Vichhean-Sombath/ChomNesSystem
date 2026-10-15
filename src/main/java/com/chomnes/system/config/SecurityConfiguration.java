package com.chomnes.system.config;

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class SecurityConfiguration {
    @Bean
    public SecretKey jwtSecretKey(@Value("${app.jwt.secret-base64}") String secretBase64) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secretBase64);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET_BASE64 must contain a valid Base64-encoded key.", exception);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET_BASE64 must decode to at least 32 bytes for HS256.");
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
                .build();
        OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer("chomnes-system");
        decoder.setJwtValidator(validator);
        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/bootstrap-admin").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("Admin")
                        .requestMatchers(HttpMethod.POST, "/api/users").hasRole("Admin")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("Admin")
                        .requestMatchers(HttpMethod.GET, "/api/users/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/students").hasAnyRole("Admin", "Teacher")
                        .requestMatchers(HttpMethod.POST, "/api/students").hasRole("Admin")
                        .requestMatchers(HttpMethod.DELETE, "/api/students/**").hasRole("Admin")
                        .requestMatchers(HttpMethod.GET, "/api/students/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/students/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/teachers").hasAnyRole("Admin", "Teacher")
                        .requestMatchers(HttpMethod.POST, "/api/teachers").hasRole("Admin")
                        .requestMatchers(HttpMethod.DELETE, "/api/teachers/**").hasRole("Admin")
                        .requestMatchers(HttpMethod.GET, "/api/teachers/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/teachers/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/academic-years", "/api/academic-years/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/academic-years").hasRole("Admin")
                        .requestMatchers(HttpMethod.PUT, "/api/academic-years/**").hasRole("Admin")
                        .requestMatchers(HttpMethod.GET, "/api/semesters", "/api/semesters/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/semesters").hasRole("Admin")
                        .requestMatchers(HttpMethod.PUT, "/api/semesters/**").hasRole("Admin")
                        .requestMatchers(HttpMethod.GET, "/api/classes", "/api/classes/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/classes").hasRole("Admin")
                        .requestMatchers(HttpMethod.PUT, "/api/classes/**").hasRole("Admin")
                        .requestMatchers(HttpMethod.GET, "/api/subjects", "/api/subjects/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/subjects").hasRole("Admin")
                        .requestMatchers(HttpMethod.PUT, "/api/subjects/**").hasRole("Admin")
                        .requestMatchers(HttpMethod.GET, "/api/class-enrollments", "/api/class-enrollments/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/class-enrollments").hasRole("Admin")
                        .requestMatchers(HttpMethod.PUT, "/api/class-enrollments/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/time-slots", "/api/time-slots/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/time-slots").hasRole("Admin")
                        .requestMatchers(HttpMethod.PUT, "/api/time-slots/**").hasRole("Admin")
                        .requestMatchers(HttpMethod.GET, "/api/schedules", "/api/schedules/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/schedules").hasRole("Admin")
                        .requestMatchers(HttpMethod.PUT, "/api/schedules/**").hasRole("Admin")
                        .requestMatchers(HttpMethod.GET, "/api/attendance-records", "/api/attendance-records/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/attendance-records").hasAnyRole("Admin", "Teacher")
                        .requestMatchers(HttpMethod.PUT, "/api/attendance-records/**").hasAnyRole("Admin", "Teacher")
                        .requestMatchers(HttpMethod.GET, "/api/grading-criteria", "/api/grading-criteria/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/grading-criteria").hasRole("Admin")
                        .requestMatchers(HttpMethod.PUT, "/api/grading-criteria/**").hasRole("Admin")
                        .requestMatchers(HttpMethod.GET, "/api/assessments", "/api/assessments/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/assessments").hasAnyRole("Admin", "Teacher")
                        .requestMatchers(HttpMethod.PUT, "/api/assessments/**").hasAnyRole("Admin", "Teacher")
                        .requestMatchers(HttpMethod.GET, "/api/grades", "/api/grades/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/grades").hasAnyRole("Admin", "Teacher")
                        .requestMatchers(HttpMethod.PUT, "/api/grades/**").hasAnyRole("Admin", "Teacher")
                        .requestMatchers(HttpMethod.GET, "/api/final-grades", "/api/final-grades/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/final-grades/compute").hasAnyRole("Admin", "Teacher")
                        .requestMatchers(HttpMethod.GET, "/api/fee-structures", "/api/fee-structures/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/fee-structures").hasRole("Admin")
                        .requestMatchers(HttpMethod.PUT, "/api/fee-structures/**").hasRole("Admin")
                        .requestMatchers(HttpMethod.GET, "/api/invoices", "/api/invoices/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/invoices").hasRole("Admin")
                        .requestMatchers(HttpMethod.GET, "/api/payments", "/api/payments/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/payments").hasRole("Admin")
                        .requestMatchers(HttpMethod.GET, "/api/bakong-qr-requests", "/api/bakong-qr-requests/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/bakong-qr-requests").hasAnyRole("Admin", "Student")
                        .requestMatchers(HttpMethod.GET, "/api/certificates", "/api/certificates/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/certificates").hasRole("Admin")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                )
                .build();
    }
}
