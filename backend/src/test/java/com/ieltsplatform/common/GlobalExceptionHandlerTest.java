package com.ieltsplatform.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltsplatform.common.exception.BadRequestException;
import com.ieltsplatform.common.exception.DuplicateResourceException;
import com.ieltsplatform.common.exception.GlobalExceptionHandler;
import com.ieltsplatform.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestErrorController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    static class DummyValidationPayload {
        @NotBlank(message = "Name is required")
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;

        public DummyValidationPayload() {}

        public DummyValidationPayload(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    @RestController
    static class TestErrorController {

        @GetMapping("/test/not-found")
        public void throwNotFound() {
            throw new ResourceNotFoundException("Resource not found with ID: 123");
        }

        @GetMapping("/test/duplicate")
        public void throwDuplicate() {
            throw new DuplicateResourceException("Email already exists: test@example.com");
        }

        @GetMapping("/test/bad-request")
        public void throwBadRequest() {
            throw new BadRequestException("Invalid request parameters");
        }

        @PostMapping("/test/validation")
        public String testValidation(@Valid @RequestBody DummyValidationPayload payload) {
            return "ok";
        }

        @GetMapping("/test/unexpected-error")
        public void throwUnexpected() {
            throw new RuntimeException("Simulated unexpected database failure");
        }
    }

    @Test
    @DisplayName("Throwing ResourceNotFoundException returns HTTP 404 with structured ApiError")
    void testHandleResourceNotFoundException() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Resource not found with ID: 123"))
                .andExpect(jsonPath("$.path").value("/test/not-found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Throwing DuplicateResourceException returns HTTP 409 with structured ApiError")
    void testHandleDuplicateResourceException() throws Exception {
        mockMvc.perform(get("/test/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email already exists: test@example.com"))
                .andExpect(jsonPath("$.path").value("/test/duplicate"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Throwing BadRequestException returns HTTP 400 with structured ApiError")
    void testHandleBadRequestException() throws Exception {
        mockMvc.perform(get("/test/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid request parameters"))
                .andExpect(jsonPath("$.path").value("/test/bad-request"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Invalid request body returns HTTP 400 with field-specific validation error details")
    void testHandleValidationException() throws Exception {
        DummyValidationPayload invalidPayload = new DummyValidationPayload("", "invalid-email-format");

        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/test/validation"))
                .andExpect(jsonPath("$.validationErrors.name").value("Name is required"))
                .andExpect(jsonPath("$.validationErrors.email").value("Email must be valid"));
    }

    @Test
    @DisplayName("Unexpected generic Exception returns HTTP 500 without leaking stack trace")
    void testHandleGenericException() throws Exception {
        mockMvc.perform(get("/test/unexpected-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.path").value("/test/unexpected-error"));
    }
}
