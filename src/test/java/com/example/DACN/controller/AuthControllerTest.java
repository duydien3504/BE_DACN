package com.example.DACN.controller;

import com.example.DACN.dto.request.RegisterRequest;
import com.example.DACN.dto.response.RegisterResponse;
import com.example.DACN.exception.DuplicateResourceException;
import com.example.DACN.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Tests")
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private AuthService authService;

        private RegisterRequest validRequest;
        private RegisterResponse successResponse;

        @BeforeEach
        void setUp() {
                validRequest = new RegisterRequest(
                                "test@example.com",
                                "password123",
                                "John Doe",
                                "0123456789");

                successResponse = new RegisterResponse(
                                UUID.randomUUID(),
                                "Success");
        }

        @Test
        @DisplayName("Should register user successfully with valid data")
        void testRegisterSuccess() throws Exception {
                // Given
                when(authService.register(any(RegisterRequest.class))).thenReturn(successResponse);

                // When & Then
                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.userId").exists())
                                .andExpect(jsonPath("$.message").value("Success"));
        }

        @Test
        @DisplayName("Should return 400 when email is invalid")
        void testRegisterWithInvalidEmail() throws Exception {
                // Given
                validRequest.setEmail("invalid-email");

                // When & Then
                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("Should return 400 when password is too short")
        void testRegisterWithShortPassword() throws Exception {
                // Given
                validRequest.setPassword("short");

                // When & Then
                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("Should return 400 when phone number is invalid")
        void testRegisterWithInvalidPhoneNumber() throws Exception {
                // Given
                validRequest.setPhoneNumber("123"); // Not 10 digits

                // When & Then
                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("Should return 409 when email already exists")
        void testRegisterWithDuplicateEmail() throws Exception {
                // Given
                when(authService.register(any(RegisterRequest.class)))
                                .thenThrow(new DuplicateResourceException("Email already exists"));

                // When & Then
                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("Email already exists"));
        }

        @Test
        @DisplayName("Should return 409 when phone number already exists")
        void testRegisterWithDuplicatePhoneNumber() throws Exception {
                // Given
                when(authService.register(any(RegisterRequest.class)))
                                .thenThrow(new DuplicateResourceException("Phone number already exists"));

                // When & Then
                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("Phone number already exists"));
        }
}
