package com.bank.loanapi;

import com.bank.loanapi.dto.request.CreateLoanRequest;
import com.bank.loanapi.dto.request.LoginRequest;
import com.bank.loanapi.dto.request.PayLoanRequest;
import com.bank.loanapi.dto.response.JwtAuthenticationResponse;
import com.bank.loanapi.dto.response.LoanResponse;
import com.bank.loanapi.dto.response.PaymentResultResponse;
import com.bank.loanapi.entity.Customer;
import com.bank.loanapi.entity.User;
import com.bank.loanapi.repository.CustomerRepository;
import com.bank.loanapi.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Disabled("Integration tests need proper setup")
class LoanApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String customerToken;
    private Long customerId;

    @BeforeEach
    void setUp() throws Exception {
        // Create admin user
        User adminUser = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .roles(Set.of(User.Role.ADMIN))
                .build();
        userRepository.save(adminUser);

        // Create customer user
        User customerUser = User.builder()
                .username("test.customer")
                .password(passwordEncoder.encode("password123"))
                .roles(Set.of(User.Role.CUSTOMER))
                .build();
        customerUser = userRepository.save(customerUser);

        // Create customer
        Customer customer = Customer.builder()
                .name("Test")
                .surname("Customer")
                .creditLimit(new BigDecimal("10000"))
                .usedCreditLimit(BigDecimal.ZERO)
                .user(customerUser)
                .build();
        customer = customerRepository.save(customer);
        customerId = customer.getId();

        // Get tokens
        adminToken = getAuthToken("admin", "admin123");
        customerToken = getAuthToken("test.customer", "password123");
    }

    private String getAuthToken(String username, String password) throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username(username)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JwtAuthenticationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                JwtAuthenticationResponse.class
        );

        return response.getAccessToken();
    }

    @Test
    void completeWorkflow_CreateAndPayLoan() throws Exception {
        // 1. Create loan
        CreateLoanRequest createRequest = CreateLoanRequest.builder()
                .customerId(customerId)
                .amount(new BigDecimal("1000"))
                .interestRate(new BigDecimal("0.2"))
                .numberOfInstallments(6)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/loans")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        LoanResponse loanResponse = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                LoanResponse.class
        );

        assertNotNull(loanResponse.getId());
        assertEquals(new BigDecimal("1200.00"), loanResponse.getTotalAmount());

        // 2. List loans
        mockMvc.perform(get("/api/loans")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("customerId", customerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(loanResponse.getId()));

        // 3. List installments
        mockMvc.perform(get("/api/loans/" + loanResponse.getId() + "/installments")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(200.00));

        // 4. Pay loan
        PayLoanRequest payRequest = new PayLoanRequest(new BigDecimal("400"));

        MvcResult payResult = mockMvc.perform(post("/api/loans/" + loanResponse.getId() + "/pay")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payRequest)))
                .andExpect(status().isOk())
                .andReturn();

        PaymentResultResponse paymentResponse = objectMapper.readValue(
                payResult.getResponse().getContentAsString(),
                PaymentResultResponse.class
        );

        assertEquals(2, paymentResponse.getInstallmentsPaid());
        assertFalse(paymentResponse.getIsLoanFullyPaid());
    }

    @Test
    void accessControl_CustomerCannotAccessOtherCustomersData() throws Exception {
        // Create another customer
        User otherUser = User.builder()
                .username("other.customer")
                .password(passwordEncoder.encode("password123"))
                .roles(Set.of(User.Role.CUSTOMER))
                .build();
        otherUser = userRepository.save(otherUser);

        Customer otherCustomer = Customer.builder()
                .name("Other")
                .surname("Customer")
                .creditLimit(new BigDecimal("5000"))
                .usedCreditLimit(BigDecimal.ZERO)
                .user(otherUser)
                .build();
        otherCustomer = customerRepository.save(otherCustomer);

        // Try to access other customer's data
        mockMvc.perform(get("/api/loans")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("customerId", otherCustomer.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void accessControl_AdminCanAccessAllCustomersData() throws Exception {
        // Admin should be able to access any customer's data
        mockMvc.perform(get("/api/loans")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("customerId", customerId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void validation_InvalidLoanParameters() throws Exception {
        CreateLoanRequest invalidRequest = CreateLoanRequest.builder()
                .customerId(customerId)
                .amount(new BigDecimal("1000"))
                .interestRate(new BigDecimal("0.7")) // Invalid: > 0.5
                .numberOfInstallments(6)
                .build();

        mockMvc.perform(post("/api/loans")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.interestRate").exists());
    }

    @Test
    void creditLimit_InsufficientCredit() throws Exception {
        CreateLoanRequest largeRequest = CreateLoanRequest.builder()
                .customerId(customerId)
                .amount(new BigDecimal("15000")) // Too large for credit limit
                .interestRate(new BigDecimal("0.2"))
                .numberOfInstallments(6)
                .build();

        mockMvc.perform(post("/api/loans")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(largeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Insufficient Credit"));
    }
}