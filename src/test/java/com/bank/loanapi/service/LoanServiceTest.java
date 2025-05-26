package com.bank.loanapi.service;

import com.bank.loanapi.dto.request.CreateLoanRequest;
import com.bank.loanapi.dto.response.LoanResponse;
import com.bank.loanapi.entity.*;
import com.bank.loanapi.exception.*;
import com.bank.loanapi.repository.*;
import com.bank.loanapi.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private LoanInstallmentRepository installmentRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LoanService loanService;

    private Customer testCustomer;
    private User testUser;
    private CreateLoanRequest validRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("john.doe")
                .roles(Set.of(User.Role.CUSTOMER))
                .build();

        testCustomer = Customer.builder()
                .id(1L)
                .name("John")
                .surname("Doe")
                .creditLimit(new BigDecimal("10000"))
                .usedCreditLimit(BigDecimal.ZERO)
                .user(testUser)
                .build();

        validRequest = CreateLoanRequest.builder()
                .customerId(1L)
                .amount(new BigDecimal("1000"))
                .interestRate(new BigDecimal("0.2"))
                .numberOfInstallments(6)
                .build();
    }

    private void setupSecurityContext() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new CustomUserDetails(testUser));
    }

    @Test
    void createLoan_Success() {
        setupSecurityContext();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan loan = invocation.getArgument(0);
            loan.setId(1L);
            return loan;
        });
        when(installmentRepository.countPaidInstallmentsByLoanId(any())).thenReturn(0);

        LoanResponse response = loanService.createLoan(validRequest);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(new BigDecimal("1000"), response.getLoanAmount());
        assertEquals(6, response.getNumberOfInstallments());
        assertEquals(0, new BigDecimal("1200.00").compareTo(response.getTotalAmount()));

        verify(customerRepository).save(any(Customer.class));
        verify(loanRepository).save(any(Loan.class));
    }

    @Test
    void createLoan_InvalidInstallments_ThrowsException() {
        validRequest.setNumberOfInstallments(5);

        assertThrows(InvalidLoanParametersException.class, () -> loanService.createLoan(validRequest));
    }

    @Test
    void createLoan_InvalidInterestRate_ThrowsException() {
        validRequest.setInterestRate(new BigDecimal("0.6"));

        assertThrows(InvalidLoanParametersException.class, () -> loanService.createLoan(validRequest));
    }

    @Test
    void createLoan_InsufficientCredit_ThrowsException() {
        setupSecurityContext();

        testCustomer.setUsedCreditLimit(new BigDecimal("9500"));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        assertThrows(InsufficientCreditException.class, () -> loanService.createLoan(validRequest));
    }

    @Test
    void createLoan_CustomerNotFound_ThrowsException() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> loanService.createLoan(validRequest));
    }

    @Test
    void listLoans_Success() {
        setupSecurityContext();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        Loan loan = Loan.builder()
                .id(1L)
                .customer(testCustomer)
                .loanAmount(new BigDecimal("1000"))
                .numberOfInstallment(6)
                .interestRate(new BigDecimal("0.2"))
                .isPaid(false)
                .build();

        when(loanRepository.findByCustomerId(1L)).thenReturn(List.of(loan));
        when(installmentRepository.countPaidInstallmentsByLoanId(1L)).thenReturn(2);

        List<LoanResponse> loans = loanService.listLoans(1L, null, null);

        assertEquals(1, loans.size());
        assertEquals(1L, loans.get(0).getId());
        assertEquals(2, loans.get(0).getPaidInstallments());
    }

    @Test
    void checkCustomerAccess_AsAdmin_Success() {
        User adminUser = User.builder()
                .id(2L)
                .username("admin")
                .roles(Set.of(User.Role.ADMIN))
                .build();

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new CustomUserDetails(adminUser));

        // Should not throw exception
        assertDoesNotThrow(() -> loanService.checkCustomerAccess(testCustomer));
    }

    @Test
    void checkCustomerAccess_AsOtherCustomer_ThrowsException() {
        User otherUser = User.builder()
                .id(2L)
                .username("jane.doe")
                .roles(Set.of(User.Role.CUSTOMER))
                .build();

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new CustomUserDetails(otherUser));

        assertThrows(AccessDeniedException.class, () -> loanService.checkCustomerAccess(testCustomer));
    }
}