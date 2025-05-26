package com.bank.loanapi.controller;

import com.bank.loanapi.dto.request.CreateLoanRequest;
import com.bank.loanapi.dto.response.LoanResponse;
import com.bank.loanapi.security.JwtAuthenticationFilter;
import com.bank.loanapi.service.LoanService;
import com.bank.loanapi.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = LoanController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LoanService loanService;

    @MockBean
    private PaymentService paymentService;

    private CreateLoanRequest createLoanRequest;
    private LoanResponse loanResponse;

    @BeforeEach
    void setUp() {
        createLoanRequest = CreateLoanRequest.builder()
                .customerId(1L)
                .amount(new BigDecimal("1000"))
                .interestRate(new BigDecimal("0.2"))
                .numberOfInstallments(6)
                .build();

        loanResponse = LoanResponse.builder()
                .id(1L)
                .customerId(1L)
                .customerName("John Doe")
                .loanAmount(new BigDecimal("1000"))
                .numberOfInstallments(6)
                .interestRate(new BigDecimal("0.2"))
                .createDate(LocalDateTime.now())
                .isPaid(false)
                .totalAmount(new BigDecimal("1200"))
                .paidInstallments(0)
                .remainingInstallments(6)
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createLoan_Success() throws Exception {
        when(loanService.createLoan(any(CreateLoanRequest.class))).thenReturn(loanResponse);

        mockMvc.perform(post("/api/loans")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createLoanRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.loanAmount").value(1000))
                .andExpect(jsonPath("$.numberOfInstallments").value(6));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void listLoans_Success() throws Exception {
        List<LoanResponse> loans = Arrays.asList(loanResponse);
        when(loanService.listLoans(1L, null, null)).thenReturn(loans);

        mockMvc.perform(get("/api/loans")
                        .param("customerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].loanAmount").value(1000));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void listLoans_WithFilters_Success() throws Exception {
        List<LoanResponse> loans = Arrays.asList(loanResponse);
        when(loanService.listLoans(1L, 6, false)).thenReturn(loans);

        mockMvc.perform(get("/api/loans")
                        .param("customerId", "1")
                        .param("numberOfInstallments", "6")
                        .param("isPaid", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void createLoan_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createLoanRequest)))
                .andExpect(status().isForbidden()); // 403 instead of 401
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createLoan_ValidationError() throws Exception {
        createLoanRequest.setAmount(null);

        mockMvc.perform(post("/api/loans")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createLoanRequest)))
                .andExpect(status().isBadRequest());
    }
}