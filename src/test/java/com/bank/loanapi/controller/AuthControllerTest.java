package com.bank.loanapi.controller;

import com.bank.loanapi.dto.request.LoginRequest;
import com.bank.loanapi.dto.response.JwtAuthenticationResponse;
import com.bank.loanapi.entity.Customer;
import com.bank.loanapi.entity.User;
import com.bank.loanapi.repository.CustomerRepository;
import com.bank.loanapi.security.CustomUserDetails;
import com.bank.loanapi.security.JwtAuthenticationFilter;
import com.bank.loanapi.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@Disabled("need to check - tests need proper setup")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtTokenProvider tokenProvider;

    @MockBean
    private CustomerRepository customerRepository;

    private LoginRequest loginRequest;
    private User testUser;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        loginRequest = LoginRequest.builder()
                .username("john.doe")
                .password("password123")
                .build();

        testUser = User.builder()
                .id(1L)
                .username("john.doe")
                .roles(Set.of(User.Role.CUSTOMER))
                .build();

        testCustomer = Customer.builder()
                .id(1L)
                .name("John")
                .surname("Doe")
                .user(testUser)
                .build();
    }

    @Test
    void authenticateUser_Success() throws Exception {
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = new CustomUserDetails(testUser);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(tokenProvider.generateToken(authentication)).thenReturn("test-jwt-token");
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(testCustomer));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())  // Add CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("john.doe"))
                .andExpect(jsonPath("$.customerId").value(1));
    }

    @Test
    void authenticateUser_AdminUser_Success() throws Exception {
        User adminUser = User.builder()
                .id(2L)
                .username("admin")
                .roles(Set.of(User.Role.ADMIN))
                .build();

        LoginRequest adminLogin = LoginRequest.builder()
                .username("admin")
                .password("admin123")
                .build();

        Authentication authentication = mock(Authentication.class);
        CustomUserDetails adminDetails = new CustomUserDetails(adminUser);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(adminDetails);
        when(tokenProvider.generateToken(authentication)).thenReturn("admin-jwt-token");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())  // Add CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("admin-jwt-token"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.customerId").doesNotExist());
    }

    @Test
    void authenticateUser_ValidationError() throws Exception {
        LoginRequest invalidRequest = LoginRequest.builder()
                .username("")
                .password("password")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())  // Add CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}